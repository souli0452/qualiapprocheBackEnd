package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Un nœud du référentiel RQAPBF : domaine, sous-domaine, ou critère à noter.
 *
 * <p>Un arbre : seules les feuilles se notent, les nœuds au-dessus agrègent la note de leurs
 * descendants.</p>
 */
@Getter
@Setter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "noeud_referentiel")
public class NoeudReferentiel extends AuditEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "libelle", nullable = false, columnDefinition = "TEXT")
    private String libelle;

    /** DOMAINE, SOUS_DOMAINE, CRITERE... libre, comme les niveaux du référentiel lui-même. */
    @Column(name = "type_noeud", length = 50)
    private String typeNoeud;

    /** Les éléments de preuve que le référentiel attend pour ce critère. */
    @Column(name = "preuve_attendue", columnDefinition = "TEXT")
    private String preuveAttendue;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NoeudReferentiel parent;

    @Builder.Default
    @OrderBy("ordre ASC")
    @OneToMany(mappedBy = "parent")
    private List<NoeudReferentiel> enfants = new ArrayList<>();

    public boolean estFeuille() {
        return enfants.isEmpty();
    }

    public void ajouterEnfant(NoeudReferentiel noeud) {
        noeud.setParent(this);
        enfants.add(noeud);
    }

    /** Le domaine auquel le nœud appartient : son ancêtre sans parent. */
    public NoeudReferentiel racine() {
        NoeudReferentiel courant = this;
        while (courant.getParent() != null) {
            courant = courant.getParent();
        }
        return courant;
    }
}
