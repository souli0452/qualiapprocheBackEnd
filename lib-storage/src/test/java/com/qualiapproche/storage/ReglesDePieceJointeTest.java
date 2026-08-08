package com.qualiapproche.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bornes des pièces jointes, appliquées au point de dépôt commun.
 *
 * <p>Sans elles, seul le plafond multipart du serveur — un gigaoctet — s'appliquait, et rien
 * n'excluait un exécutable déposé comme justificatif d'étape. La liste des formats est blanche et
 * non noire : une liste noire court après les extensions dangereuses et en oublie toujours une.</p>
 */
class ReglesDePieceJointeTest {

    private static final long UN_MO = 1024 * 1024;

    @Test
    @DisplayName("Un document de bureau de taille normale passe")
    void documentOrdinaire_admis() {
        assertThatCode(() -> ReglesDePieceJointe.verifier("attestation-formation-2026.pdf", 2 * UN_MO))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReglesDePieceJointe.verifier("photo-constat.JPG", 8 * UN_MO))
                .doesNotThrowAnyException();
        assertThatCode(() -> ReglesDePieceJointe.verifier("preuves.zip", 20 * UN_MO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Au-delà de 25 Mo, refus avec la limite dans le message")
    void tropLourd_refuse() {
        assertThatThrownBy(() -> ReglesDePieceJointe.verifier("rapport.pdf", 26 * UN_MO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("25");
    }

    @Test
    @DisplayName("Un exécutable est refusé, quel que soit son poids")
    void executable_refuse() {
        for (String nom : new String[]{"outil.exe", "script.sh", "macro.bat", "page.html", "app.jar"}) {
            assertThatThrownBy(() -> ReglesDePieceJointe.verifier(nom, UN_MO))
                    .as(nom)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("n'est pas admis");
        }
    }

    @Test
    @DisplayName("Sans extension, refus : impossible de dire ce que c'est")
    void sansExtension_refuse() {
        assertThatThrownBy(() -> ReglesDePieceJointe.verifier("fichier", UN_MO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReglesDePieceJointe.verifier(null, UN_MO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("La casse de l'extension ne change rien")
    void casseIndifferente() {
        assertThatCode(() -> ReglesDePieceJointe.verifier("RAPPORT.PDF", UN_MO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Une double extension est jugée sur la dernière : « facture.pdf.exe » est refusé")
    void doubleExtension_jugeeSurLaDerniere() {
        assertThatThrownBy(() -> ReglesDePieceJointe.verifier("facture.pdf.exe", UN_MO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
