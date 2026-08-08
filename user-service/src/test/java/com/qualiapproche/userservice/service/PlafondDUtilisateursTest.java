package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.client.StructureClient;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le plafond d'utilisateurs de la licence.
 *
 * <p>Le nombre était inscrit dans la licence sans que rien ne l'applique : une licence vendue
 * pour deux personnes en acceptait dix. Ces cas fixent ce qui compte pour une place, et ce que
 * l'utilisateur lit quand il n'en reste plus.</p>
 */
class PlafondDUtilisateursTest {

    private UsersResource comptes;
    private StructureClient referentiel;
    private PlafondDUtilisateurs plafond;

    @BeforeEach
    void setUp() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource royaume = mock(RealmResource.class);
        comptes = mock(UsersResource.class);
        referentiel = mock(StructureClient.class);

        when(keycloak.realm(anyString())).thenReturn(royaume);
        when(royaume.users()).thenReturn(comptes);

        KcAuthProperties proprietes = mock(KcAuthProperties.class);
        when(proprietes.getRealm()).thenReturn("quali");

        plafond = new PlafondDUtilisateurs(keycloak, proprietes, referentiel);
    }

    private void licenceAutorise(int utilisateursMax) {
        when(referentiel.etatLicence()).thenReturn(EtatLicenceDto.builder()
                .statut("ACTIVE").actionsOuvertes(true).utilisateursMax(utilisateursMax)
                .modules(List.of("NON_CONFORMITE")).build());
    }

    private UserRepresentation compte(String nom, boolean actif) {
        UserRepresentation compte = new UserRepresentation();
        compte.setUsername(nom);
        compte.setEnabled(actif);
        return compte;
    }

    private void enBase(UserRepresentation... existants) {
        when(comptes.list(anyInt(), anyInt())).thenReturn(Arrays.asList(existants));
    }

    @Test
    @DisplayName("Sous le plafond, la création passe")
    void sousLePlafond_creationPossible() {
        licenceAutorise(2);
        enBase(compte("alice", true));

        assertThatCode(() -> plafond.verifierAvantCreation()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Plafond atteint : refus chiffré, avec les deux issues réelles")
    void plafondAtteint_refusExplicite() {
        licenceAutorise(2);
        enBase(compte("alice", true), compte("bob", true));

        assertThatThrownBy(() -> plafond.verifierAvantCreation())
                .isInstanceOf(BusinessException.class)
                .satisfies(erreur -> assertThat(((BusinessException) erreur).getStatus())
                        // 409 : le seul statut que l'écran de gestion des comptes affiche avec le
                        // message du serveur. Sous un code qu'il ne connaît pas, l'utilisateur
                        // lirait « Erreur de connection, contactez l'administrateur ».
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("2 utilisateurs")
                .hasMessageContaining("désactivez un compte existant")
                .hasMessageContaining("étendre");
    }

    @Test
    @DisplayName("Un compte désactivé libère sa place")
    void compteDesactive_liberelaPlace() {
        // C'est ce qui permet de remplacer quelqu'un qui part sans effacer ce qu'il a produit.
        licenceAutorise(2);
        enBase(compte("alice", true), compte("bob", false));

        assertThatCode(() -> plafond.verifierAvantCreation()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Les comptes de service ne prennent pas de place")
    void comptesDeService_nesontPasDesUtilisateurs() {
        // Les compter reviendrait à confisquer au client une place sur deux pour nos propres
        // appels entre services.
        licenceAutorise(2);
        enBase(compte("alice", true), compte("service-account-quali-sira", true));

        assertThatCode(() -> plafond.verifierAvantCreation()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Réactiver reprend une place, et se refuse au plafond")
    void reactivation_soumiseAuPlafond() {
        licenceAutorise(1);
        enBase(compte("alice", true), compte("bob", false));

        assertThatThrownBy(() -> plafond.verifierAvantReactivation())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Licence sans limite : rien n'est entravé")
    void sansLimite_rienNestEntrave() {
        licenceAutorise(0);
        enBase(compte("alice", true), compte("bob", true), compte("claire", true));

        assertThatCode(() -> plafond.verifierAvantCreation()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Référentiel injoignable : la création reste ouverte")
    void referentielInjoignable_creationOuverte() {
        // Une panne de quelques secondes chez le voisin ne doit pas bloquer les arrivées, alors
        // que rien n'indique un dépassement.
        when(referentiel.etatLicence()).thenThrow(new IllegalStateException("injoignable"));

        assertThatCode(() -> plafond.verifierAvantCreation()).doesNotThrowAnyException();
    }
}
