package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Une personne habilitée à auditer.
 *
 * <p>Son identité — nom, courriel, rôle — et son authentification vivent dans Keycloak, hors de la
 * base applicative : seul l'identifiant Keycloak est gardé ici, dans {@code utilisateurId}. Le
 * diagramme en fait l'identifiant de la classe ; il en reste distinct ici pour que la fiche garde
 * la clé UUID et les traces de création communes à toutes les entités du dépôt.</p>
 *
 * <p>{@code niveauHabilitation} et {@code processusGeres} ne sont pertinents que si la personne est
 * Responsable Qualité ou Pilote de processus dans Keycloak.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auditeur")
public class Auditeur extends AuditEntity {

    /** L'identifiant Keycloak ({@code sub}) de la personne. */
    @Column(name = "utilisateur_id", nullable = false)
    private String utilisateurId;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "auditeur_domaine_habilite", joinColumns = @JoinColumn(name = "auditeur_id"))
    @Column(name = "valeur", nullable = false)
    private List<String> domainesHabilites = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "auditeur_certification", joinColumns = @JoinColumn(name = "auditeur_id"))
    @Column(name = "valeur", nullable = false)
    private List<String> certifications = new ArrayList<>();

    @Column(name = "disponibilite")
    private String disponibilite;

    /** La structure de rattachement : c'est elle qui fonde le conflit d'intérêts. */
    @Column(name = "structure_appartenance")
    private UUID structureAppartenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAuditeur statut;

    @Column(name = "niveau_habilitation")
    private String niveauHabilitation;

    /** Les processus (structures) dont la personne est pilote. */
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "auditeur_processus_gere", joinColumns = @JoinColumn(name = "auditeur_id"))
    @Column(name = "valeur", nullable = false)
    private List<String> processusGeres = new ArrayList<>();

    /**
     * Auditer ce processus mettrait-il la personne en conflit d'intérêts ?
     *
     * <p>Oui si elle appartient à la structure auditée, ou si elle en est pilote : on ne s'audite
     * pas soi-même. C'est le principe d'indépendance de l'ISO 19011, et la seule règle que le
     * modèle permette de vérifier sans jugement humain.</p>
     */
    public boolean aConflitInteret(Audit audit) {
        UUID processus = audit.getProcessusId();
        if (processus == null) {
            return false;
        }
        return processus.equals(structureAppartenance)
                || processusGeres.contains(processus.toString());
    }
}
