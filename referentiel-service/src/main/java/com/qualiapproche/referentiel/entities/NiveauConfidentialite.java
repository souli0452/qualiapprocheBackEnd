package com.qualiapproche.referentiel.entities;

import com.qualiapproche.common.base.AuditEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Niveau de confidentialité d'un document et rôles admis à le consulter.
 *
 * <p>Les rôles sont désignés par leur <b>nom</b> — « RESPONSABLE_QUALITE », « PILOTE » — et non par
 * leur identifiant : c'est le nom que portent déjà les étapes des circuits, et il est immuable une
 * fois le rôle publié (voir {@code AppRoleService}), ce qui en fait une référence stable.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@Table(name = "niveaux_confidentialite",
        uniqueConstraints = @UniqueConstraint(name = "uk_niveau_confidentialite_libelle", columnNames = "libelle"))
public class NiveauConfidentialite extends AuditEntity {

    @Column(nullable = false)
    private String libelle;

    private String description;

    /** Rang, du moins sensible au plus sensible. */
    private Integer ordre;

    /** Liste vide : le niveau ne restreint rien de plus que la règle de structure. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "niveau_confidentialite_roles",
            joinColumns = @JoinColumn(name = "niveau_id"))
    @Column(name = "role_nom")
    @Builder.Default
    private List<String> rolesAutorises = new ArrayList<>();
}
