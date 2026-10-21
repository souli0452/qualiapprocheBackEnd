package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.JetonDeCompte;
import com.qualiapproche.userservice.entities.JetonDeCompte.Usage;
import com.qualiapproche.userservice.repository.JetonDeCompteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les liens envoyés par courriel. Jusqu'ici leur jeton n'était jamais lu : l'identifiant d'un
 * compte suffisait à en fixer le mot de passe, sans être connecté. Ces tests tiennent les quatre
 * conditions d'un lien valide — bon compte, bon usage, pas encore servi, pas expiré.
 */
class JetonsDeCompteTest {

    private static final String COMPTE = "8f1d0c4a-0000-4000-8000-0000000000a1";

    private final JetonDeCompteRepository depot = mock(JetonDeCompteRepository.class);
    private final Map<String, JetonDeCompte> parEmpreinte = new HashMap<>();
    private Instant maintenant = Instant.parse("2026-10-10T08:00:00Z");
    private JetonsDeCompte jetons;

    @BeforeEach
    void preparer() {
        jetons = new JetonsDeCompte(depot, new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return maintenant; }
        });
        when(depot.save(any(JetonDeCompte.class))).thenAnswer(i -> {
            JetonDeCompte j = i.getArgument(0);
            parEmpreinte.put(j.getEmpreinte(), j);
            return j;
        });
        when(depot.findByEmpreinte(anyString())).thenAnswer(i -> Optional.ofNullable(parEmpreinte.get(i.getArgument(0))));
        // La mise à jour conditionnelle, telle que la base l'exécute : une ligne touchée au plus.
        when(depot.marquerServi(any(), any())).thenAnswer(i -> {
            java.util.UUID id = i.getArgument(0);
            java.time.LocalDateTime quand = i.getArgument(1);
            synchronized (parEmpreinte) {
                for (JetonDeCompte j : parEmpreinte.values()) {
                    if (j.getId().equals(id) && j.getUtiliseLe() == null && j.getExpireLe().isAfter(quand)) {
                        j.setUtiliseLe(quand);
                        return 1;
                    }
                }
                return 0;
            }
        });
    }

    private void refuse(Runnable geste) {
        assertThatThrownBy(geste::run).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Le jeton ne se garde que par son empreinte, et sert une fois")
    void le_jeton_sert_une_fois() {
        String jeton = jetons.emettre(COMPTE, Usage.REINITIALISATION, Duration.ofMinutes(30));

        ArgumentCaptor<JetonDeCompte> garde = ArgumentCaptor.forClass(JetonDeCompte.class);
        verify(depot).save(garde.capture());
        assertThat(garde.getValue().getEmpreinte()).isNotEqualTo(jeton).hasSize(64);

        assertThatCode(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton)).doesNotThrowAnyException();
        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton));
    }

    @Test
    @DisplayName("Sans jeton, ou avec un jeton inventé, le compte reste fermé")
    void sans_jeton_rien() {
        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, null));
        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, ""));
        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, "jeton-invente"));
    }

    @Test
    @DisplayName("Le jeton d'un compte n'ouvre pas un autre compte, ni un autre usage")
    void un_compte_un_usage() {
        String jeton = jetons.emettre(COMPTE, Usage.VERIFICATION_COURRIEL, Duration.ofDays(7));

        refuse(() -> jetons.consommer("un-autre-compte", Usage.VERIFICATION_COURRIEL, jeton));
        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton));
    }

    @Test
    @DisplayName("Passé sa durée, le lien est refusé")
    void le_lien_expire() {
        String jeton = jetons.emettre(COMPTE, Usage.REINITIALISATION, Duration.ofMinutes(30));
        maintenant = maintenant.plus(Duration.ofMinutes(31));

        refuse(() -> jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton));
    }

    @Test
    @DisplayName("Deux présentations simultanées du même jeton : une seule passe")
    void deux_requetes_simultanees_une_seule_passe() throws Exception {
        String jeton = jetons.emettre(COMPTE, Usage.REINITIALISATION, Duration.ofMinutes(30));
        // Les deux requêtes lisent le jeton libre avant que l'une ou l'autre ne le consomme : c'est
        // la fenêtre qu'ouvrait l'ancien « lire, vérifier, enregistrer ».
        java.util.concurrent.CyclicBarrier lecturesFaites = new java.util.concurrent.CyclicBarrier(2);
        when(depot.findByEmpreinte(anyString())).thenAnswer(i -> {
            JetonDeCompte lu = parEmpreinte.get(i.getArgument(0));
            JetonDeCompte copie = JetonDeCompte.builder().id(lu.getId()).userId(lu.getUserId())
                    .usage(lu.getUsage()).empreinte(lu.getEmpreinte()).expireLe(lu.getExpireLe())
                    .utiliseLe(lu.getUtiliseLe()).creeLe(lu.getCreeLe()).build();
            lecturesFaites.await(5, java.util.concurrent.TimeUnit.SECONDS);
            return Optional.of(copie);
        });

        java.util.concurrent.ExecutorService fils = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> presentation = () -> {
                try {
                    jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton);
                    return true;
                } catch (BusinessException refus) {
                    return false;
                }
            };
            java.util.List<java.util.concurrent.Future<Boolean>> issues =
                    fils.invokeAll(java.util.List.of(presentation, presentation));
            long reussites = 0;
            for (java.util.concurrent.Future<Boolean> issue : issues) {
                if (issue.get()) {
                    reussites++;
                }
            }
            assertThat(reussites).isEqualTo(1);
        } finally {
            fils.shutdownNow();
        }
    }

    @Test
    @DisplayName("Le jeton n'est plus enregistré entier : seule la mise à jour conditionnelle le consomme")
    void la_consommation_passe_par_la_mise_a_jour_conditionnelle() {
        String jeton = jetons.emettre(COMPTE, Usage.REINITIALISATION, Duration.ofMinutes(30));
        org.mockito.Mockito.clearInvocations(depot);

        jetons.consommer(COMPTE, Usage.REINITIALISATION, jeton);

        verify(depot).marquerServi(any(), org.mockito.ArgumentMatchers.eq(
                java.time.LocalDateTime.ofInstant(maintenant, ZoneOffset.UTC)));
        verify(depot, org.mockito.Mockito.never()).save(any(JetonDeCompte.class));
    }

    @Test
    @DisplayName("Un nouveau lien annule les précédents du même usage")
    void un_nouveau_lien_annule_les_precedents() {
        jetons.emettre(COMPTE, Usage.REINITIALISATION, Duration.ofMinutes(30));

        verify(depot).annulerLesPrecedents(org.mockito.ArgumentMatchers.eq(COMPTE),
                org.mockito.ArgumentMatchers.eq(Usage.REINITIALISATION), any());
    }
}
