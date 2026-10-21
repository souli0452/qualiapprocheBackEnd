package com.qualiapproche.evaluation.entities;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Brouillon, compilé, publié — et les retours qu'on s'autorise entre eux. */
class PaliersDuConstatTest {

    private static Constat brouillon(TypeConstat nature) {
        return Constat.builder().observation("Procédure non signée").statut(StatutConstat.BROUILLON).nature(nature).build();
    }

    private static TypeConstat nc() {
        return TypeConstat.builder().libelle("NC mineure").genereNonConformite(true).build();
    }

    @Test
    @DisplayName("Un constat va du brouillon à la compilation puis à la publication")
    void un_constat_va_du_brouillon_a_la_compilation_puis_a_la_publication() {
        Constat constat = brouillon(nc());
        constat.publier();
        assertThat(constat.getStatut()).isEqualTo(StatutConstat.COMPILE);
        constat.valider();
        assertThat(constat.getStatut()).isEqualTo(StatutConstat.PUBLIE);
        assertThat(constat.getDatePublication()).isNotNull();
    }

    @Test
    @DisplayName("L'équipe peut renvoyer un constat compilé mais pas un publié")
    void l_equipe_peut_renvoyer_un_constat_compile_mais_pas_un_publie() {
        Constat constat = brouillon(nc());
        constat.publier();
        constat.retirerVersBrouillon("Preuve manquante", "Aïcha OUEDRAOGO");
        assertThat(constat.getStatut()).isEqualTo(StatutConstat.BROUILLON);
        assertThat(constat.estRenvoye()).isTrue();

        constat.publier();
        assertThat(constat.estRenvoye()).isFalse();
        constat.valider();
        assertThatThrownBy(() -> constat.retirerVersBrouillon(null, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Un brouillon sans nature ne se verse pas à l'équipe")
    void un_brouillon_sans_nature_ne_se_verse_pas_a_l_equipe() {
        assertThatThrownBy(brouillon(null)::publier).isInstanceOf(BusinessException.class)
                .hasMessageContaining("nature");
    }

    @Test
    @DisplayName("Un brouillon ne se valide pas sans passer par la compilation")
    void un_brouillon_ne_se_valide_pas_sans_passer_par_la_compilation() {
        assertThatThrownBy(brouillon(nc())::valider).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("C'est la nature qui fait la non-conformité")
    void c_est_la_nature_qui_fait_la_non_conformite() {
        assertThat(brouillon(nc()).estNonConformite()).isTrue();
        assertThat(brouillon(TypeConstat.builder().libelle("Point fort").build()).estNonConformite()).isFalse();
        assertThat(brouillon(null).estNonConformite()).isFalse();
    }
}
