package com.qualiapproche.support.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.qualiapproche.support.dto.DocumentStatDimension;
import com.qualiapproche.support.model.DocumentQms;

/**
 * Le statut affiché d'un document, et les deux comptes du tableau de bord qui s'en déduisent.
 *
 * <p>« En vigueur » et « en circuit » se lisent sur la même règle que la répartition par statut :
 * la vue d'ensemble les affiche côte à côte, et deux calculs parallèles auraient fini par se
 * contredire sur le même écran.</p>
 *
 * <p>« En circuit » se lit à l'envers — est en circuit tout document dont le statut affiché n'est
 * aucun des quatre que la dimension nomme, c'est-à-dire le code de son étape. Énumérer les étapes
 * aurait supposé de les connaître : elles sont paramétrables, et un circuit peut en gagner une
 * sans que rien ici ne l'apprenne.</p>
 */
class StatutAfficheDuDocumentTest {

    private static DocumentQms document() {
        return DocumentQms.builder().build();
    }

    @Test
    @DisplayName("Un document validé est en vigueur, et n'est pas en circuit")
    void valide_enVigueur() {
        DocumentQms document = document();
        document.setEsTraiter(true);

        assertThat(DocumentStatDimension.estEnVigueur(document)).isTrue();
        assertThat(DocumentStatDimension.estEnCircuit(document)).isFalse();
    }

    @Test
    @DisplayName("Un document arrêté à une étape est en circuit, quel que soit le nom de l'étape")
    void etapeQuelconque_enCircuit() {
        DocumentQms document = document();
        // Une étape que personne n'a prévue ici : le circuit est paramétrable.
        document.setCurrentEtape("RELECTURE_JURIDIQUE");

        assertThat(DocumentStatDimension.estEnCircuit(document)).isTrue();
        assertThat(DocumentStatDimension.estEnVigueur(document)).isFalse();
    }

    @Test
    @DisplayName("Un document jamais remis au circuit n'est ni en vigueur ni en circuit")
    void brouillon_dansAucunDesDeux() {
        DocumentQms document = document();

        assertThat(DocumentStatDimension.estEnCircuit(document)).isFalse();
        assertThat(DocumentStatDimension.estEnVigueur(document)).isFalse();
    }

    @Test
    @DisplayName("L'archive prime sur l'obsolescence, celle-ci sur la validation")
    void ordreDesDrapeaux() {
        DocumentQms archive = document();
        archive.setArchived(true);
        archive.setObsolete(true);
        archive.setEsTraiter(true);
        assertThat(DocumentStatDimension.STATUT.extract(archive))
                .isEqualTo(DocumentStatDimension.STATUT_ARCHIVE);

        DocumentQms obsolete = document();
        obsolete.setObsolete(true);
        obsolete.setEsTraiter(true);
        // Un document périmé n'est plus applicable : le compter en vigueur l'aurait fait figurer
        // parmi les documents opposables.
        assertThat(DocumentStatDimension.estEnVigueur(obsolete)).isFalse();
        assertThat(DocumentStatDimension.estEnCircuit(obsolete)).isFalse();
    }

    @Test
    @DisplayName("Une étape vide ne fait pas passer le document pour engagé")
    void etapeVide_pasEnCircuit() {
        DocumentQms document = document();
        document.setCurrentEtape("   ");

        assertThat(DocumentStatDimension.estEnCircuit(document)).isFalse();
        assertThat(DocumentStatDimension.STATUT.extract(document))
                .isEqualTo(DocumentStatDimension.STATUT_BROUILLON);
    }
}
