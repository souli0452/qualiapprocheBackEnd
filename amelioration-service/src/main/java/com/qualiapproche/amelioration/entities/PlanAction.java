package com.qualiapproche.amelioration.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.utils.StatutEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@Entity
@Table(name = "plan_action")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(description = "Plan d'action correctif ou préventif mis en place pour traiter une non-conformité.")
public class PlanAction extends AuditEntity {

    @Schema(description = "Numéro d'ordre de l'action dans la liste (ex: 1, 2, 3)", example = "1")
    @Column(name = "numero_ordre")
    private String numeroOrdre;

    @Schema(description = "Identifiant unique de la non-conformité parente à laquelle ce plan est rattaché")
    @Column(name = "non_conformite_id")
    private UUID nonConformiteId;

    @Schema(
        description = "Analyse approfondie de la cause racine ayant conduit à la non-conformité (ex: méthode 5 Pourquoi ou diagramme d'Ishikawa)",
        example = "Absence de procédure de contrôle documentée lors du changement d'équipe."
    )
    @Lob
    @Column(name = "cause_identifiee", columnDefinition = "TEXT")
    private String causeIdentifiee;

    @Schema(
        description = "Solution stratégique retenue pour éradiquer définitivement la cause racine",
        example = "Mise en place d'une checklist de transmission obligatoire entre chaque vacation."
    )
    @Lob
    @Column(name = "solution_retenue", columnDefinition = "TEXT")
    private String solutionRetenue;

    @Schema(
        description = "Description opérationnelle et concrète des tâches à exécuter",
        example = "Rédiger la fiche de poste, former les 3 chefs d'équipe et intégrer le formulaire au classeur d'atelier."
    )
    @Lob
    @Column(name = "action_corrective", columnDefinition = "TEXT")
    private String actionCorrective;

    @Schema(description = "Nom complet de la personne chargée de réaliser ou de piloter l'action", example = "Jean Dupont")
    @Column(name = "responsable_nom_complet")
    private String responsableNomComplet;

    @Schema(description = "Identifiant de l'utilisateur responsable")
    @Column(name = "responsable_id")
    private UUID responsableId;

    @Schema(description = "Adresse email du responsable", example = "responsable@entreprise.com")
    @Column(name = "responsable_email")
    private String responsableEmail;

    @Schema(description = "Numéro de téléphone du responsable")
    @Column(name = "numero_telephone")
    private String numeroTelephone;

    @Schema(description = "Date limite impérative pour l'achèvement complet de l'action")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Schema(description = "Date réelle à laquelle l'action a été traitée et achevée")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "date_traitement")
    private LocalDate dateTraitement;

    @Schema(description = "Statut d'avancement du plan d'action")
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatutEnum status;

    @Schema(description = "Référence lisible de la non-conformité parente")
    @Column(name = "numero_nc")
    private String numeroNc;

    @Schema(description = "Processus émetteur")
    @Column(name = "proc_emetteur")
    private String procEmetteur;

    @Schema(description = "Observations générales sur l'action")
    @Lob
    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Schema(description = "Observations ou motifs en cas de rejet")
    @Lob
    @Column(name = "observation_rejet", columnDefinition = "TEXT")
    private String observationRejet;

    @Schema(description = "Date de rejet le cas échéant")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    @Column(name = "date_rejet")
    private LocalDate dateRejet;

    @Schema(
        description = "Indicateur ou critère objectif permettant de mesurer si l'action a réellement été efficace (obligation ISO 9001)",
        example = "Zéro récidive de l'anomalie constatée lors des audits des 3 prochains mois."
    )
    @Column(name = "critere_efficacite", columnDefinition = "TEXT")
    private String critereEfficacite;

    @Schema(
        description = "Constat factuel relevé lors de l'évaluation de l'efficacité de l'action après son échéance",
        example = "Contrôle réalisé le 15/10 : aucun écart constaté sur 45 transmissions d'équipe."
    )
    @Lob
    @Column(name = "constat_efficacite", columnDefinition = "TEXT")
    private String constatEfficacite;

    @Schema(description = "Identifiant de l'instance de workflow associée à l'action")
    @Column(name = "workflow_id")
    private UUID workflowId;

    @Schema(description = "Libellé de l'étape courante du workflow")
    @Column(name = "workflow_status")
    private String workflowStatus;

    // --- ALIAS DE COMPATIBILITÉ ASCENDANTE ---
    public String getNumeroOdre() {
        return this.numeroOrdre;
    }
    public void setNumeroOdre(String v) {
        this.numeroOrdre = v;
    }

    public UUID getNonConformeId() {
        return this.nonConformiteId;
    }
    public void setNonConformeId(UUID v) {
        this.nonConformiteId = v;
    }

    public String getCauseIdentifiees() {
        return this.causeIdentifiee;
    }
    public void setCauseIdentifiees(String v) {
        this.causeIdentifiee = v;
    }

    public String getSolutionRetenues() {
        return this.solutionRetenue;
    }
    public void setSolutionRetenues(String v) {
        this.solutionRetenue = v;
    }
}
