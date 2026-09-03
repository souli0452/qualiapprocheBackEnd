package com.qualiapproche.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Critères de recherche pour les documents QMS.
 * Tous les champs sont optionnels : seuls ceux renseignés sont pris en compte dans le filtre.
 */
@Schema(description = "Critères de recherche des documents. Tous facultatifs, et combinés par ET : "
        + "seuls les champs renseignés restreignent. Aucun d'eux n'élargit la visibilité, qui est "
        + "posée en amont par la structure de l'appelant et son niveau de confidentialité.")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSearchCriteria {

    // ---- Recherche texte libre (documentNumber, titre, redacteur, processusDestLibelle) ----
    @Schema(description = "Recherche en texte libre, portée simultanément sur le numéro, le titre, "
            + "le rédacteur et la structure destinataire. Insensible à la casse et partielle.",
            example = "maîtrise des enregistrements")
    private String query;

    // ---- Filtres exacts ----
    /** Code du type de document (ex: "PRO", "INS", "FOR", ...) */
    @Schema(description = "Code du type documentaire, comparé à l'identique.", example = "PRO")
    private String documentType;

    /** ID interne du service propriétaire du document */
    @Schema(description = "Structure propriétaire, par son identifiant. Comparé à l'identique.")
    private String serviceId;

    /** Libellé du service (recherche partielle, insensible à la casse) */
    @Schema(description = "Structure propriétaire, par son libellé : recherche partielle et "
            + "insensible à la casse, à la différence du filtre par identifiant.",
            example = "systèmes d'information")
    private String serviceLibelle;

    /** Sigle du service (ex: "DSI") */
    @Schema(description = "Structure propriétaire, par son sigle. Comparé en entier, mais sans "
            + "égard à la casse.",
            example = "DSI")
    private String serviceSigle;

    /** Nom ou ID du rédacteur (recherche partielle) */
    @Schema(description = "Rédacteur déclaré, en recherche partielle : le champ étant déclaratif, "
            + "un nom peut aussi bien y figurer qu'un identifiant.")
    private String redacteur;

    /** ID du processus / structure destinateur */
    @Schema(description = "Structure destinataire, par son identifiant. Comparé à l'identique.")
    private String processusDestId;

    /** Libellé du processus / structure destinateur */
    @Schema(description = "Structure destinataire, par son libellé : recherche partielle et "
            + "insensible à la casse.")
    private String processusDestLibelle;

    /** Domaine fonctionnel du document (ex: "Qualité", "RH", ...) */
    @Schema(description = "Domaine par son libellé affiché, comparé à l'identique. Ne vaut que "
            + "pour les documents antérieurs au référentiel, où le domaine était saisi en clair.",
            example = "Qualité")
    private String domaine;

    /** Domaine d'application, par son identifiant au référentiel. */
    @Schema(description = "Domaine d'application, par son identifiant au référentiel. C'est le "
            + "filtre à préférer : il ne souffre pas des variantes d'orthographe.")
    private String domaineId;

    /** Priorité, par son identifiant au référentiel. */
    @Schema(description = "Priorité, par son identifiant au référentiel.")
    private String prioriteId;

    /**
     * Niveau de confidentialité, par son identifiant au référentiel.
     *
     * <p>Filtrer sur un niveau que l'appelant n'a pas le droit de voir ne le lui ouvre pas : la
     * clause de visibilité écarte ces documents en amont, et le filtre ne fait alors que réduire
     * un ensemble déjà vide. Le front ne propose de son côté que les niveaux permis, pour ne pas
     * offrir un critère qui ne rendrait jamais rien.</p>
     */
    @Schema(description = "Niveau de confidentialité, par son identifiant au référentiel. Filtrer "
            + "sur un niveau que l'appelant n'a pas le droit de voir ne le lui ouvre pas : la "
            + "clause de visibilité a déjà écarté ces documents, et le critère ne fait alors que "
            + "réduire un ensemble vide.")
    private String niveauConfidentialiteId;

    /** Statut légal du document externe */
    @Schema(description = "Portée juridique, comparée à l'identique. Ne rend que des documents "
            + "externes, seuls à porter cette valeur.")
    private String statutLegal;

    /** Référence officielle interne */
    @Schema(description = "Référence saisie par l'auteur, en recherche partielle et insensible à "
            + "la casse. Sans rapport avec le numéro attribué par le système.",
            example = "QSE/PR")
    private String reference;

    /** Référence vers une non-conformité liée */
    @Schema(description = "Non-conformité à l'origine du document, par son numéro. Comparé à "
            + "l'identique.")
    private String ncReference;

    /** Statut(s) du document — peut contenir plusieurs valeurs (OR) */
    @Schema(description = "Statuts retenus, combinés par OU. Ce ne sont pas des colonnes mais des "
            + "combinaisons de drapeaux, traduites à la construction du filtre. Laisser vide "
            + "n'ouvre pas tout : la recherche écarte alors les documents obsolètes et archivés.",
            example = "[\"valide\", \"en_approbation\"]",
            allowableValues = {"brouillon", "en_approbation", "valide", "obsolete"})
    private List<String> status;

    /** Étape actuelle du workflow */
    @Schema(description = "Étape où le circuit est arrêté, comparée à l'identique. Ne rend que des "
            + "documents en cours de validation.",
            example = "VERIFICATION")
    private String currentEtape;

    // ---- Filtres booléens ----
    /** null = pas de filtre, true/false = filtre strict */
    @Schema(description = "Documents classés, ou non classés. Absent, le critère ne joue pas — "
            + "d'où l'objet, et non le type primitif.")
    private Boolean confidentiel;

    @Schema(description = "Documents d'origine externe, ou internes. Absent, le critère ne joue "
            + "pas.")
    private Boolean documentExterne;

    @Schema(description = "Documents archivés, ou actifs. Absent, la recherche écarte d'office les "
            + "archives : les rendre par défaut exposerait à travailler sur un document retiré.")
    private Boolean archived;

    // ---- Filtres de dates (dateVigueur) ----
    @Schema(description = "Mise en vigueur au plus tôt, bornes comprises. La journée entière est "
            + "retenue.")
    private LocalDate dateVigueurFrom;

    @Schema(description = "Mise en vigueur au plus tard, bornes comprises.")
    private LocalDate dateVigueurTo;

    // ---- Filtres de dates (dateProchRevision) ----
    @Schema(description = "Prochaine révision attendue au plus tôt. Sert à lister ce qui vient à "
            + "échéance sur une période.")
    private LocalDate dateRevisionFrom;

    @Schema(description = "Prochaine révision attendue au plus tard.")
    private LocalDate dateRevisionTo;

    // ---- Filtres de dates (datePublication) ----
    @Schema(description = "Parution chez l'émetteur au plus tôt. Ne concerne que les documents "
            + "externes.")
    private LocalDate datePublicationFrom;

    @Schema(description = "Parution chez l'émetteur au plus tard.")
    private LocalDate datePublicationTo;

    // ---- Filtres de dates (createdAt — hérité de AuditEntity) ----
    @Schema(description = "Dépôt au plus tôt, sur la date d'enregistrement et non sur la mise en "
            + "vigueur.")
    private LocalDate createdAtFrom;

    @Schema(description = "Dépôt au plus tard.")
    private LocalDate createdAtTo;

    /** ID Keycloak du créateur du document (= createdById dans AuditEntity) */
    @Schema(description = "Identifiant Keycloak de qui a déposé le document. Comparé à l'identique "
            + "— c'est le seul critère qui désigne une personne sans ambiguïté, le rédacteur étant "
            + "déclaratif.")
    private String createdById;

    // ---- Pagination / tri ----
    /**
     * Colonne de tri (ex : {@code "titre"}, {@code "documentNumber"}, {@code "dateVigueur"}).
     *
     * <p>À défaut, le tri se fait sur {@code createdAt}. La ligne précédente annonçait
     * {@code documentNumber} : c'était l'ancien classement, abandonné parce qu'un document
     * fraîchement déposé — donc portant le numéro le plus élevé — se retrouvait en fin de liste,
     * et que son auteur le croyait perdu.</p>
     */
    @Schema(description = "Champ sur lequel trier, nommé comme la propriété de l'entité. Laissé "
            + "vide, la liste rend les dépôts les plus récents d'abord, pour que l'auteur retrouve "
            + "aussitôt ce qu'il vient d'enregistrer.",
            example = "dateVigueur")
    private String sortBy;

    /**
     * Sens du tri : {@code "ASC"} ou {@code "DESC"}.
     *
     * <p>À défaut, il suit le champ : croissant lorsqu'un tri est demandé, décroissant sur le
     * tri par défaut, qui doit rendre les dépôts les plus récents d'abord.</p>
     */
    @Schema(description = "Sens du tri. Absent, il suit le champ demandé : croissant si un tri est "
            + "précisé, décroissant sur le tri par défaut.",
            example = "DESC",
            allowableValues = {"ASC", "DESC"})
    private String sortDirection;
}
