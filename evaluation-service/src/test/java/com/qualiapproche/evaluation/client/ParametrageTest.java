package com.qualiapproche.evaluation.client;

import com.qualiapproche.common.dto.NiveauEvaluationAuditeurDto;
import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Ce que le module fait du paramétrage lu dans referentiel-service, et quand celui-ci se tait. */
@ExtendWith(MockitoExtension.class)
class ParametrageTest {

    @Mock private ParametrageClient client;

    @InjectMocks private Parametrage parametrage;

    private static SiteDto site(UUID id) {
        return SiteDto.builder().id(id).nom("Siège").actif(true).build();
    }

    @Test
    @DisplayName("Un site actif de la direction se rattache")
    void un_site_actif_se_rattache() {
        UUID id = UUID.randomUUID();
        when(client.sites(false)).thenReturn(List.of(site(id)));

        parametrage.exigerDesSitesActifs(List.of(id));
    }

    @Test
    @DisplayName("Un site absent des sites actifs — retiré, ou d'une autre direction — ne se rattache pas")
    void un_site_absent_ne_se_rattache_pas() {
        when(client.sites(false)).thenReturn(List.of(site(UUID.randomUUID())));

        assertThatThrownBy(() -> parametrage.exigerDesSitesActifs(List.of(UUID.randomUUID())))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("Sans site à rattacher, le référentiel n'est pas appelé")
    void sans_site_le_referentiel_n_est_pas_appele() {
        parametrage.exigerDesSitesActifs(List.of());
    }

    @Test
    @DisplayName("Un référentiel muet refuse le choix en 503, en disant pourquoi")
    void un_referentiel_muet_refuse_le_choix() {
        when(client.niveaux(false)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> parametrage.niveauActif(UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("momentanément illisible");
    }

    @Test
    @DisplayName("Un référentiel muet n'empêche pas d'afficher : les libellés manquent, rien de plus")
    void un_referentiel_muet_n_empeche_pas_d_afficher() {
        when(client.niveaux(true)).thenThrow(new RuntimeException("Connection refused"));

        assertThat(parametrage.niveauxPourAffichage()).isEmpty();
    }

    @Test
    @DisplayName("L'affichage lit aussi les entrées retirées")
    void l_affichage_lit_aussi_les_retirees() {
        UUID id = UUID.randomUUID();
        when(client.niveaux(true)).thenReturn(List.of(
                NiveauEvaluationAuditeurDto.builder().id(id).libelle("Insuffisant").ordre(1).actif(false).build()));

        assertThat(parametrage.niveauxPourAffichage().get(id).getLibelle()).isEqualTo("Insuffisant");
    }
}
