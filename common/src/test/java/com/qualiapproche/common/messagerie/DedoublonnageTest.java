package com.qualiapproche.common.messagerie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Un courtier livre « au moins une fois » : la seconde livraison d'un même message est écartée. */
class DedoublonnageTest {

    static class Trace extends AbstractMessageTraite { }

    @Test
    @DisplayName("Première livraison traitée, suivantes écartées ; un message sans identifiant passe")
    void premiere_livraison_seulement() {
        @SuppressWarnings("unchecked")
        MessageTraiteRepository<Trace> depot = mock(MessageTraiteRepository.class);
        Set<UUID> vus = new HashSet<>();
        when(depot.existsById(any())).thenAnswer(i -> vus.contains((UUID) i.getArgument(0)));
        when(depot.saveAndFlush(any(Trace.class))).thenAnswer(i -> {
            vus.add(((Trace) i.getArgument(0)).getId());
            return i.getArgument(0);
        });
        Dedoublonnage<Trace> dedoublonnage = new Dedoublonnage<>(depot, Trace::new);
        String id = UUID.randomUUID().toString();

        assertThat(dedoublonnage.premiereLivraison(id)).isTrue();
        assertThat(dedoublonnage.premiereLivraison(id)).isFalse();
        assertThat(dedoublonnage.premiereLivraison(null)).isTrue();
    }
}
