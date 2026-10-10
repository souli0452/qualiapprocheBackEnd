package com.qualiapproche.support.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Protection des exports de documents confidentiels.
 *
 * <p>Le mot de passe propriétaire — celui qui lève l'interdiction de copier — était écrit en dur
 * (« qmssecure ») : le connaître suffisait à déverrouiller tous les exports. Il est désormais tiré
 * au hasard à chaque export, et rien ne le réutilise.</p>
 */
class ProtectionDesExportsConfidentielsTest {

    private byte[] exportProtege() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            QmsDocumentService.protegerContreLExtraction(document);
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        }
    }

    @Test
    @DisplayName("L'export s'ouvre sans mot de passe, mais n'autorise pas l'extraction du contenu")
    void ouvertureLibre_extractionInterdite() throws Exception {
        try (PDDocument relu = PDDocument.load(exportProtege())) {
            assertThat(relu.isEncrypted()).isTrue();
            assertThat(relu.getCurrentAccessPermission().canPrint()).isTrue();
            assertThat(relu.getCurrentAccessPermission().canExtractContent()).isFalse();
            assertThat(relu.getCurrentAccessPermission().isOwnerPermission()).isFalse();
        }
    }

    @Test
    @DisplayName("L'ancien mot de passe écrit en dur ne déverrouille plus rien")
    void ancienMotDePasse_sansEffet() throws Exception {
        byte[] export = exportProtege();

        // « qmssecure » n'est plus ni le mot de passe propriétaire ni celui d'ouverture : PDFBox
        // refuse un mot de passe qui n'est aucun des deux.
        assertThatThrownBy(() -> PDDocument.load(export, "qmssecure"))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    @DisplayName("Chaque export reçoit son propre mot de passe propriétaire, imprévisible")
    void motDePasse_aleatoireEtPropreAChaqueExport() {
        Set<String> tires = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            tires.add(QmsDocumentService.motDePasseProprietaireAleatoire());
        }

        assertThat(tires).hasSize(50);
        assertThat(tires).allSatisfy(motDePasse -> assertThat(motDePasse).hasSizeGreaterThanOrEqualTo(40));
    }
}
