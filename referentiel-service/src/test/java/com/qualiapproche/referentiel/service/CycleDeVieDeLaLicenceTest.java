package com.qualiapproche.referentiel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.ModuleAbonnement;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.LicenceInstallee;
import com.qualiapproche.referentiel.repository.LicenceInstalleeRepository;
import com.qualiapproche.referentiel.repository.StructureRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Poser une licence, puis la relire jour après jour : ce qui est refusé à l'installation, ce que
 * l'état annonce, et ce qu'une horloge reculée n'achète pas.
 *
 * <p>Les licences sont signées ici par une paire de clés tirée pour le test, et datées par rapport
 * au jour courant : le test vaut quel que soit le jour où il tourne, ce que ne permettrait pas une
 * licence d'exemple émise une fois pour toutes.</p>
 */
class CycleDeVieDeLaLicenceTest {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static KeyPair cles;
    private static String clePublique;

    private LicenceInstalleeRepository repository;
    private CodeDeLInstallation installation;
    private LicenceInstalleeService service;

    @BeforeAll
    static void tirerLesCles() throws Exception {
        cles = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        clePublique = Base64.getEncoder().encodeToString(cles.getPublic().getEncoded());
    }

    @BeforeEach
    void preparer() {
        repository = mock(LicenceInstalleeRepository.class);
        installation = new CodeDeLInstallation(mock(StructureRepository.class));
        ReflectionTestUtils.setField(installation, "duDeploiement", "DQA");
        service = new LicenceInstalleeService(repository, installation);
        ReflectionTestUtils.setField(service, "clePublique", clePublique);
        when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    /** Émet une licence comme le ferait l'outil de l'éditeur, signée par la clé du test. */
    private static String emettre(String reference, String type, LocalDate debut, LocalDate fin,
                                  List<String> modules) throws Exception {
        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("ref", reference);
        charge.put("cli", "DQA");
        charge.put("nom", "Direction de la Qualité");
        charge.put("deb", debut == null ? null : debut.toString());
        charge.put("fin", fin == null ? null : fin.toString());
        charge.put("mod", modules);
        charge.put("usr", 25);
        charge.put("typ", type);
        String corps = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JSON.writeValueAsBytes(charge));
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(cles.getPrivate());
        signature.update(corps.getBytes(StandardCharsets.US_ASCII));
        return "QSL1." + corps + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    private static String commerciale(LocalDate debut, LocalDate fin) throws Exception {
        return emettre("LIC-TEST-1", "COMMERCIALE", debut, fin, List.of("NON_CONFORMITE", "DOCUMENTAIRE"));
    }

    /** Une licence déjà posée : les dates et le type de l'entité sont ceux que l'état relit. */
    private LicenceInstallee posee(String type, LocalDate debut, LocalDate fin, LocalDate dernierJourVu)
            throws Exception {
        LicenceInstallee l = LicenceInstallee.builder()
                .jeton(emettre("LIC-TEST-1", type, debut, fin, List.of("NON_CONFORMITE")))
                .type(type).reference("LIC-TEST-1").partenaireCode("DQA")
                .partenaireNom("Direction de la Qualité").debut(debut).fin(fin)
                .modules(" NON_CONFORMITE , ,DOCUMENTAIRE").utilisateursMax(25)
                .installeeLe(LocalDateTime.now()).dernierJourVu(dernierJourVu).build();
        when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.of(l));
        return l;
    }

    @Test
    @DisplayName("Sans clé de vérification, le service refuse de démarrer")
    void sansCleDeVerification() {
        ReflectionTestUtils.setField(service, "clePublique", " ");

        assertThatThrownBy(service::verifierLaCleDeVerification)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QUALISIRA_CLE_PUBLIQUE");
    }

    @Nested
    @DisplayName("À l'installation")
    class Installation {

        @Test
        @DisplayName("Une licence valide est enregistrée telle que signée, avec son auteur, et ouvre aussitôt")
        void licenceValide() throws Exception {
            Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "RS256").subject("u1")
                    .claim("name", "Awa Traoré")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
            String jeton = commerciale(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
            // La base simulée rend, à la relecture, ce qui vient d'y être écrit.
            java.util.concurrent.atomic.AtomicReference<LicenceInstallee> enBase =
                    new java.util.concurrent.atomic.AtomicReference<>();
            when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> {
                enBase.compareAndSet(null, appel.getArgument(0));
                return appel.getArgument(0);
            });
            when(repository.findTopByOrderByInstalleeLeDesc()).thenAnswer(appel -> Optional.ofNullable(enBase.get()));

            // Collée depuis un courriel : coupée de sauts de ligne.
            EtatLicenceDto etat = service.installer(jeton.substring(0, 20) + "\n  " + jeton.substring(20));

            LicenceInstallee l = enBase.get();
            assertThat(l.getJeton()).isEqualTo(jeton);
            assertThat(l.getType()).isEqualTo("COMMERCIALE");
            assertThat(l.getModules()).isEqualTo("NON_CONFORMITE,DOCUMENTAIRE");
            assertThat(l.getInstalleePar()).isEqualTo("Awa Traoré");
            assertThat(l.getUtilisateursMax()).isEqualTo(25);
            assertThat(etat.getStatut()).isEqualTo("ACTIVE");
            assertThat(etat.isActionsOuvertes()).isTrue();
        }

        @Test
        @DisplayName("Un essai est rangé comme tel")
        void essai() throws Exception {
            String jeton = emettre("ESSAI-1", "essai", LocalDate.now(), LocalDate.now().plusDays(30),
                    List.of("NON_CONFORMITE"));
            ArgumentCaptor<LicenceInstallee> enregistree = ArgumentCaptor.forClass(LicenceInstallee.class);

            service.installer(jeton);

            verify(repository).save(enregistree.capture());
            assertThat(enregistree.getValue().getType()).isEqualTo("ESSAI");
        }

        @Test
        @DisplayName("Un texte qui n'est pas une licence : 400, rien n'est enregistré")
        void illisible() {
            assertThatThrownBy(() -> service.installer("pas-une-licence"))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Une licence signée par une autre clé : 400, signature invalide")
        void autreCle() throws Exception {
            String jeton = commerciale(LocalDate.now(), LocalDate.now().plusYears(1));
            ReflectionTestUtils.setField(service, "clePublique", Base64.getEncoder().encodeToString(
                    KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPublic().getEncoded()));

            assertThatThrownBy(() -> service.installer(jeton))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("signature");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Une licence déjà échue : 400, la date de fin dite")
        void echue() throws Exception {
            LocalDate fin = LocalDate.now().minusDays(1);
            String jeton = commerciale(LocalDate.now().minusYears(1), fin);

            assertThatThrownBy(() -> service.installer(jeton))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("a pris fin le " + fin.format(JOUR));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Une licence sans période, ou sans module : 400")
        void incomplete() throws Exception {
            String sansPeriode = emettre("L-2", "COMMERCIALE", null, null, List.of("NON_CONFORMITE"));
            String sansModule = emettre("L-3", "COMMERCIALE", LocalDate.now(), LocalDate.now().plusDays(10), List.of());

            assertThatThrownBy(() -> service.installer(sansPeriode)).hasMessageContaining("période de validité");
            assertThatThrownBy(() -> service.installer(sansModule)).hasMessageContaining("aucun module");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Une licence déjà installée : 409, sans doublon en base")
        void dejaInstallee() throws Exception {
            when(repository.existsByReference("LIC-TEST-1")).thenReturn(true);
            String jeton = commerciale(LocalDate.now(), LocalDate.now().plusYears(1));

            assertThatThrownBy(() -> service.installer(jeton))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("À la lecture")
    class Lecture {

        @Test
        @DisplayName("Aucune licence : absente, actions fermées, aucun module")
        void absente() {
            when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.empty());

            EtatLicenceDto etat = service.etat();

            assertThat(etat.getStatut()).isEqualTo("ABSENTE");
            assertThat(etat.isActionsOuvertes()).isFalse();
            assertThat(etat.getModules()).isEmpty();
            assertThat(service.ouvre(ModuleAbonnement.NON_CONFORMITE)).isFalse();
        }

        @Test
        @DisplayName("Les modules se lisent sans les blancs ni les vides de la colonne")
        void modulesNettoyes() throws Exception {
            posee("COMMERCIALE", LocalDate.now().minusDays(10), LocalDate.now().plusDays(100), LocalDate.now());

            assertThat(service.etat().getModules()).containsExactly("NON_CONFORMITE", "DOCUMENTAIRE");
            assertThat(service.ouvre(ModuleAbonnement.NON_CONFORMITE)).isTrue();
        }

        @Test
        @DisplayName("Une licence expirée garde les données consultables, mais ferme les actions et les modules")
        void expiree() throws Exception {
            LocalDate fin = LocalDate.now().minusDays(1);
            posee("COMMERCIALE", LocalDate.now().minusYears(1), fin, LocalDate.now());

            EtatLicenceDto etat = service.etat();

            assertThat(etat.getStatut()).isEqualTo("EXPIREE");
            assertThat(etat.isActionsOuvertes()).isFalse();
            assertThat(etat.getJoursRestants()).isEqualTo(-1);
            assertThat(etat.getMessage()).contains("a pris fin le " + fin.format(JOUR))
                    .contains("consultables").contains("renouvellement");
            assertThat(service.ouvre(ModuleAbonnement.NON_CONFORMITE)).isFalse();
        }

        @Test
        @DisplayName("Un essai échu se dit essai, et renvoie à l'installation d'une licence")
        void essaiEchu() throws Exception {
            posee("ESSAI", LocalDate.now().minusDays(31), LocalDate.now().minusDays(1), LocalDate.now());

            assertThat(service.etat().getMessage()).startsWith("Votre essai gratuit a pris fin le")
                    .contains("installation d'une licence");
        }

        @Test
        @DisplayName("Reculer l'horloge ne rouvre pas une licence échue : le dernier jour vu fait foi")
        void horlogeReculee() throws Exception {
            LocalDate dernierJourVu = LocalDate.now().plusDays(10);
            LicenceInstallee l = posee("COMMERCIALE", LocalDate.now().minusYears(1),
                    LocalDate.now().plusDays(5), dernierJourVu);

            EtatLicenceDto etat = service.etat();

            assertThat(etat.getStatut()).isEqualTo("EXPIREE");
            assertThat(etat.getJoursRestants()).isEqualTo(-5);
            assertThat(l.getDernierJourVu()).isEqualTo(dernierJourVu);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Un jour nouveau avance le repère et l'enregistre")
        void repereAvance() throws Exception {
            LicenceInstallee l = posee("COMMERCIALE", LocalDate.now().minusYears(1),
                    LocalDate.now().plusYears(1), LocalDate.now().minusDays(3));

            service.etat();

            assertThat(l.getDernierJourVu()).isEqualTo(LocalDate.now());
            verify(repository).save(l);
        }

        @Test
        @DisplayName("Avant son premier jour, une licence n'ouvre pas encore les actions")
        void pasEncoreCommencee() throws Exception {
            posee("COMMERCIALE", LocalDate.now().plusDays(5), LocalDate.now().plusYears(1), LocalDate.now());

            EtatLicenceDto etat = service.etat();

            assertThat(etat.isActionsOuvertes()).isFalse();
        }

        @Test
        @DisplayName("Le bandeau dit l'essai, l'échéance proche, ou la simple validité")
        void messages() throws Exception {
            LocalDate debut = LocalDate.now().minusDays(1);

            posee("ESSAI", debut, LocalDate.now().plusDays(7), LocalDate.now());
            assertThat(service.etat().getMessage()).startsWith("Essai gratuit : il reste 7 jour(s).");

            posee("COMMERCIALE", debut, LocalDate.now().plusDays(30), LocalDate.now());
            assertThat(service.etat().getMessage()).contains("dans 30 jour(s)").contains("renouveler");

            LocalDate lointaine = LocalDate.now().plusDays(31);
            posee("COMMERCIALE", debut, lointaine, LocalDate.now());
            assertThat(service.etat().getMessage()).isEqualTo("Licence active jusqu'au " + lointaine.format(JOUR) + ".");
        }
    }
}
