package com.qualiapproche.ia.service;

import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.config.IaAssistantProperties;
import com.qualiapproche.ia.dto.MessageDemandeDto;
import com.qualiapproche.ia.entities.ConversationIa;
import com.qualiapproche.ia.entities.MessageIa;
import com.qualiapproche.ia.enumeration.RoleMessage;
import com.qualiapproche.ia.repository.ConversationIaRepository;
import com.qualiapproche.ia.repository.MessageIaRepository;
import com.qualiapproche.ia.service.impl.ConversationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le déroulé d'un tour de conversation : ce qui se fait avant l'appel au modèle, pendant, et après.
 *
 * <p>Le point capital est le « pendant ». Envelopper l'appel dans une transaction retenait une
 * connexion de la réserve pendant toute la génération — six secondes sur une machine accélérée,
 * une à trois minutes sur un serveur sans carte graphique. Dix conversations simultanées
 * épuisaient la réserve par défaut, et le service entier cessait de répondre, jusqu'au catalogue
 * des questions qui n'appelle pourtant aucun modèle.</p>
 */
class TourDeConversationTest {

    private static final UUID FIL = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private ConversationIaRepository conversations;
    private MessageIaRepository messages;
    private ClientDuModele client;
    private BudgetDeJetons budget;
    private IaAssistantProperties proprietes;
    private TransactionTemplate transactions;
    private ConversationServiceImpl service;
    private MockedStatic<SecurityUtils> securite;

    /** Vrai tant que la transaction d'écriture est ouverte. */
    private final AtomicBoolean transactionOuverte = new AtomicBoolean(false);
    /** Vrai si le modèle a été appelé alors qu'une transaction était ouverte. */
    private final AtomicBoolean appelSousTransaction = new AtomicBoolean(false);

    @BeforeEach
    void preparer() {
        conversations = mock(ConversationIaRepository.class);
        messages = mock(MessageIaRepository.class);
        client = mock(ClientDuModele.class);
        budget = mock(BudgetDeJetons.class);
        proprietes = new IaAssistantProperties();
        transactions = mock(TransactionTemplate.class);

        // Le gabarit de transaction exécute son bloc, en signalant l'ouverture et la fermeture :
        // c'est ce témoin qui permet de savoir si le modèle a été appelé au-dedans.
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> bloc = invocation.getArgument(0);
            transactionOuverte.set(true);
            try {
                return bloc.doInTransaction(null);
            } finally {
                transactionOuverte.set(false);
            }
        });

        when(client.repondre(anyString(), any(List.class))).thenAnswer(invocation -> {
            appelSousTransaction.set(transactionOuverte.get());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("la réponse"))));
        });
        when(client.texteDe(any())).thenReturn("la réponse");
        when(client.jetonsOuEstimation(any(), anyString(), anyString())).thenReturn(42L);

        when(conversations.save(any())).thenAnswer(i -> {
            ConversationIa fil = i.getArgument(0);
            if (fil.getId() == null) {
                fil.setId(FIL);
            }
            return fil;
        });

        // Le catalogue rend une FAQ vide : ces cas éprouvent le déroulé d'un tour, pas la FAQ.
        CatalogueDeFaq faq = mock(CatalogueDeFaq.class);
        when(faq.pourLaConsigne()).thenReturn("");

        service = new ConversationServiceImpl(conversations, messages, new PromptRegistry(),
                client, budget, proprietes, faq, transactions);

        securite = mockStatic(SecurityUtils.class);
        securite.when(SecurityUtils::getCurrentUserId).thenReturn("agent-1");
    }

    @AfterEach
    void ranger() {
        securite.close();
    }

    private MessageDemandeDto demande(UUID conversationId) {
        MessageDemandeDto d = new MessageDemandeDto();
        d.setConversationId(conversationId);
        d.setMessage("Quelle différence entre correction et action corrective ?");
        return d;
    }

    @Test
    @DisplayName("le modèle est appelé hors de toute transaction : aucune connexion n'est retenue")
    void leModele_estAppeleHorsTransaction() {
        service.repondre(demande(null));

        assertThat(appelSousTransaction)
                .as("le modèle ne doit jamais être appelé pendant une transaction ouverte")
                .isFalse();
    }

    @Test
    @DisplayName("un fil neuf ne s'ouvre qu'à l'inscription : une panne ne laisse rien derrière")
    void filNeuf_nEstOuvertQuApresLaReponse() {
        when(client.texteDe(any())).thenReturn(null);

        // La réponse vide fait lever : aucun fil, aucun message ne doit avoir été enregistré.
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> service.repondre(demande(null))))
                .isNotNull();
        verify(conversations, never()).save(any());
        verify(messages, never()).save(any());
    }

    @Test
    @DisplayName("le budget est vérifié avant l'appel, jamais après : un refus ne coûte rien")
    void leBudget_estVerifieAvantLAppel() {
        service.repondre(demande(null));

        var ordre = org.mockito.Mockito.inOrder(budget, client);
        ordre.verify(budget).exigerDuReste();
        ordre.verify(client).repondre(anyString(), any(List.class));
    }

    @Test
    @DisplayName("la fenêtre se borne au poids, pas au seul nombre de messages")
    void laFenetre_seBorneAuPoids() {
        proprietes.setFenetreConversation(12);
        proprietes.setFenetreCaracteres(1_000);

        // Six messages de quatre cents caractères : le compte passerait, le poids non.
        List<MessageIa> anciens = new ArrayList<>();
        for (int i = 6; i >= 1; i--) {
            anciens.add(MessageIa.builder().conversationId(FIL).rang(i)
                    .role(i % 2 == 0 ? RoleMessage.ASSISTANT : RoleMessage.UTILISATEUR)
                    .contenu("m".repeat(400)).build());
        }
        when(messages.findByConversationIdOrderByRangDesc(any(), any(Pageable.class))).thenReturn(anciens);
        when(conversations.findById(FIL)).thenReturn(Optional.of(
                ConversationIa.builder().id(FIL).createdById("agent-1").nombreMessages(6).build()));

        service.repondre(demande(FIL));

        ArgumentCaptor<List<Message>> echange = ArgumentCaptor.forClass(List.class);
        verify(client).repondre(anyString(), echange.capture());

        // Deux anciens (800 caractères) tiennent sous le plafond, le troisième le dépasserait ;
        // la question du tour s'ajoute toujours.
        assertThat(echange.getValue()).hasSize(3);
    }

    @Test
    @DisplayName("les plus anciens messages sont écartés en premier : le récent porte le contexte")
    void lesPlusAnciens_sontEcartesEnPremier() {
        proprietes.setFenetreCaracteres(1_000);

        List<MessageIa> anciens = List.of(
                MessageIa.builder().rang(3).role(RoleMessage.UTILISATEUR).contenu("récent " + "r".repeat(400)).build(),
                MessageIa.builder().rang(2).role(RoleMessage.ASSISTANT).contenu("milieu " + "m".repeat(400)).build(),
                MessageIa.builder().rang(1).role(RoleMessage.UTILISATEUR).contenu("ancien " + "a".repeat(400)).build());
        when(messages.findByConversationIdOrderByRangDesc(any(), any(Pageable.class))).thenReturn(anciens);
        when(conversations.findById(FIL)).thenReturn(Optional.of(
                ConversationIa.builder().id(FIL).createdById("agent-1").nombreMessages(3).build()));

        service.repondre(demande(FIL));

        ArgumentCaptor<List<Message>> echange = ArgumentCaptor.forClass(List.class);
        verify(client).repondre(anyString(), echange.capture());

        String transmis = echange.getValue().toString();
        assertThat(transmis).contains("récent").contains("milieu");
        assertThat(transmis).doesNotContain("ancien");
    }
}
