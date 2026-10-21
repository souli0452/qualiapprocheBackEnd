package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

/**
 * Sans serveur de fichiers, le refus dit ce que l'utilisateur cherchait à faire : il parlait de
 * « rapport ou plan » à qui joignait une preuve (recette du 17 oct. 2026, observation 19).
 */
class FichiersAuditTest {

    @SuppressWarnings("unchecked")
    private final FichiersAudit fichiers = new FichiersAudit(mock(ObjectProvider.class));

    private final MockMultipartFile photo =
            new MockMultipartFile("fichiers", "preuve.png", "image/png", new byte[]{1, 2, 3});

    private BusinessException refus(String... sousDossiers) {
        return catchThrowableOfType(() -> fichiers.deposer(photo, sousDossiers), BusinessException.class);
    }

    @Test
    void preuve_sansServeur_leRefusParleDePreuve() {
        BusinessException e = refus("audit-1", "preuves", "constat-1");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(e.getMessage()).startsWith("Joindre une preuve").doesNotContain("rapport");
    }

    @Test
    void planEtRapport_sansServeur_leRefusLesNomme() {
        assertThat(refus("audit-1", "plan").getMessage()).startsWith("Charger un plan d'audit reçu");
        assertThat(refus("audit-1", "rapport").getMessage()).startsWith("Charger un rapport d'audit reçu");
    }

    @Test
    void lecture_sansServeur_refusee() {
        BusinessException e = catchThrowableOfType(() -> fichiers.contenu("audit/1/preuves/x.png"),
                BusinessException.class);
        assertThat(e.getMessage()).startsWith("La relecture d'un fichier");
    }
}
