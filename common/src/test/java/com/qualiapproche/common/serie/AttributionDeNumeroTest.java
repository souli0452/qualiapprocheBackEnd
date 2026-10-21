package com.qualiapproche.common.serie;

import com.qualiapproche.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Le socle des numérotations. La concurrence, elle, tient au verrou de la base et se vérifie
 * contre PostgreSQL ; ici se fixent les règles : partir de l'existant, sauter un numéro pris, ne
 * jamais déborder ni reculer, ne jamais attribuer hors transaction.
 */
class AttributionDeNumeroTest {

    static class Serie extends AbstractSerieEntity { }

    /** « TST-0001 », dans la série TST, partant de l'existant annoncé, bornée à 3 si demandé. */
    record Contexte(long existant, long max) { }

    static final StrategieDeSerie<Contexte> STRATEGIE = new StrategieDeSerie<>() {
        @Override public String cle(Contexte c) { return "TST"; }
        @Override public long valeurInitiale(Contexte c) { return c.existant(); }
        @Override public String formater(Contexte c, long rang) { return String.format("TST-%04d", rang); }
        @Override public long rangMaximal() { return 3; }
    };

    @SuppressWarnings("unchecked")
    private final SerieRepository<Serie> depot = mock(SerieRepository.class);
    private final Map<String, Serie> base = new HashMap<>();
    private final AttributionDeNumero<Serie> attribution = new AttributionDeNumero<>(depot);

    @BeforeEach
    void preparer() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(depot.verrouiller(anyString())).thenAnswer(i -> Optional.ofNullable(base.get((String) i.getArgument(0))));
        when(depot.creerSiAbsente(anyString(), anyLong())).thenAnswer(i -> {
            Serie s = new Serie();
            s.setCleSerie(i.getArgument(0));
            s.setValeurCourante(i.getArgument(1));
            return base.putIfAbsent(s.getCleSerie(), s) == null ? 1 : 0;
        });
        when(depot.save(any(Serie.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void nettoyer() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("Une série neuve reprend la suite de l'existant, puis avance d'un en un")
    void reprend_l_existant_puis_avance() {
        Contexte c = new Contexte(1, 3);
        assertThat(attribution.attribuer(STRATEGIE, c, n -> false)).isEqualTo("TST-0002");
        assertThat(attribution.attribuer(STRATEGIE, c, n -> false)).isEqualTo("TST-0003");
        assertThat(base.get("TST").getValeurCourante()).isEqualTo(3);
    }

    @Test
    @DisplayName("Un numéro déjà porté est sauté, pas dupliqué")
    void saute_un_numero_pris() {
        Set<String> pris = Set.of("TST-0001");
        assertThat(attribution.attribuer(STRATEGIE, new Contexte(0, 3), pris::contains)).isEqualTo("TST-0002");
    }

    @Test
    @DisplayName("Une série épuisée est refusée, jamais débordée")
    void serie_epuisee() {
        assertThatThrownBy(() -> attribution.attribuer(STRATEGIE, new Contexte(3, 3), n -> false))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("Hors transaction, aucune attribution : le verrou n'y tiendrait pas")
    void hors_transaction_refuse() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        assertThatThrownBy(() -> attribution.attribuer(STRATEGIE, new Contexte(0, 3), n -> false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Le calage avance la série, mais ne la fait jamais reculer")
    void calage_sans_recul() {
        assertThat(attribution.caler("TST", 10, c -> new Serie()).getValeurCourante()).isEqualTo(10);
        Serie s = new Serie();
        s.setCleSerie("TST");
        s.setValeurCourante(10);
        base.put("TST", s);

        assertThatThrownBy(() -> attribution.caler("TST", 9, c -> new Serie()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(attribution.caler("TST", 12, c -> new Serie()).getValeurCourante()).isEqualTo(12);
    }
}
