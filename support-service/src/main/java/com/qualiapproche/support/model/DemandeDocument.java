package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Demande de modification ou de suppression portant sur un document existant.
 *
 * <p>Elle suit un circuit de validation comme le document lui-même — famille
 * {@code DEMANDE_DOCUMENT} — et c'est ce qui lui donne des étapes, des responsables, une
 * traçabilité et des notifications sans qu'il faille les réinventer.</p>
 *
 * <p>Ce qui la distingue d'un simple changement d'état : son aboutissement agit sur le document.
 * Une modification acceptée ouvre le dépôt d'un fichier remplaçant, qui fait monter la version
 * majeure ; une suppression acceptée retire le document. La demande, elle, subsiste — avec son
 * circuit et sa piste d'audit — car c'est la seule trace de ce qui a été décidé et pourquoi.</p>
 */
@Entity
@Table(name = "qms_demandes_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DemandeDocument extends AuditEntity {

    public enum TypeDemande { MODIFICATION, SUPPRESSION }

    /**
     * État de la demande, du point de vue de support-service.
     *
     * <p>{@code EN_COURS} tant que le circuit n'a pas rendu sa décision ; {@code ACCEPTEE} ouvre
     * l'aboutissement ; {@code EXECUTEE} le constate. La distinction compte : une modification
     * acceptée reste à exécuter tant que le fichier remplaçant n'a pas été déposé.</p>
     */
    public enum EtatDemande { EN_COURS, ACCEPTEE, REFUSEE, EXECUTEE }

    /** Document visé. Conservé par identifiant : il peut disparaître, la demande lui survit. */
    @Column(nullable = false)
    private UUID documentId;

    /** Numéro du document au moment de la demande, pour que la trace reste lisible après retrait. */
    private String documentNumber;
    private String documentTitre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDemande type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EtatDemande etat = EtatDemande.EN_COURS;

    /** Ce que la demande cherche à obtenir. */
    @Column(nullable = false, length = 1000)
    private String objectif;

    @Column(length = 4000)
    private String description;

    /** Structure et demandeur : pris de l'utilisateur connecté, jamais saisis. */
    private String structureId;
    private String structureLibelle;
    private String demandeurId;
    private String demandeurNom;

    /** Pièce jointe éventuelle, rangée dans le même stockage que les documents. */
    private String pieceJointeObjectName;
    private String pieceJointeNom;

    // ------------------------------------------------------------------ circuit
    private UUID workflowId;
    /** Étape courante du circuit, telle que la renvoie workflow-service. */
    private String currentEtape;

    private LocalDateTime dateDecision;
    /** Motif de la décision finale, repris du commentaire porté par la dernière transition. */
    @Column(length = 2000)
    private String motifDecision;

    private LocalDateTime dateExecution;
}
