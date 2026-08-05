package com.qualiapproche.workflow.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'état d'un circuit voyage par deux objets de transfert homonymes : celui du moteur, et celui de
 * {@code common} dans lequel les modules métier le désérialisent.
 *
 * <p>Une propriété ajoutée d'un seul côté est perdue en chemin, <b>sans erreur</b> : Jackson ignore
 * ce qu'il ne sait pas placer. C'est arrivé à la portée de décision d'un champ — le moteur la
 * publiait, l'écran ne la recevait jamais, et un justificatif de rejet se présentait donc aussi à
 * qui approuvait. Rien, à l'exécution, ne signalait la perte.</p>
 */
class ContratDesChampsPartagesTest {

    private Set<String> proprietesDe(Class<?> type) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Toute propriété d'un champ d'étape publiée par le moteur existe côté common")
    void champDEtape_contratsAlignes() throws IntrospectionException {
        Set<String> cotesMoteur = proprietesDe(WorkflowStepFieldDto.class);
        Set<String> coteCommun = proprietesDe(com.qualiapproche.common.dto.WorkflowStepFieldDto.class);

        assertThat(coteCommun)
                .withFailMessage("Le moteur publie %s, common n'en connaît que %s : la différence "
                        + "est silencieusement perdue à la désérialisation.", cotesMoteur, coteCommun)
                .containsAll(cotesMoteur);
    }

    @Test
    @DisplayName("Toute propriété d'un état de circuit publiée par le moteur existe côté common")
    void etatDuCircuit_contratsAlignes() throws IntrospectionException {
        Set<String> cotesMoteur = proprietesDe(WorkflowStateDto.class);
        Set<String> coteCommun = proprietesDe(com.qualiapproche.common.dto.WorkflowStateDto.class);

        assertThat(coteCommun).containsAll(cotesMoteur);
    }
}
