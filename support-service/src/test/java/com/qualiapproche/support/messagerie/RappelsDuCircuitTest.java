package com.qualiapproche.support.messagerie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.messagerie.ConvertisseurDesMessages;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import com.qualiapproche.common.messagerie.RappelDuCircuit;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.support.service.DemandeDocumentService;
import com.qualiapproche.support.service.QmsDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Les rappels du circuit arrivent par le courtier : un document voit son état et sa piste d'audit
 * mis à jour, une demande son avancement — une seule fois.
 */
class RappelsDuCircuitTest {

    private final QmsDocumentService documents = mock(QmsDocumentService.class);
    private final DemandeDocumentService demandes = mock(DemandeDocumentService.class);
    @SuppressWarnings("unchecked")
    private final Dedoublonnage<MessageTraite> dedoublonnage = mock(Dedoublonnage.class);
    private final RappelsDuCircuit rappels = new RappelsDuCircuit(documents, demandes, dedoublonnage,
            new LecteurDeMessages(new ConvertisseurDesMessages(new ObjectMapper())));
    private final UUID dossier = UUID.randomUUID();

    @BeforeEach
    void premiereLivraison() {
        when(dedoublonnage.premiereLivraison(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Un document : son état suit le circuit, et la transition entre dans sa piste d'audit")
    void document() {
        rappels.recevoir(RappelDuCircuit.builder().typeRessource("DOCUMENT").ressourceId(dossier)
                .contenu(Map.of("status", "APPROVED", "statusName", "Approbation", "decision", "VALIDATE")).build(), "m-1");

        verify(documents).updateWorkflowStatus(dossier, "APPROVED", "Approbation", null);
        verify(documents).logWorkflowAudit(dossier, "TRANSITION_VALIDATE", "Passage à l'étape « Approbation »");
    }

    @Test
    @DisplayName("Une demande : son avancement est traité avec tout le contenu du rappel")
    void demande() {
        Map<String, Object> contenu = Map.of("status", "APPROVED", "etatCode", "ACCEPTEE");
        rappels.recevoir(RappelDuCircuit.builder().typeRessource("DEMANDE_DOCUMENT").ressourceId(dossier)
                .contenu(contenu).build(), "m-2");

        verify(demandes).traiterAvancement(dossier, contenu);
    }

    @Test
    @DisplayName("Une livraison en double est écartée")
    void livraisonEnDouble() {
        when(dedoublonnage.premiereLivraison("m-1")).thenReturn(false);

        rappels.recevoir(RappelDuCircuit.builder().typeRessource("DOCUMENT").ressourceId(dossier).build(), "m-1");

        verifyNoInteractions(documents, demandes);
    }

    @Test
    @DisplayName("La piste d'audit du document porte le nom de la personne qui a décidé")
    void attribueeAuDecideur() {
        AtomicReference<String> nom = new AtomicReference<>();
        doAnswer(i -> {
            nom.set(SecurityUtils.getAuteurNom());
            return null;
        }).when(documents).logWorkflowAudit(any(), any(), any());

        rappels.recevoir(RappelDuCircuit.builder().typeRessource("DOCUMENT").ressourceId(dossier)
                .contenu(Map.of("status", "APPROVED", "statusName", "Approbation", "decision", "VALIDATE"))
                .auteurId("u-7").auteurNom("Awa Ouédraogo").build(), "m-8");

        assertThat(nom.get()).isEqualTo("Awa Ouédraogo");
    }

    @Test
    @DisplayName("Un rappel sans type ni dossier est écarté sans erreur")
    void rappelIncomplet() {
        rappels.recevoir(RappelDuCircuit.builder().ressourceId(dossier).build(), "m-9");

        verifyNoInteractions(documents, demandes, dedoublonnage);
    }
}
