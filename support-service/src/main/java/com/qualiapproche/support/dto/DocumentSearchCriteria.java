package com.qualiapproche.support.dto;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSearchCriteria {

    // ---- Recherche texte libre (documentNumber, titre, redacteur, processusDestLibelle) ----
    private String query;

    // ---- Filtres exacts ----
    /** Code du type de document (ex: "PRO", "INS", "FOR", ...) */
    private String documentType;

    /** ID interne du service propriétaire du document */
    private String serviceId;

    /** Libellé du service (recherche partielle, insensible à la casse) */
    private String serviceLibelle;

    /** Sigle du service (ex: "DSI") */
    private String serviceSigle;

    /** Nom ou ID du rédacteur (recherche partielle) */
    private String redacteur;

    /** ID du processus / structure destinateur */
    private String processusDestId;

    /** Libellé du processus / structure destinateur */
    private String processusDestLibelle;

    /** Domaine fonctionnel du document (ex: "Qualité", "RH", ...) */
    private String domaine;

    /** Domaine d'application, par son identifiant au référentiel. */
    private String domaineId;

    /** Priorité, par son identifiant au référentiel. */
    private String prioriteId;

    /**
     * Niveau de confidentialité, par son identifiant au référentiel.
     *
     * <p>Filtrer sur un niveau que l'appelant n'a pas le droit de voir ne le lui ouvre pas : la
     * clause de visibilité écarte ces documents en amont, et le filtre ne fait alors que réduire
     * un ensemble déjà vide. Le front ne propose de son côté que les niveaux permis, pour ne pas
     * offrir un critère qui ne rendrait jamais rien.</p>
     */
    private String niveauConfidentialiteId;

    /** Statut légal du document externe */
    private String statutLegal;

    /** Référence officielle interne */
    private String reference;

    /** Référence vers une non-conformité liée */
    private String ncReference;

    /** Statut(s) du document — peut contenir plusieurs valeurs (OR) */
    private List<String> status;

    /** Étape actuelle du workflow */
    private String currentEtape;

    // ---- Filtres booléens ----
    /** null = pas de filtre, true/false = filtre strict */
    private Boolean confidentiel;
    private Boolean documentExterne;
    private Boolean archived;

    // ---- Filtres de dates (dateVigueur) ----
    private LocalDate dateVigueurFrom;
    private LocalDate dateVigueurTo;

    // ---- Filtres de dates (dateProchRevision) ----
    private LocalDate dateRevisionFrom;
    private LocalDate dateRevisionTo;

    // ---- Filtres de dates (datePublication) ----
    private LocalDate datePublicationFrom;
    private LocalDate datePublicationTo;

    // ---- Filtres de dates (createdAt — hérité de AuditEntity) ----
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;

    /** ID Keycloak du créateur du document (= createdById dans AuditEntity) */
    private String createdById;

    // ---- Pagination / tri ----
    /** Colonne de tri (ex: "titre", "documentNumber", "dateVigueur") — défaut: "documentNumber" */
    private String sortBy;

    /** Sens du tri : "ASC" ou "DESC" — défaut: "ASC" */
    private String sortDirection;
}
