package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * La nature d'un constat : point fort, piste d'amélioration, non-conformité mineure...
 *
 * <p>Paramétrable plutôt qu'énuméré : chaque organisation a sa grille. {@code genereNonConformite}
 * marque les natures qui, une fois le constat publié, partent vers le module des
 * non-conformités.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "type_constat")
public class TypeConstat extends AuditEntity {

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "couleur", length = 32)
    private String couleur;

    @Column(name = "genere_non_conformite", nullable = false)
    private boolean genereNonConformite;

    /**
     * La gravité que porte, dans le module Non-conformités, un constat de cette nature —
     * « Mineure », « Majeure ». Facultative : sans elle, le dossier part sans gravité, et le
     * responsable qualité la fixe à la validation.
     */
    @Column(name = "niveau_non_conformite_id")
    private UUID niveauNonConformiteId;
}
