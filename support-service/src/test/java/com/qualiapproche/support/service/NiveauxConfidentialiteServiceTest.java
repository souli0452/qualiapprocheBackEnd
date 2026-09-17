package com.qualiapproche.support.service;

import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.support.client.ReferentielClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le niveau de confidentialité restreint la vue au-delà de la structure : relever du bon service
 * n'ouvre pas un document classé à qui n'en détient pas le rôle.
 *
 * <p>Ce que ces tests protègent surtout, c'est le comportement en panne. Un référentiel injoignable
 * ne doit pas lever la restriction — un document confidentiel qui s'ouvre à la faveur d'une
 * indisponibilité est exactement ce que ce niveau existe pour empêcher.</p>
 */
class NiveauxConfidentialiteServiceTest {

    private static final UUID NIVEAU_RESTREINT = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID NIVEAU_OUVERT = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private ReferentielClient client;
    private NiveauxConfidentialiteService service;

    @BeforeEach
    void setUp() {
        client = mock(ReferentielClient.class);
        service = new NiveauxConfidentialiteService(client);
        ReflectionTestUtils.setField(service, "retentionSecondes", 300L);
    }

    private NiveauConfidentialiteDto niveau(UUID id, String libelle, List<String> roles) {
        NiveauConfidentialiteDto dto = new NiveauConfidentialiteDto();
        dto.setId(id);
        dto.setLibelle(libelle);
        dto.setRolesAutorises(roles);
        return dto;
    }

    /** Le niveau « M », tel qu'un administrateur le configure : deux rôles admis. */
    private static final UUID NIVEAU_M = UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Test
    @DisplayName("Niveau M (AGENT, PILOTE) : les deux rôles admis voient le document")
    void niveauM_lesDeuxRolesAdmisVoient() {
        when(client.niveauxConfidentialite())
                .thenReturn(List.of(niveau(NIVEAU_M, "M", List.of("AGENT", "PILOTE"))));

        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("PILOTE"))).isTrue();
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("AGENT"))).isTrue();
        // Un rôle de plus ne retire rien : il suffit d'en détenir un des deux.
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("AGENT", "RESPONSABLE_QUALITE"))).isTrue();
    }

    @Test
    @DisplayName("Niveau M : un rôle qui n'y figure pas ne voit rien, même dans la bonne structure")
    void niveauM_roleAbsentNeVoitRien() {
        when(client.niveauxConfidentialite())
                .thenReturn(List.of(niveau(NIVEAU_M, "M", List.of("AGENT", "PILOTE"))));

        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("RESPONSABLE_QUALITE"))).isFalse();
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of())).isFalse();
        assertThat(service.niveauxInterdits(Set.of("RESPONSABLE_QUALITE")))
                .containsExactly(NIVEAU_M.toString());
    }

    @Test
    @DisplayName("Le rôle se compare sans égard à la casse ni aux espaces de saisie")
    void roleCompareSansEgardALaCasse() {
        // L'écran d'administration laisse saisir « Pilote » ou « pilote  » ; le profil rend
        // « PILOTE ». Sans normalisation des deux côtés, le porteur du rôle ne verrait rien.
        when(client.niveauxConfidentialite())
                .thenReturn(List.of(niveau(NIVEAU_M, "M", List.of(" Agent ", "pilote"))));

        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("PILOTE"))).isTrue();
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("AGENT"))).isTrue();
    }

    @Test
    @DisplayName("Référentiel injoignable : le niveau M se ferme à tous, rôle ou pas")
    void referentielInjoignable_niveauMSeFermeATous() {
        // Le symptôme à reconnaître : ce n'est pas le classement qui refuse, c'est l'impossibilité
        // de l'établir. Aucun rôle n'y change rien, et rien ne le signale à l'écran.
        when(client.niveauxConfidentialite()).thenThrow(new RuntimeException("referentiel injoignable"));

        assertThat(service.restrictionIndecidable()).isTrue();
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("PILOTE"))).isFalse();
        assertThat(service.peutVoir(NIVEAU_M.toString(), Set.of("AGENT"))).isFalse();
        // Seuls les documents sans niveau subsistent.
        assertThat(service.peutVoir(null, Set.of())).isTrue();
    }

    @Test
    @DisplayName("Un document sans niveau reste visible de tous")
    void sansNiveau_visible() {
        when(client.niveauxConfidentialite()).thenReturn(List.of());

        assertThat(service.peutVoir(null, Set.of())).isTrue();
        assertThat(service.peutVoir("", Set.of())).isTrue();
    }

    @Test
    @DisplayName("Le rôle exigé ouvre le document ; son absence le ferme")
    void niveauRestreint_selonLeRole() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_RESTREINT, "Confidentiel", List.of("RESPONSABLE_QUALITE", "PILOTE"))));

        assertThat(service.peutVoir(NIVEAU_RESTREINT.toString(), Set.of("PILOTE"))).isTrue();
        assertThat(service.peutVoir(NIVEAU_RESTREINT.toString(), Set.of("AGENT"))).isFalse();
    }

    @Test
    @DisplayName("Un niveau qui n'exige aucun rôle ne restreint rien")
    void niveauSansRole_neRestreintRien() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_OUVERT, "Interne", List.of())));

        assertThat(service.peutVoir(NIVEAU_OUVERT.toString(), Set.of())).isTrue();
        assertThat(service.niveauxInterdits(Set.of())).isEmpty();
    }

    @Test
    @DisplayName("Référentiel injoignable : tout document classé est écarté, jamais ouvert")
    void referentielIndisponible_restrictionMaintenue() {
        when(client.niveauxConfidentialite()).thenThrow(new IllegalStateException("injoignable"));

        assertThat(service.restrictionIndecidable()).isTrue();
        assertThat(service.peutVoir(NIVEAU_RESTREINT.toString(), Set.of("RESPONSABLE_QUALITE"))).isFalse();
        // Les documents sans niveau, eux, restent accessibles : la panne ne bloque pas l'ordinaire.
        assertThat(service.peutVoir(null, Set.of())).isTrue();
    }

    @Test
    @DisplayName("Les niveaux interdits se déduisent des rôles détenus")
    void niveauxInterdits() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_RESTREINT, "Confidentiel", List.of("RESPONSABLE_QUALITE")),
                niveau(NIVEAU_OUVERT, "Interne", List.of())));

        assertThat(service.niveauxInterdits(Set.of("AGENT")))
                .containsExactly(NIVEAU_RESTREINT.toString());
        assertThat(service.niveauxInterdits(Set.of("RESPONSABLE_QUALITE"))).isEmpty();
    }

    @Test
    @DisplayName("Le filtre ne propose que les niveaux permis à l'appelant")
    void niveauxFiltrables_selonLeRole() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_RESTREINT, "Confidentiel", List.of("DIRECTION")),
                niveau(NIVEAU_OUVERT, "Public", List.of())));

        assertThat(service.visiblesPour(Set.of("AGENT"), false))
                .extracting(NiveauConfidentialiteDto::getLibelle)
                .containsExactly("Public");
        assertThat(service.visiblesPour(Set.of("DIRECTION"), false))
                .extracting(NiveauConfidentialiteDto::getLibelle)
                .containsExactlyInAnyOrder("Confidentiel", "Public");
    }

    @Test
    @DisplayName("Voir toutes les structures ne dispense pas du classement")
    void niveauxFiltrables_aucuneDispense() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_RESTREINT, "Confidentiel", List.of("DIRECTION")),
                niveau(NIVEAU_OUVERT, "Public", List.of())));

        // Le responsable qualité s'affranchit de la barrière de structure, non du classement :
        // un niveau dont il n'a pas le rôle ne lui rendrait rien, autant ne pas le lui proposer.
        assertThat(service.visiblesPour(Set.of("RESPONSABLE_QUALITE"), false))
                .extracting(NiveauConfidentialiteDto::getLibelle)
                .containsExactly("Public");

        // L'administration générale, elle, reste dispensée : c'est par elle qu'un document
        // classé à tort se répare.
        assertThat(service.visiblesPour(Set.of("SUPER_ADMIN"), true)).hasSize(2);
    }

    @Test
    @DisplayName("Référentiel injoignable : aucun niveau n'est proposé au filtre")
    void niveauxFiltrables_referentielInjoignable() {
        when(client.niveauxConfidentialite()).thenThrow(new IllegalStateException("injoignable"));

        // Cohérent avec la recherche, qui écarte alors tout document classé : proposer un critère
        // qui ne rendrait rien ferait passer une panne pour une absence de documents.
        assertThat(service.visiblesPour(Set.of("DIRECTION"), false)).isEmpty();
    }

    @Test
    @DisplayName("Le référentiel n'est lu qu'une fois par période, pas à chaque document")
    void referentiel_misEnCache() {
        when(client.niveauxConfidentialite()).thenReturn(List.of(
                niveau(NIVEAU_RESTREINT, "Confidentiel", List.of("PILOTE"))));

        service.peutVoir(NIVEAU_RESTREINT.toString(), Set.of("PILOTE"));
        service.peutVoir(NIVEAU_RESTREINT.toString(), Set.of("PILOTE"));
        service.niveauxInterdits(Set.of("PILOTE"));

        verify(client, times(1)).niveauxConfidentialite();
    }
}
