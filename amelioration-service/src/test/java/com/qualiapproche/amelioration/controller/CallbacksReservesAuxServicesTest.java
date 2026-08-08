package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.controller.internal.AmeliorationInternalCallbackController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les callbacks internes restent fermés aux utilisateurs.
 *
 * <p>Ces points de rappel écrivent l'état métier — statut du document, piste d'audit, avancement
 * d'une demande — sans passer par une décision de circuit : c'est le moteur de workflow qui les
 * appelle, sous son compte de service. Sous la seule authentification, n'importe quel agent muni
 * d'un jeton valide pouvait y poster un statut « validé ». Une garde retirée par mégarde ne se
 * verrait à aucun test fonctionnel — les appels de service continueraient de passer — d'où cette
 * vérification.</p>
 */
class CallbacksReservesAuxServicesTest {

    @Test
    @DisplayName("Le contrôleur de callbacks est réservé aux appels de service")
    void callbacks_reservesAuxServices() {
        PreAuthorize garde = AmeliorationInternalCallbackController.class.getAnnotation(PreAuthorize.class);
        assertThat(garde)
                .withFailMessage("Les callbacks internes doivent porter la garde de service : "
                        + "sans elle, un statut se force sans décision de circuit.")
                .isNotNull();
        assertThat(garde.value()).contains("appelDeService");
    }
}
