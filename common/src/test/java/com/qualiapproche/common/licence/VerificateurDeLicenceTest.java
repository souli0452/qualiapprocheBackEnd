package com.qualiapproche.common.licence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le contrat entre le produit livré et l'outil d'émission.
 *
 * <p>La licence vérifiée ici a été <b>réellement émise</b> par l'outil, et l'est avec la clé
 * publique embarquée dans {@code referentiel-service}. C'est ce que les deux projets se
 * promettent : le format et la signature. Une divergence rendrait invérifiables des licences
 * pourtant valides — et ne se découvrirait que chez le client, le jour de l'installation.</p>
 */
class VerificateurDeLicenceTest {

    /** Clé publique de l'éditeur, telle que la configure referentiel-service. */
    private static final String CLE_PUBLIQUE =
            "MCowBQYDK2VwAyEAs2ajPoJ/faSW2FTtOV9tVXc/k3Fk2ZNc3SwQsyEJbxg";

    /** Licence émise par l'outil pour « CHU d'Abidjan », offre Essentiel, 25 utilisateurs. */
    private static final String LICENCE =
            "QSL1.eyJyZWYiOiJMSUMtMjAyNi0wMDAxIiwiY2xpIjoiQ0hVLUFCSiIsIm5vbSI6IkNIVSBkQWJpZGphbiIs"
            + "ImRlYiI6IjIwMjYtMDgtMDgiLCJmaW4iOiIyMDI3LTA4LTA4IiwibW9kIjpbIkRPQ1VNRU5UQUlSRSIsIk5P"
            + "Tl9DT05GT1JNSVRFIl0sInVzciI6MjUsInR5cCI6IkNPTU1FUkNJQUxFIiwiZWR0IjoiRXNzZW50aWVsIn0."
            + "bCHZIXCW84ID2UrYjYkN3ogzcGKWWYAATIqSwhgZ5F5lVHrw5VIgZMOKkl3-hYrxbQaP1z8rL-hngy0dF1U1BA";

    @Test
    @DisplayName("Une licence émise par l'outil est reconnue par le produit")
    void licenceReelle() {
        ContenuDeLicence contenu = VerificateurDeLicence.lire(LICENCE, CLE_PUBLIQUE);

        assertThat(contenu.partenaireCode()).isEqualTo("CHU-ABJ");
        assertThat(contenu.modules()).contains("NON_CONFORMITE", "DOCUMENTAIRE");
        assertThat(contenu.utilisateursMax()).isEqualTo(25);
        assertThat(contenu.estUnEssai()).isFalse();
        assertThat(contenu.couvre(contenu.debut())).isTrue();
        assertThat(contenu.couvre(contenu.fin().plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("Une licence retouchée est rejetée — c'est tout l'objet de la signature")
    void licenceFalsifiee() {
        String[] parties = LICENCE.split("\\.");
        // On prolonge la licence de dix ans, comme le ferait quelqu'un qui a compris le format :
        // la charge utile change, la signature ne suit pas.
        String forgee = parties[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(
                        ("{\"ref\":\"LIC-2036-9999\",\"cli\":\"CHU-ABJ\",\"deb\":\"2026-08-08\","
                                + "\"fin\":\"2036-12-31\"}").getBytes())
                + "." + parties[2];

        assertThatThrownBy(() -> VerificateurDeLicence.lire(forgee, CLE_PUBLIQUE))
                .isInstanceOf(LicenceIllisibleException.class)
                .hasMessageContaining("signature");
    }

    @Test
    @DisplayName("Une clé de vérification absente le dit, plutôt que de tout accepter")
    void sansCleDeVerification() {
        // Le repli permissif est précisément ce qui rendait inopérant le dispositif précédent :
        // une licence illisible y valait « tous les modules ouverts ».
        assertThatThrownBy(() -> VerificateurDeLicence.lire(LICENCE, ""))
                .isInstanceOf(LicenceIllisibleException.class)
                .hasMessageContaining("clé de vérification");
    }

    @Test
    @DisplayName("Les retours à la ligne d'un courriel ne font pas échouer la lecture")
    void espacesTolerees() {
        String colle = "  " + LICENCE.substring(0, 60) + "\n   " + LICENCE.substring(60) + "\n";

        assertThat(VerificateurDeLicence.lire(colle, CLE_PUBLIQUE).partenaireCode())
                .isEqualTo("CHU-ABJ");
    }

    @Test
    @DisplayName("Un texte qui n'est pas une licence le dit clairement")
    void texteQuelconque() {
        assertThatThrownBy(() -> VerificateurDeLicence.lire("bonjour", CLE_PUBLIQUE))
                .isInstanceOf(LicenceIllisibleException.class)
                .hasMessageContaining("licence QualiSira");
    }

    @Test
    @DisplayName("L'expiration est une question distincte de l'authenticité")
    void expirationDistincteDeLAuthenticite() {
        ContenuDeLicence contenu = VerificateurDeLicence.lire(LICENCE, CLE_PUBLIQUE);

        // Elle se relit sans erreur : elle est authentique. C'est à l'écran de dire « votre
        // abonnement a pris fin le … » plutôt que « licence invalide », qui enverrait vérifier
        // un copier-coller pourtant correct.
        assertThat(contenu.couvre(LocalDate.of(2050, 1, 1))).isFalse();
        assertThat(contenu.reference()).isEqualTo("LIC-2026-0001");
    }

    @Test
    @DisplayName("Les modules absents de la licence ne sont pas ouverts")
    void modulesNonSouscrits() {
        ContenuDeLicence contenu = VerificateurDeLicence.lire(LICENCE, CLE_PUBLIQUE);

        assertThat(contenu.ouvre("NON_CONFORMITE")).isTrue();
        assertThat(contenu.ouvre("AUDIT")).isFalse();
        assertThat(contenu.ouvre("RISQUE")).isFalse();
    }
}
