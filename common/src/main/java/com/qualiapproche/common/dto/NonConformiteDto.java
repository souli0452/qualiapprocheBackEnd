package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
@Transactional
@Schema(description = "Fiche de non-conformité : ce qui a été constaté, qui en répond, et où en "
        + "est son traitement. Les champs vides sont omis de la réponse.")
public class NonConformiteDto extends AuditEntityDto {

    // ---------------------------------------------------------------- identification et circuit

    @Schema(description = "Numéro d'immatriculation lisible du dossier, attribué par le serveur à "
            + "la création. C'est lui que citent les courriels et la fiche de clôture.",
            example = "NFQT-GAI-2026-00003")
    private String numeroReference;

    @Schema(description = "Version de la déclaration.", example = "1.0")
    private String version;

    @Schema(description = "Nature de la demande. Ce module ne traite que les non-conformités.",
            example = "NON_CONFORMITE")
    @Enumerated(EnumType.STRING)
    private TypeDemande typeDemande;

    @Schema(description = "Statut macroscopique du cycle de vie : brouillon, publiée, traitée, "
            + "rejetée, archivée.", example = "IN_PROGRESS")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Schema(description = "État métier du dossier, correspondant à l'étape franchie. Une étape que "
            + "l'énumération ne sait pas nommer laisse le dossier sur son état précédent.",
            example = "IMPUTATION")
    @Enumerated(EnumType.STRING)
    private Etat etatTraitement;

    @Schema(description = "Circuit de traitement retenu par le responsable qualité. Il commande ce "
            + "que les plans d'action devront porter : en correction, la cause n'est pas exigée.",
            example = "ACTION_CORRECTIVE")
    @Enumerated(EnumType.STRING)
    private Circuit circuit;

    @Schema(description = "Identifiant de l'instance de circuit qui suit le dossier.")
    private UUID workflowId;

    @Schema(description = "Nom de l'étape courante, tel que le circuit la nomme.",
            example = "Validation RQ")
    private String workflowStatus;

    /**
     * État du circuit de validation : étape courante, actions autorisées pour l'utilisateur
     * appelant et champs à saisir. Alimenté à la consultation d'une non-conformité, à l'image
     * de ce que fait déjà le service documentaire — sans quoi les écrans ne pouvaient afficher
     * aucune action de workflow.
     */
    @Schema(description = "État du circuit : étape courante, décisions ouvertes à l'appelant et "
            + "champs qu'elles réclament. Alimenté à la consultation d'un dossier ; sans lui, "
            + "l'écran ne peut offrir aucune action.")
    private WorkflowStateDto workflowState;

    // ---------------------------------------------------------------- émetteur et soumission

    @Schema(description = "Identifiant de la structure qui déclare le dossier.")
    private String structureSoumissionId;

    @Schema(description = "Nom de la structure émettrice, conservé tel qu'il était à la "
            + "déclaration.", example = "Direction des achats")
    private String structureSoumissionLibelle;

    @Schema(description = "Fonction de l'agent qui déclare, telle qu'il la renseigne.",
            example = "Contrôleur qualité")
    private String fonctionEmetteur;

    @Schema(description = "Date et heure du visa de l'émetteur.", example = "02-03-2026 à 09:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateVisaEmetteur;

    @Schema(description = "Identifiant de la structure d'origine, quand le dossier a été transféré "
            + "depuis une autre que celle qui l'a soumis.")
    private String origineId;

    @Schema(description = "Nom du service d'origine.", example = "Magasin central")
    private String origineService;

    @Schema(description = "Sigle du service d'origine.", example = "MC")
    private String origineServiceLibelleCourt;

    // ---------------------------------------------------------------- qualification du constat

    @Schema(description = "Description factuelle de l'écart constaté : le quoi, l'où et le "
            + "comment.",
            example = "Écart au contrôle de réception : lot livré sans étiquetage.")
    private String justification;

    @Schema(description = "Identifiant de l'origine du constat (audit interne, réclamation "
            + "client…).")
    private UUID typeNonConformiteId;

    @Schema(description = "Libellé de l'origine du constat, figé au moment du choix.",
            example = "Audit interne")
    private String typeNonConformiteLibelle;

    @Schema(description = "Identifiant du niveau de gravité.")
    private UUID niveauNonConformiteId;

    @Schema(description = "Libellé de la gravité, figé au moment du choix.", example = "Majeure")
    private String niveauNonConformiteLibelle;

    @Schema(description = "Identifiant de la famille de processus concernée.")
    private UUID typeProcessusId;

    @Schema(description = "Libellé de la famille de processus.", example = "Réalisation")
    private String typeProcessusLibelle;

    @Schema(description = "Nom du processus métier visé.", example = "Approvisionnements")
    private String nomProcessus;

    // ---------------------------------------------------------------- affectation et traitement

    @Schema(description = "Identifiant de la structure chargée de traiter le dossier, désignée à "
            + "la validation qualité.")
    private String structureResponsableId;

    @Schema(description = "Sigle de la structure responsable.", example = "DQ")
    private String structureResponsableSigle;

    @Schema(description = "Nom de la structure responsable.", example = "Direction qualité")
    private String structureResponsableLibelle;

    @Schema(description = "Identifiant de l'agent à qui le dossier est imputé. C'est cette valeur, "
            + "et non le titulaire porté par le circuit, que lisent les listes du module.")
    private String userImputId;

    @Schema(description = "Nom de l'agent imputé.", example = "Awa Traoré")
    private String userImputFullName;

    @Schema(description = "Adresse de l'agent imputé.", example = "a.traore@exemple.bf")
    private String userImputeEmail;

    @Schema(description = "Mesures prises immédiatement au constat, avant tout traitement de fond.",
            example = "Mise en quarantaine du lot.")
    private String actionDsc;

    @Schema(description = "Délai annoncé pour la mise en œuvre.", example = "15 jours")
    private String delaisMiseOeuvre;

    @Schema(description = "Agents associés au traitement, en plus de celui qui en répond.")
    private Set<String> participants = new HashSet<>();

    // ---------------------------------------------------------------- avis du pilote et du RS

    @Schema(description = "Avis du pilote de processus sur le bien-fondé de la déclaration.",
            example = "FONDEE")
    private String pertinancePilote;

    @Schema(description = "Ce que le pilote motive à l'appui de son avis.")
    private String justificationPilote;

    @Schema(description = "Avis du responsable de structure sur la prise en charge.",
            example = "FONDEE")
    private String pertinanceRs;

    @Schema(description = "Ce que le responsable de structure motive à l'appui de son avis.")
    private String justificationRs;

    @Schema(description = "Avis du responsable de structure au moment du suivi, distinct de celui "
            + "rendu à la prise en charge.")
    private String pertinanceRsSuivi;

    // ---------------------------------------------------------------- rejet

    @Schema(description = "Motif du renvoi du dossier, saisi à l'étape qui l'a refusé.")
    private String observationRejet;

    @Schema(description = "Pièce déposée à l'appui du rejet, au fil du circuit. Elle ne figure pas "
            + "parmi les fichiers de la fiche : l'enregistrement du dossier l'aurait effacée.")
    private PieceJointeDTO docRejet;

    // ---------------------------------------------------------------- suivi et clôture

    @Schema(description = "Observations du responsable qualité sur le traitement.")
    private String observationsRq;

    @Schema(description = "Date de ces observations.")
    private String dateObservationsRq;

    @Schema(description = "Date de la vérification du traitement.")
    private String dateVerification;

    @Schema(description = "Dispositions prises pour empêcher la réapparition de l'écart.")
    private String dispositionPreventives;

    @Schema(description = "Action préventive retenue, distincte de la correction de l'écart "
            + "constaté.")
    private String actionPreventive;

    @Schema(description = "Observations portées à la clôture définitive.")
    private String observationsCloture;

    @Schema(description = "Date de clôture par le responsable qualité.")
    private String dateClotureRq;

    @Schema(description = "Identifiant de l'appréciation d'efficacité du traitement.")
    private UUID efficaciteId;

    @Schema(description = "Libellé de l'appréciation d'efficacité.", example = "Efficace")
    private String efficaciteLibelle;

    @Schema(description = "Date du suivi.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime dateSuivi;

    @Schema(description = "Date de publication, c'est-à-dire de sortie du brouillon.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime publicationDate;

    @Schema(description = "Date d'archivage.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm")
    private LocalDateTime archivageDate;

    // ---------------------------------------------------------------- pièces et actions

    @Schema(description = "Pièces jointes composées sur la fiche. Leur contenu ne circule qu'au "
            + "dépôt.")
    private List<PieceJointeDTO> fichiers;

    @Schema(description = "Actions engagées pour traiter le dossier. Elles ne se réécrivent pas "
            + "depuis la fiche : elles ont leur propre service, qui sait ce qu'un engagement "
            + "interdit.")
    private List<PlanActionDto> planActions;

    // ---------------------------------------------------------------- rattachements

    @Schema(description = "Identifiant du référentiel d'origine, quand le constat provient d'un "
            + "autre dispositif.")
    private String originNonConformiteId;

    @Schema(description = "Libellé de ce référentiel d'origine.")
    private String originNonConformiteLibelle;

    @Schema(description = "Identifiant de l'action de référence retenue.")
    private UUID actionId;

    @Schema(description = "Libellé de cette action de référence.")
    private String actionLibelle;

    @Schema(description = "Numéro de la fiche d'action corrective associée.")
    private String numeroFdac;
}
