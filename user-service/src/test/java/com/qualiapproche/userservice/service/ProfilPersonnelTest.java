package com.qualiapproche.userservice.service;

import com.qualiapproche.common.dto.auth.ProfilPersonnelDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.config.utils.KcAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le libre-service du profil.
 *
 * <p>Ce qui est vérifié ici tient moins à ce que la méthode écrit qu'à ce qu'elle <b>n'écrit
 * pas</b>. La voie d'administration voisine pose {@code setEnabled} et resynchronise les rôles à
 * partir du corps de la requête ; appelée depuis un formulaire de profil, elle désactiverait le
 * compte de l'intéressé et effacerait ses rôles. C'est précisément ce que ces tests interdisent.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfilPersonnelTest {

    private static final String ROYAUME = "qualiapproche";
    private static final String UTILISATEUR = "3f1c0e5a-77aa-4b3d-9c21-0f5e2b8a1d44";

    @Mock private Keycloak keycloak;
    @Mock private RealmResource royaume;
    @Mock private UsersResource utilisateurs;
    @Mock private UserResource ressourceUtilisateur;
    @Mock private KcAuthProperties proprietes;

    private ProfilPersonnel profilPersonnel;
    private UserRepresentation enBase;

    @BeforeEach
    void setUp() {
        enBase = new UserRepresentation();
        enBase.setId(UTILISATEUR);
        enBase.setUsername("atraore");
        enBase.setFirstName("Awa");
        enBase.setLastName("Traore");
        enBase.setEmail("awa.traore@exemple.test");
        enBase.setEnabled(true);
        Map<String, List<String>> attributs = new HashMap<>();
        attributs.put("structure", List.of("DIR-QUALITE"));
        attributs.put("fonction", List.of("Auditrice qualité"));
        attributs.put("phoneNumber", List.of("+226 70 00 00 00"));
        enBase.setAttributes(attributs);

        when(proprietes.getRealm()).thenReturn(ROYAUME);
        when(keycloak.realm(ROYAUME)).thenReturn(royaume);
        when(royaume.users()).thenReturn(utilisateurs);
        when(utilisateurs.get(UTILISATEUR)).thenReturn(ressourceUtilisateur);
        when(ressourceUtilisateur.toRepresentation()).thenReturn(enBase);

        profilPersonnel = new ProfilPersonnel(keycloak, proprietes);
    }

    @Test
    @DisplayName("Nom, prénom et téléphone sont écrits")
    void lesTroisChampsSontEcrits() {
        profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa Bintou", "Traoré", "+226 76 12 34 56"));

        UserRepresentation ecrit = capture();
        assertThat(ecrit.getFirstName()).isEqualTo("Awa Bintou");
        assertThat(ecrit.getLastName()).isEqualTo("Traoré");
        assertThat(ecrit.getAttributes().get("phoneNumber")).containsExactly("+226 76 12 34 56");
    }

    @Test
    @DisplayName("Ni l'activation, ni l'adresse, ni la structure, ni la fonction ne sont touchées")
    void leResteDuCompteEstIntact() {
        profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa", "Traoré", "+226 76 12 34 56"));

        UserRepresentation ecrit = capture();
        assertThat(ecrit.isEnabled())
                .as("un compte ne doit pas se désactiver parce que son titulaire corrige son nom")
                .isTrue();
        assertThat(ecrit.getEmail()).isEqualTo("awa.traore@exemple.test");
        assertThat(ecrit.getAttributes())
                .as("la structure commande les habilitations : la perdre reviendrait à les perdre")
                .containsEntry("structure", List.of("DIR-QUALITE"))
                .containsEntry("fonction", List.of("Auditrice qualité"));
    }

    @Test
    @DisplayName("Un téléphone vidé retire l'attribut au lieu d'y laisser une chaîne vide")
    void telephoneVideRetireLAttribut() {
        profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa", "Traoré", "   "));

        assertThat(capture().getAttributes()).doesNotContainKey("phoneNumber");
    }

    @Test
    @DisplayName("Les espaces autour des valeurs sont retirés")
    void lesValeursSontElaguees() {
        profilPersonnel.mettreAJour(UTILISATEUR, profil("  Awa  ", "  Traoré  ", "  +226 76 12 34 56  "));

        UserRepresentation ecrit = capture();
        assertThat(ecrit.getFirstName()).isEqualTo("Awa");
        assertThat(ecrit.getLastName()).isEqualTo("Traoré");
        assertThat(ecrit.getAttributes().get("phoneNumber")).containsExactly("+226 76 12 34 56");
    }

    @Test
    @DisplayName("Un nom ou un prénom manquant est refusé, et rien n'est écrit")
    void nomEtPrenomSontExiges() {
        assertThatThrownBy(() -> profilPersonnel.mettreAJour(UTILISATEUR, profil("  ", "Traoré", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prénom")
                .extracting(e -> ((BusinessException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nom");

        verify(ressourceUtilisateur, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Un téléphone malformé est refusé, et rien n'est écrit")
    void telephoneMalformeEstRefuse() {
        assertThatThrownBy(() ->
                profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa", "Traoré", "appelez le standard")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("téléphone");

        verify(ressourceUtilisateur, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Un compte sans aucun attribut ne fait pas échouer l'écriture")
    void compteSansAttributs() {
        enBase.setAttributes(null);

        profilPersonnel.mettreAJour(UTILISATEUR, profil("Awa", "Traoré", "70000000"));

        assertThat(capture().getAttributes()).containsEntry("phoneNumber", List.of("70000000"));
    }

    private ProfilPersonnelDto profil(String prenom, String nom, String telephone) {
        return ProfilPersonnelDto.builder()
                .firstName(prenom).lastName(nom).phoneNumber(telephone).build();
    }

    private UserRepresentation capture() {
        ArgumentCaptor<UserRepresentation> capteur = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(ressourceUtilisateur).update(capteur.capture());
        return capteur.getValue();
    }
}
