package com.qualiapproche.common.courriel;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La file des courriels : déposer, remettre, reporter, abandonner — et ne rien perdre quand la
 * messagerie manque. Le dépôt et la revendication sont simulés ; la revendication atomique et la
 * sélection des échus s'appuient sur des requêtes que PostgreSQL exécute.
 */
class FileDeCourrielsTest {

    static class Courriel extends AbstractCourrielSortant { }

    @SuppressWarnings("unchecked")
    private final CourrielSortantRepository<Courriel> depot = mock(CourrielSortantRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> messagerie = mock(ObjectProvider.class);
    private final JavaMailSender serveur = mock(JavaMailSender.class);
    private final Map<UUID, Courriel> base = new HashMap<>();
    private Instant maintenant = Instant.parse("2026-10-10T10:00:00Z");
    private FileDeCourriels<Courriel> file;

    @BeforeEach
    void preparer() {
        Clock horloge = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return maintenant; }
        };
        file = new FileDeCourriels<>(depot, Courriel::new, messagerie, "qualite@exemple.bf",
                mock(PlatformTransactionManager.class), horloge);
        when(messagerie.getIfAvailable()).thenReturn(serveur);
        when(serveur.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        when(depot.save(any(Courriel.class))).thenAnswer(i -> {
            Courriel c = i.getArgument(0);
            base.put(c.getId(), c);
            return c;
        });
        when(depot.findById(any())).thenAnswer(i -> Optional.ofNullable(base.get((UUID) i.getArgument(0))));
        when(depot.echus(any(), anyInt())).thenAnswer(i -> base.values().stream()
                .filter(c -> c.getStatut() == StatutCourriel.A_ENVOYER || c.getStatut() == StatutCourriel.EN_COURS)
                .filter(c -> !c.getProchaineTentativeAt().isAfter(i.getArgument(0)))
                .map(AbstractCourrielSortant::getId).toList());
        when(depot.revendiquer(any(), any(), any(), any(), any())).thenAnswer(i -> {
            Courriel c = base.get((UUID) i.getArgument(0));
            if (c == null || c.getProchaineTentativeAt().isAfter(i.getArgument(3))) {
                return 0;
            }
            c.setStatut(StatutCourriel.EN_COURS);
            c.setTentatives(c.getTentatives() + 1);
            c.setProchaineTentativeAt(i.getArgument(4));
            return 1;
        });
    }

    private LocalDateTime ici() {
        return LocalDateTime.ofInstant(maintenant, ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Un courriel déposé attend, puis la tournée le remet et le marque envoyé")
    void deposer_puis_remettre() {
        UUID id = file.deposer("pilote@exemple.bf", null, "Audit AUD-2026-001", "<p>Bonjour</p>", "essai");
        assertThat(base.get(id).getStatut()).isEqualTo(StatutCourriel.A_ENVOYER);

        assertThat(file.tournee(50)).isEqualTo(1);

        verify(serveur).send(any(MimeMessage.class));
        assertThat(base.get(id).getStatut()).isEqualTo(StatutCourriel.ENVOYE);
        assertThat(base.get(id).getTentatives()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un échec reporte le courriel de 30 s, puis double le délai ; il n'est pas perdu")
    void echec_reporte() {
        UUID id = file.deposer("pilote@exemple.bf", null, "Sujet", "<p>x</p>", "essai");
        doThrow(new MailSendException("serveur injoignable")).when(serveur).send(any(MimeMessage.class));

        file.tournee(50);
        Courriel c = base.get(id);
        assertThat(c.getStatut()).isEqualTo(StatutCourriel.A_ENVOYER);
        assertThat(c.getProchaineTentativeAt()).isEqualTo(ici().plusSeconds(30));
        assertThat(c.getDerniereErreur()).contains("serveur injoignable");

        assertThat(file.tournee(50)).isZero(); // pas encore l'heure
        maintenant = maintenant.plusSeconds(30);
        file.tournee(50);
        assertThat(base.get(id).getProchaineTentativeAt()).isEqualTo(ici().plusSeconds(60));
    }

    @Test
    @DisplayName("Après huit tentatives, le courriel est abandonné et ne repart plus seul")
    void abandon_apres_huit() {
        UUID id = file.deposer("pilote@exemple.bf", null, "Sujet", "<p>x</p>", "essai");
        doThrow(new MailSendException("refusé")).when(serveur).send(any(MimeMessage.class));

        for (int i = 0; i < FileDeCourriels.TENTATIVES_MAX; i++) {
            file.tournee(50);
            maintenant = maintenant.plusSeconds(3600);
        }
        assertThat(base.get(id).getStatut()).isEqualTo(StatutCourriel.ABANDONNE);
        file.tournee(50);
        assertThat(base.get(id).getTentatives()).isEqualTo(FileDeCourriels.TENTATIVES_MAX);
    }

    @Test
    @DisplayName("Sans messagerie configurée, rien n'est consommé : les courriels attendent")
    void sans_messagerie_rien_ne_se_perd() {
        UUID id = file.deposer("pilote@exemple.bf", null, "Sujet", "<p>x</p>", "essai");
        when(messagerie.getIfAvailable()).thenReturn(null);

        assertThat(file.tournee(50)).isZero();
        assertThat(base.get(id).getStatut()).isEqualTo(StatutCourriel.A_ENVOYER);
        assertThat(base.get(id).getTentatives()).isZero();
    }

    @Test
    @DisplayName("Un courriel qu'une autre tournée a revendiqué n'est pas envoyé deux fois")
    void deja_revendique() {
        UUID id = file.deposer("pilote@exemple.bf", null, "Sujet", "<p>x</p>", "essai");
        when(depot.revendiquer(eq(id), any(), any(), any(), any())).thenReturn(0);

        assertThat(file.tournee(50)).isZero();
        verify(serveur, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Sans destinataire, rien n'est déposé")
    void sans_destinataire() {
        assertThat(file.deposer("  ", null, "Sujet", "x", "essai")).isNull();
        assertThat(base).isEmpty();
        assertThat(List.copyOf(base.values())).isEmpty();
    }
}
