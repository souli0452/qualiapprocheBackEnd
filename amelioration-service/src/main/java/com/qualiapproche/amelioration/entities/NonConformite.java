package com.qualiapproche.amelioration.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import com.qualiapproche.common.base.Participants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(name = "quali_nc")
@Schema(description = "Entité principale représentant une fiche de Non-Conformité qualité.")
public class NonConformite extends AuditEntity {

    // =========================================================================
    // 1. IDENTIFICATION & SUIVI DU WORKFLOW
    // =========================================================================

    @Schema(description = "Numéro d'immatriculation unique lisible", example = "NFQT-GAI-2026-00003")
    @Column(name = "numero_reference", nullable = false)
    private String numeroDeReference;

    @Schema(description = "Numéro de version du document de déclaration", example = "1.0")
    private String version;

    @Schema(description = "Type de demande géré par ce module", example = "NON_CONFORMITE")
    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande;

    @Schema(description = "Statut macro du cycle de vie (DRAFT, IN_PROGRESS, APPROVED, REJECTED, ARCHIVED)")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Schema(description = "État métier précis correspondant à l'étape en cours", example = "RECEPTION")
    @Enumerated(EnumType.STRING)
    private Etat etatDeTraitement;

    @Schema(description = "Identifiant de la définition de workflow associée")
    @Column(name = "workflow_id")
    private UUID workflowId;

    @Schema(description = "Libellé de l'étape courante dans le circuit de validation", example = "Réception")
    @Column(name = "workflow_status")
    private String workflowStatus;

    // =========================================================================
    // 2. ÉMETTEUR & CONTEXTE DE SOUMISSION
    // =========================================================================

    @Schema(description = "Identifiant de la structure organisationnelle qui émet la déclaration")
    private String structureDeSoumissionId;

    @Schema(description = "Sigle ou nom court de la structure émettrice", example = "GAI")
    private String structureDeSoumissionLibelle;

    @Schema(description = "Intitulé du poste ou fonction de l'agent qui déclare l'anomalie")
    private String fonctionEmetteur;

    @Schema(description = "Date et heure de signature/validation de la déclaration par l'émetteur")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateVisaEmetteur;

    // =========================================================================
    // 3. QUALIFICATION & DESCRIPTION DE L'ANOMALIE
    // =========================================================================

    @Schema(
        description = "Description factuelle et détaillée du constat ou dysfonctionnement observé (Le QUOI, OÙ et COMMENT)",
        example = "Écart constaté lors du contrôle de réception : lot non étiqueté."
    )
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Ancien 'justification'

    @Schema(description = "Identifiant de la source/origine de détection de la non-conformité")
    private UUID sourceDeNonConformiteId; // Ancien 'sourceNonConformiteId'

    @Schema(description = "Libellé de la source (ex: Audit Interne, Réclamation Client, Contrôle Qualité)")
    private String sourceDeNonConformiteLibelle;

    @Schema(description = "Identifiant du niveau de gravité de la non-conformité")
    private UUID niveauNonConformiteId;

    @Schema(description = "Libellé de la gravité (ex: Mineure, Majeure, Critique)", example = "Majeure")
    private String niveauNonConformiteLibelle;

    @Schema(description = "Identifiant de la catégorie de processus ISO (Management, Réalisation, Support)")
    private UUID categorieProcessusId; // Ancien 'categorieProcessusId'

    @Schema(description = "Libellé de la catégorie de processus", example = "Direction")
    private String categorieProcessusLibelle;

    @Schema(description = "Nom précis du processus métier concerné", example = "Gestion des Approvisionnements")
    private String nomProcessus;

    // =========================================================================
    // 4. AFFECTATION & TRAITEMENT
    // =========================================================================

    @Schema(description = "Identifiant de la structure en charge de traiter l'anomalie")
    private String structureResponsableId;

    @Schema(description = "Sigle de la structure responsable", example = "DIR-QUALITE")
    private String structureResponsableSigle;

    @Schema(description = "Libellé complet de la structure responsable", example = "Direction Qualité")
    private String structureResponsableLibelle;

    @Schema(description = "Identifiant de l'agent désigné pour le traitement (imputation)")
    private String agentImputeId; // Ancien 'userImputId'

    @Schema(description = "Nom complet de l'agent désigné", example = "Jean Dupont")
    private String agentImputeNomComplet; // Ancien 'userImputFullName'

    @Schema(description = "Adresse email de l'agent désigné", example = "j.dupont@entreprise.com")
    private String agentImputeEmail; // Ancien 'userImputeEmail'

    @Schema(description = "Actions curatives immédiates prises dès le constat", example = "Mise en quarantaine immédiate du lot.")
    @Lob
    @Column(name = "action_immediate", columnDefinition = "TEXT") // Ancien 'actionDsc'
    private String actionImmediate;

    @Schema(description = "Délai estimé ou fixé pour la mise en œuvre de la solution", example = "15 jours")
    private String delaisMiseOeuvre;

    // =========================================================================
    // 5. AVIS DU PILOTE & RESPONSABLE DE STRUCTURE
    // =========================================================================

    @Schema(description = "Avis du Pilote sur la pertinence de la déclaration (FONDÉE / NON_FONDÉE)")
    private String pertinencePilote;

    @Schema(description = "Argumentation ou justification donnée par le pilote du processus")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String observationPilote;

    @Schema(description = "Avis du Responsable de Structure (RS) sur la prise en charge")
    private String pertinenceRs;

    @Schema(description = "Justification donnée par le responsable de structure")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String observationRs;

    // =========================================================================
    // 6. GESTION DU REJET & CLÔTURE DU DOSSIER
    // =========================================================================

    @Schema(description = "Motif ou commentaire en cas de rejet du dossier lors du workflow")
    @Lob
    @Column(name = "observation_rejet", columnDefinition = "TEXT")
    private String observationRejet;

    @Schema(description = "Document officiel ou preuve justificative du rejet déposé au fil de l'étape")
    @ManyToOne
    private PieceJointe docRejet;

    @Schema(description = "Observations et remarques du Responsable Qualité (RQ) sur le traitement")
    @Lob
    @Column(name = "observation_rq", columnDefinition = "TEXT")
    private String observationRq;

    @Schema(description = "Observations finales lors de la clôture définitive du dossier")
    @Lob
    @Column(name = "observations_cloture", columnDefinition = "TEXT")
    private String observationsCloture;

    @Schema(description = "Date de clôture effective par le Responsable Qualité")
    private String dateClotureRq;

    @Schema(description = "Libellé de l'évaluation d'efficacité de la résolution (EFFICACE / NON_EFFICACE)")
    private String efficaciteLibelle;

    // =========================================================================
    // 7. RELATIONS & COMPOSANTS ASSOCIÉS
    // =========================================================================

    @Schema(description = "Liste des pièces jointes et preuves documentaires déposées sur la fiche")
    @OneToMany
    private List<PieceJointe> fichiers = new ArrayList<>();

    @Schema(description = "Liste des plans d'action correctifs ou préventifs engagés pour ce dossier")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanAction> planActions = new ArrayList<>();

    @Embedded
    private Participants participants = new Participants();

    // --- CHAMPS COMPLÉMENTAIRES / ARCHIVAGE / ANCIEN CIRCUIT ---
    @Enumerated(EnumType.STRING)
    private Circuit circuit;

    private String origineId;
    private String origineService;
    private String origineServiceLibelleCourt;
    private String actionLibelle;

    private UUID actionId;
    private String originNonConformiteId;
    private String originNonConformiteLibelle;
    private UUID efficaciteId;
    private String actionPreventive;
    private String pertinanceRsSuivi;
    private String numeroFdac;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateSuivi;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime publicationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime archivageDate;


    // --- ALIAS DE COMPATIBILITÉ ASCENDANTE ---
    public String getNumeroReference() {
        return this.numeroDeReference;
    }
    public void setNumeroReference(String v) {
        this.numeroDeReference = v;
    }

    public String getJustification() {
        return this.description;
    }
    public void setJustification(String v) {
        this.description = v;
    }

    public Etat getEtatTraitement() {
        return this.etatDeTraitement;
    }
    public void setEtatTraitement(Etat v) {
        this.etatDeTraitement = v;
    }

    public String getStructureSoumissionId() {
        return this.structureDeSoumissionId;
    }
    public void setStructureSoumissionId(String v) {
        this.structureDeSoumissionId = v;
    }

    public String getStructureSoumissionLibelle() {
        return this.structureDeSoumissionLibelle;
    }
    public void setStructureSoumissionLibelle(String v) {
        this.structureDeSoumissionLibelle = v;
    }

    public UUID getSourceNonConformiteId() {
        return this.sourceDeNonConformiteId;
    }
    public void setSourceNonConformiteId(UUID v) {
        this.sourceDeNonConformiteId = v;
    }

    public String getSourceNonConformiteLibelle() {
        return this.sourceDeNonConformiteLibelle;
    }
    public void setSourceNonConformiteLibelle(String v) {
        this.sourceDeNonConformiteLibelle = v;
    }

    public UUID getCategorieProcessusId() {
        return this.categorieProcessusId;
    }
    public void setCategorieProcessusId(UUID v) {
        this.categorieProcessusId = v;
    }

    public String getCategorieProcessusLibelle() {
        return this.categorieProcessusLibelle;
    }
    public void setCategorieProcessusLibelle(String v) {
        this.categorieProcessusLibelle = v;
    }

    public String getActionDsc() {
        return this.actionImmediate;
    }
    public void setActionDsc(String v) {
        this.actionImmediate = v;
    }

    public String getUserImputId() {
        return this.agentImputeId;
    }
    public void setUserImputId(String v) {
        this.agentImputeId = v;
    }

    public String getUserImputFullName() {
        return this.agentImputeNomComplet;
    }
    public void setUserImputFullName(String v) {
        this.agentImputeNomComplet = v;
    }

    public String getUserImputeEmail() {
        return this.agentImputeEmail;
    }
    public void setUserImputeEmail(String v) {
        this.agentImputeEmail = v;
    }

    public String getPertinanceRs() {
        return this.pertinenceRs;
    }
    public void setPertinanceRs(String v) {
        this.pertinenceRs = v;
    }

    public String getPertinancePilote() {
        return this.pertinencePilote;
    }
    public void setPertinancePilote(String v) {
        this.pertinencePilote = v;
    }

}
