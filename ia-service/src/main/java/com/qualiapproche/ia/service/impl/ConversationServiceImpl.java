package com.qualiapproche.ia.service.impl;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.ia.config.IaAssistantProperties;
import com.qualiapproche.ia.dto.ConversationDto;
import com.qualiapproche.ia.dto.MessageDemandeDto;
import com.qualiapproche.ia.dto.MessageDto;
import com.qualiapproche.ia.dto.ReponseConversationDto;
import com.qualiapproche.ia.entities.ConversationIa;
import com.qualiapproche.ia.entities.MessageIa;
import com.qualiapproche.ia.enumeration.RoleMessage;
import com.qualiapproche.ia.repository.ConversationIaRepository;
import com.qualiapproche.ia.repository.MessageIaRepository;
import com.qualiapproche.ia.service.BudgetDeJetons;
import com.qualiapproche.ia.service.CatalogueDeFaq;
import com.qualiapproche.ia.service.ClientDuModele;
import com.qualiapproche.ia.service.ConversationService;
import com.qualiapproche.ia.service.PromptRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    /**
     * Rappel affiché sous le fil. Il dit la limite plutôt que la capacité : c'est elle qui évite
     * qu'on prenne pour un état du dossier ce qui n'est qu'un propos de méthode.
     */
    public static final String AVERTISSEMENT =
            "Assistant IA — il ne voit pas les données de votre organisation ; vérifiez avant d'agir.";

    /** Longueur du titre tiré de la première question. */
    private static final int LONGUEUR_TITRE = 120;

    private final ConversationIaRepository conversations;
    private final MessageIaRepository messages;
    private final PromptRegistry promptRegistry;
    private final ClientDuModele client;
    private final BudgetDeJetons budget;
    private final IaAssistantProperties proprietes;
    private final CatalogueDeFaq faq;
    private final TransactionTemplate transactions;

    @Override
    public ReponseConversationDto repondre(MessageDemandeDto demande) {
        String question = demande.getMessage().trim();
        return tenirUnTour(demande.getConversationId(), question, question,
                consigneAvecLaFaq());
    }

    /**
     * La consigne de conversation, augmentée de la FAQ de l'organisation.
     *
     * <p>Relue à chaque tour plutôt que mise en cache : une réponse corrigée par un administrateur
     * doit valoir dès la question suivante, et non au prochain redémarrage du service. La lecture
     * est un index sur trois colonnes, et elle se fait dans la phase brève qui précède l'appel au
     * modèle.</p>
     *
     * <p>La version du prompt suit le contenu : deux organisations n'ont pas la même FAQ, et une
     * trace qui les confondrait rendrait l'historique des suggestions incomparable. Le suffixe
     * dit donc si la consigne portait une FAQ, et de quelle taille.</p>
     */
    private PromptRegistry.PromptVersionne consigneAvecLaFaq() {
        PromptRegistry.PromptVersionne base = promptRegistry.promptDeConversation();
        String catalogue = faq.pourLaConsigne();
        if (catalogue.isEmpty()) {
            return base;
        }
        return new PromptRegistry.PromptVersionne(
                base.version() + "+faq" + catalogue.length(), base.contenu() + catalogue);
    }

    @Override
    public ReponseConversationDto repondreSurMatiere(UUID conversationId, String questionAffichee,
                                                     String matiere) {
        // Ce que le fil garde, ce que le modèle reçoit : la question d'un côté, la question et ses
        // données de l'autre. Voir la javadoc de l'interface.
        String transmis = questionAffichee + "\n\nDonnées :\n" + matiere;
        return tenirUnTour(conversationId, questionAffichee, transmis,
                promptRegistry.promptDeCommentaireDeDonnees());
    }

    /**
     * Un tour de conversation, quelle qu'en soit l'origine.
     *
     * <p>Les deux chemins — question libre, question prédéfinie — partagent tout ce qui compte :
     * le budget vérifié avant l'appel, la borne du fil, la fenêtre glissante, l'inscription des
     * deux tours une fois la réponse obtenue. Les écrire deux fois aurait fait diverger le
     * décompte des jetons, et la conversation aurait fini par échapper au plafond par l'un des
     * deux bouts.</p>
     */
    private ReponseConversationDto tenirUnTour(UUID conversationId, String questionAffichee,
                                               String questionTransmise,
                                               PromptRegistry.PromptVersionne consigne) {
        String question = questionAffichee.trim();

        // 1. Ce qu'il faut savoir avant d'appeler : des lectures brèves, refermées aussitôt.
        budget.exigerDuReste();
        List<Message> echange = new ArrayList<>();
        if (conversationId != null) {
            exigerDeLaPlace(sienneOuRien(conversationId));
            echange.addAll(fenetreDuFil(conversationId));
        }
        echange.add(new UserMessage(questionTransmise));

        // 2. Le temps long, hors de toute transaction : aucune connexion n'est retenue pendant
        //    que le modèle écrit.
        long debut = System.currentTimeMillis();
        ChatResponse reponse = client.repondre(consigne.contenu(), echange);
        long dureeMs = System.currentTimeMillis() - debut;

        String texte = client.texteDe(reponse);
        if (texte == null || texte.isBlank()) {
            throw new BusinessException(
                    "L'assistant n'a produit aucune réponse.", HttpStatus.BAD_GATEWAY);
        }

        // 3. L'inscription des deux tours, atomique et brève.
        long jetons = client.jetonsOuEstimation(reponse, consigne.contenu() + questionTransmise, texte);
        return transactions.execute(statut ->
                inscrire(conversationId, question, texte, reponse, consigne, dureeMs, jetons));
    }

    /**
     * Inscrit la question et la réponse, et rend ce que l'écran affiche.
     *
     * <p>Le fil n'est ouvert qu'ici, jamais avant l'appel : une génération qui échoue ne laisse
     * donc ni question orpheline, ni fil ouvert sur rien. C'est la garantie que donnait la
     * transaction unique, conservée sans son coût.</p>
     *
     * <p>Il est relu plutôt que repris de la phase de lecture : entre-temps, un autre message a
     * pu s'y inscrire, et son compte d'alors servirait de rang à deux messages.</p>
     */
    private ReponseConversationDto inscrire(UUID conversationId, String question, String texte,
                                            ChatResponse reponse,
                                            PromptRegistry.PromptVersionne consigne,
                                            long dureeMs, long jetons) {
        ConversationIa fil = conversationId != null
                ? sienneOuRien(conversationId)
                : ouvrirUnFil(question);

        int rang = fil.getNombreMessages();
        messages.save(MessageIa.builder()
                .conversationId(fil.getId())
                .role(RoleMessage.UTILISATEUR)
                .contenu(question)
                .rang(++rang)
                .build());
        messages.save(MessageIa.builder()
                .conversationId(fil.getId())
                .role(RoleMessage.ASSISTANT)
                .contenu(texte)
                .rang(++rang)
                .modele(client.modeleDe(reponse))
                .promptVersion(consigne.version())
                .dureeMs(dureeMs)
                .jetonsUtilises(jetons)
                .build());

        fil.setNombreMessages(rang);
        fil.setDerniereActiviteAt(LocalDateTime.now());
        conversations.save(fil);

        return ReponseConversationDto.builder()
                .conversationId(fil.getId())
                .reponse(texte)
                .avertissement(AVERTISSEMENT)
                .messagesRestants(Math.max(0, proprietes.getMaxMessagesConversation() - rang))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDto> mesConversations(Pageable pageable) {
        String utilisateurId = SecurityUtils.getCurrentUserId();
        if (utilisateurId == null) {
            return Page.empty(pageable);
        }
        return conversations.findAllByCreatedByIdOrderByDerniereActiviteAtDesc(utilisateurId, pageable)
                .map(fil -> versDto(fil, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDto conversation(UUID id) {
        ConversationIa fil = sienneOuRien(id);
        List<MessageDto> echanges = messages.findAllByConversationIdOrderByRangAsc(id).stream()
                .map(this::versDto)
                .toList();
        return versDto(fil, echanges);
    }

    // ---------------------------------------------------------------- périmètre et bornes

    /**
     * Le fil demandé, s'il appartient à l'appelant — sinon rien, et le mot est juste.
     *
     * <p>Un fil qui n'est pas le sien est traité comme inexistant, et non comme interdit. C'est
     * l'inverse de ce que fait la lecture d'un dossier, où le 403 est préférable parce que le
     * périmètre demandé existe et que l'appelant le sait déjà. Ici, répondre « elle existe mais
     * n'est pas à vous » dirait déjà quelque chose : qu'un collègue a ouvert une conversation. Ce
     * qu'on écrit à un assistant est privé, y compris son existence.</p>
     */
    private ConversationIa sienneOuRien(UUID id) {
        String utilisateurId = SecurityUtils.getCurrentUserId();
        return conversations.findById(id)
                .filter(fil -> utilisateurId != null && utilisateurId.equals(fil.getCreatedById()))
                .orElseThrow(() -> new BusinessException(
                        "Aucune conversation ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    private ConversationIa ouvrirUnFil(String premiereQuestion) {
        return conversations.save(ConversationIa.builder()
                .titre(titreDe(premiereQuestion))
                .derniereActiviteAt(LocalDateTime.now())
                .nombreMessages(0)
                .build());
    }

    /**
     * Un fil a une fin. Atteinte, elle n'efface rien : elle invite à en ouvrir un autre, ce qui
     * repart d'un contexte propre — et coupe court à la conversation sans fin qui viderait le
     * budget de toute une structure.
     */
    private void exigerDeLaPlace(ConversationIa fil) {
        int maximum = proprietes.getMaxMessagesConversation();
        if (maximum > 0 && fil.getNombreMessages() >= maximum) {
            throw new BusinessException(
                    "Cette conversation a atteint sa longueur maximale (" + maximum
                            + " messages). Ouvrez-en une nouvelle pour poursuivre.",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Les derniers messages du fil, remis dans l'ordre où ils ont été dits.
     *
     * <p>Le dépôt les rend du plus récent au plus ancien — c'est ainsi qu'on prend « les
     * derniers » sans lire le fil entier — et le modèle, lui, a besoin de l'ordre du dialogue.</p>
     */
    private List<Message> fenetreDuFil(UUID conversationId) {
        List<MessageIa> derniers = messages.findByConversationIdOrderByRangDesc(
                conversationId, PageRequest.of(0, Math.max(1, proprietes.getFenetreConversation())));

        // Deux bornes, et la plus stricte l'emporte. Le compte de messages ne dit rien de leur
        // poids : douze messages longs, plus la consigne, dépassent la fenêtre de contexte d'un
        // petit modèle, qui tronque alors par le début — c'est-à-dire par la consigne système,
        // qu'il cesse donc de suivre précisément dans les conversations les plus longues. On
        // remonte du plus récent au plus ancien et on s'arrête au plafond de caractères : ce qui
        // est écarté est toujours le plus vieux.
        int plafond = Math.max(1, proprietes.getFenetreCaracteres());
        List<MessageIa> retenus = new ArrayList<>(derniers.size());
        int poids = 0;
        for (MessageIa message : derniers) {
            int taille = message.getContenu() == null ? 0 : message.getContenu().length();
            if (!retenus.isEmpty() && poids + taille > plafond) {
                break;
            }
            poids += taille;
            retenus.add(message);
        }
        Collections.reverse(retenus);

        List<Message> echange = new ArrayList<>(retenus.size() + 1);
        for (MessageIa message : retenus) {
            echange.add(message.getRole() == RoleMessage.ASSISTANT
                    ? new AssistantMessage(message.getContenu())
                    : new UserMessage(message.getContenu()));
        }
        return echange;
    }

    private String titreDe(String question) {
        String propre = question.replaceAll("\\s+", " ").trim();
        return propre.length() <= LONGUEUR_TITRE ? propre
                : propre.substring(0, LONGUEUR_TITRE).trim() + "…";
    }

    private ConversationDto versDto(ConversationIa fil, List<MessageDto> echanges) {
        return ConversationDto.builder()
                .id(fil.getId())
                .titre(fil.getTitre())
                .nombreMessages(fil.getNombreMessages())
                .derniereActiviteAt(fil.getDerniereActiviteAt())
                .messages(echanges)
                .build();
    }

    private MessageDto versDto(MessageIa message) {
        return MessageDto.builder()
                .role(message.getRole())
                .contenu(message.getContenu())
                .rang(message.getRang())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
