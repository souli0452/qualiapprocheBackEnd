package com.qualiapproche.support.model;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qms_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DocumentQms extends AuditEntity {

    /**
     * Verrou optimiste : protège contre deux transitions de workflow (ou deux dépôts de version)
     * concurrents sur le même document — la seconde écriture échoue proprement au lieu de
     * silencieusement écraser la première ou dupliquer un changement d'état.
     */
    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String documentType; // code du type ex: "PRO", "INS", etc.

    private String reference;    // Référence officielle interne

    private String description;

    @Column(nullable = false)
    private String serviceId;

    private String serviceLibelle;
    private String serviceSigle;

    @Column(nullable = false)
    private String redacteur;

    private boolean esTraiter;
    private boolean enRetardRevision;
    private boolean obsolete;


    /**
     * Rang de révision du document, à partir de zéro.
     *
     * <p>Un document déposé est en v0. Chaque modification aboutie d'une demande le fait passer
     * au rang suivant : v1, v2, et ainsi de suite. Rien d'autre ne le change — ni le dépôt d'un
     * fichier corrigé pendant le circuit initial, ni une reprise après rejet : tant que la
     * révision n'a pas été demandée, instruite et acceptée, le document reste à son rang.</p>
     *
     * <p>Remplace le couple majeure/mineure, qui distinguait une retouche d'une révision alors
     * que seule la seconde existe dans le circuit documentaire.</p>
     */
    @Builder.Default
    @Column(name = "numero_version", nullable = false)
    private int numeroVersion = 0;

    private LocalDateTime dateVigueur;
    private LocalDateTime dateProchRevision;
    private Integer periodiciteMois;
    private boolean confidentiel;
    private boolean documentExterne;
    private String processusDestId;
    private String processusDestLibelle;
    private String referenceOfficielle;
    private LocalDateTime datePublication;
    private String domaine;
    private String statutLegal;

    /*
     * Code du document saisi par son auteur, distinct du numéro attribué par le système :
     * le champ `reference` déclaré plus haut tient ce rôle, aucune colonne nouvelle n'est
     * nécessaire. Il existait déjà en base mais n'était offert par aucun écran — les
     * organisations qui numérotent leurs documents selon leur propre convention n'avaient
     * nulle part où l'inscrire.
     */

    /** Priorité, choisie dans le référentiel paramétrable servi par referentiel-service. */
    private String prioriteId;
    private String prioriteLibelle;

    /**
     * Niveau de confidentialité, choisi dans le référentiel paramétrable. Il porte la liste des
     * rôles admis à consulter le document ; la restriction s'ajoute à celle de la structure, elle
     * ne s'y substitue pas.
     */
    private String niveauConfidentialiteId;
    private String niveauConfidentialiteLibelle;

    /**
     * Domaine d'application, choisi dans le référentiel paramétrable.
     *
     * <p>Le champ {@code domaine} ci-dessus reste la valeur affichée — il porte désormais le
     * libellé du domaine choisi. Il était en saisie libre : « RH », « Ressources Humaines » et
     * « ressources humaines » comptaient pour trois domaines distincts dans les statistiques.</p>
     */
    private String domaineId;

    private String ncReference;

    private String lastModifiedBy;
    private String lastModifiedReason;
    private String alerteEnvoyee;
    private boolean archived;

    // Workflow — libellé de l'étape actuelle (null = brouillon, pas de workflow actif)
    private String currentEtape;
    private java.util.UUID workflowId;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<QmsDocumentVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentUserAccess> userAccessList = new ArrayList<>();

    /**
     * Structures avec lesquelles le document a été partagé — geste explicite de la structure
     * émettrice, sans lequel aucune autre structure ne le voit.
     */
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<DocumentStructureAccess> structureAccessList = new ArrayList<>();

    @JsonProperty("currentObjectName")
    public String getCurrentObjectName() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(versions.size() - 1).getObjectName();
        }
        return null;
    }

    @JsonProperty("currentFileHash")
    public String getCurrentFileHash() {
        if (versions != null && !versions.isEmpty()) {
            return versions.get(versions.size() - 1).getFileHash();
        }
        return null;
    }
}

