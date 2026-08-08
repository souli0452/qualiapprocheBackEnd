package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.LicenceInstallee;
import com.qualiapproche.referentiel.repository.LicenceInstalleeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Une licence ne vaut que chez son destinataire.
 *
 * <p>La signature prouve qu'une licence n'a pas été fabriquée ; elle ne dit rien de l'installation
 * qui la présente. Le code partenaire est inscrit dans la charge signée, donc infalsifiable — mais
 * tant que personne ne le compare à quoi que ce soit, il n'est qu'une étiquette d'affichage : la
 * licence du CHU s'installait telle quelle chez un autre client et y ouvrait tout.</p>
 *
 * <p>La comparaison exige un repère <b>local</b>. Il ne peut pas venir de la licence — ce serait
 * demander à la pièce à vérifier de fournir la référence de sa propre vérification. Il est donc
 * posé sur la direction au provisionnement, depuis {@code tenant-init.json}, et une propriété de
 * déploiement peut le remplacer sans reconstruire le produit.</p>
 *
 * <p>Le contrôle vaut à l'installation <b>et</b> à chaque lecture : une licence posée avant que le
 * code ne soit renseigné ne doit pas passer au travers.</p>
 */
class LicenceReserveeAuPartenaireTest {

    /** Clé publique de l'éditeur, telle que la configure referentiel-service. */
    private static final String CLE_PUBLIQUE =
            "MCowBQYDK2VwAyEAs2ajPoJ/faSW2FTtOV9tVXc/k3Fk2ZNc3SwQsyEJbxg";

    /** Licence réellement émise par l'outil pour le CHU du Burkina Faso — code {@code CHU-BF}. */
    private static final String LICENCE_DU_CHU =
            "QSL1.eyJyZWYiOiJMSUMtMjAyNi0wMDAxIiwiY2xpIjoiQ0hVLUJGIiwibm9tIjoiQ0hVIGR1IEJ1cmtpbmEg"
            + "RmFzbyIsImRlYiI6IjIwMjYtMDgtMDgiLCJmaW4iOiIyMDI3LTA4LTA4IiwibW9kIjpbIkRPQ1VNRU5UQUlS"
            + "RSIsIk5PTl9DT05GT1JNSVRFIl0sInVzciI6MjUsInR5cCI6IkNPTU1FUkNJQUxFIiwiZWR0IjoiRXNzZW50"
            + "aWVsIn0.qxeikzlSkbN-lngX2D89cFqYzyufZAGavgmvS3mcfJbu1l-UBlDdU76ahm9ZONZlaae-PCWuJPfdqrMoYLc0Dw";

    private LicenceInstalleeRepository repository;
    private StructureRepository structures;
    private CodeDeLInstallation installation;
    private LicenceInstalleeService service;

    @BeforeEach
    void setUp() {
        repository = mock(LicenceInstalleeRepository.class);
        structures = mock(StructureRepository.class);
        installation = new CodeDeLInstallation(structures);
        service = new LicenceInstalleeService(repository, installation);
        ReflectionTestUtils.setField(service, "clePublique", CLE_PUBLIQUE);
        ReflectionTestUtils.setField(service, "joursDEssai", 7);
        ReflectionTestUtils.setField(service, "modulesDEssai", "NON_CONFORMITE,DOCUMENTAIRE");

        when(repository.existsByType("ESSAI")).thenReturn(false);
        when(repository.save(any(LicenceInstallee.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    /** Le code posé au déploiement, celui qui l'emporte sur la base. */
    private void declare(String codePartenaire) {
        ReflectionTestUtils.setField(installation, "duDeploiement", codePartenaire);
        ReflectionTestUtils.setField(installation, "attendu", null);
    }

    /** Le code tel que le provisionnement l'a posé sur la direction : obfusqué. */
    private void declareEnBase(String codePartenaire) {
        ReflectionTestUtils.setField(installation, "duDeploiement", "");
        ReflectionTestUtils.setField(installation, "attendu", null);
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(
                Structure.builder().codePartenaire(CryptoUtils.encrypt(codePartenaire)).build()));
    }

    /**
     * Une licence du CHU déjà posée, dont la période couvre aujourd'hui.
     *
     * <p>Les dates sont portées par l'entité et non déduites du jeton : la validité se teste ici
     * indépendamment du calendrier, sans quoi ce test cesserait de valoir au terme de la licence
     * d'exemple.</p>
     */
    private void licenceDuChuDejaPosee() {
        LicenceInstallee posee = LicenceInstallee.builder()
                .jeton(LICENCE_DU_CHU)
                .type("COMMERCIALE")
                .reference("LIC-2026-0001")
                .partenaireCode("CHU-BF")
                .partenaireNom("CHU du Burkina Faso")
                .debut(LocalDate.now().minusMonths(1))
                .fin(LocalDate.now().plusMonths(6))
                .modules("DOCUMENTAIRE,NON_CONFORMITE")
                .utilisateursMax(25)
                .installeeLe(LocalDateTime.now())
                .dernierJourVu(LocalDate.now())
                .build();
        when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.of(posee));
    }

    // ---------------------------------------------------------------- à l'installation

    @Test
    @DisplayName("La licence d'un autre client est refusée, et le message dit pour qui elle vaut")
    void installation_licenceDUnAutreClient() {
        declare("MINSANTE");

        assertThatThrownBy(() -> service.installer(LICENCE_DU_CHU))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CHU")
                .hasMessageContaining("votre installation");
    }

    /**
     * Le refus tombe avant l'examen de la période : ce qu'il faut dire à celui qui colle une
     * licence qui n'est pas la sienne, c'est qu'elle ne l'est pas — et non qu'elle a expiré, qui
     * l'enverrait réclamer un renouvellement dont il n'a que faire.
     */
    @Test
    @DisplayName("Le mauvais destinataire prime sur toute autre objection")
    void installation_leDestinatairePrime() {
        declare("MINSANTE");

        assertThatThrownBy(() -> service.installer(LICENCE_DU_CHU))
                .hasMessageNotContainingAny("pris fin", "période de validité", "aucun module");
    }

    // ---------------------------------------------------------------- à chaque lecture

    @Test
    @DisplayName("Une licence posée pour un autre client est tenue pour absente")
    void lecture_baseRestaureeDepuisUnAutreClient() {
        declare("MINSANTE");
        licenceDuChuDejaPosee();

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ABSENTE");
        assertThat(etat.isActionsOuvertes()).isFalse();
        assertThat(etat.getModules()).isEmpty();
        assertThat(etat.getMessage()).contains("CHU");
    }

    @Test
    @DisplayName("Chez son destinataire, la licence ouvre tout — casse et espaces indifférents")
    void lecture_chezSonDestinataire() {
        declare("  chu-bf  ");
        licenceDuChuDejaPosee();

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ACTIVE");
        assertThat(etat.isActionsOuvertes()).isTrue();
        assertThat(etat.getModules()).containsExactlyInAnyOrder("DOCUMENTAIRE", "NON_CONFORMITE");
    }

    /**
     * À défaut de propriété, le code vient de la direction, où il est rangé obfusqué.
     *
     * <p>Le déchiffrement est vérifié pour lui-même : mal posé, il serait tenu pour absent, et le
     * contrôle se désarmerait sans bruit sur toutes les installations livrées.</p>
     *
     * <p>La licence d'exemple étant celle du CHU, elle est ici <b>refusée</b> : c'est précisément
     * ce qui prouve que le code en base est appliqué, et non seulement lu.</p>
     */
    @Test
    @DisplayName("Sans propriété, le code obfusqué de la direction prend le relais — et gouverne")
    void lecture_codeObfusqueDeLaBase() {
        declareEnBase("DQA");
        licenceDuChuDejaPosee();

        assertThat(installation.attendu()).isEqualTo("DQA");

        EtatLicenceDto etat = service.etat();
        assertThat(etat.getStatut()).isEqualTo("ABSENTE");
        assertThat(etat.isActionsOuvertes()).isFalse();
        assertThat(etat.getMessage()).contains("CHU");
    }

    /**
     * Le code n'est lu qu'une fois : il est posé au provisionnement et n'a aucune raison de
     * bouger. Le relire à chaque vérification ferait une requête par appel — et la passerelle en
     * fait une par écriture.
     */
    @Test
    @DisplayName("Le code n'est lu en base qu'une fois")
    void lecture_codeReteneuApresLaPremiereFois() {
        declareEnBase("DQA");

        assertThat(installation.attendu()).isEqualTo("DQA");
        assertThat(installation.attendu()).isEqualTo("DQA");

        org.mockito.Mockito.verify(structures, org.mockito.Mockito.times(1))
                .findAllByTypeStructure(TypeStructure.DIRECTION);
    }

    /**
     * Aucun code nulle part : le comportement d'avant est conservé, pour ne pas mettre à l'arrêt
     * les installations déjà livrées. C'est le démarrage qui le signale — un contrôle qui n'opère
     * pas doit se dire, jamais se supposer.
     */
    @Test
    @DisplayName("Aucun code nulle part : rien n'est refusé")
    void lecture_aucunCodeNullePart() {
        ReflectionTestUtils.setField(installation, "duDeploiement", "");
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of());

        assertThat(installation.reconnait("N_IMPORTE_QUOI")).isTrue();
        assertThat(installation.reconnait(null)).isTrue();
    }

    /**
     * Une absence n'est pas retenue : une installation qui pose son code après coup doit en
     * profiter sans attendre un redémarrage, sans quoi les structures créées entre-temps
     * n'en hériteraient jamais.
     */
    @Test
    @DisplayName("Un code posé après coup est vu sans redémarrage")
    void lecture_codePoseApresCoup() {
        ReflectionTestUtils.setField(installation, "duDeploiement", "");
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of());
        assertThat(installation.attendu()).isEmpty();

        declareEnBase("DQA");

        assertThat(installation.attendu()).isEqualTo("DQA");
    }

    /**
     * Le cas dégradé, et pourquoi il n'est pas un blocage.
     *
     * <p>Une colonne retouchée à la main, ou chiffrée avec une autre clé, rend le repère
     * illisible. Laisser l'exception remonter ferait tomber toute lecture de licence — donc
     * l'écran d'accueil, donc l'application entière — pour un contrôle qui n'est qu'un garde-fou
     * commercial. On retient l'absence de contrôle, et le journal le dit assez fort pour qu'on ne
     * le découvre pas le jour où une licence étrangère est acceptée.</p>
     */
    @Test
    @DisplayName("Un code illisible en base ne met pas le service à l'arrêt")
    void lecture_codeIllisibleEnBase() {
        ReflectionTestUtils.setField(installation, "duDeploiement", "");
        ReflectionTestUtils.setField(installation, "attendu", null);
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(
                Structure.builder().codePartenaire("ceci-n-est-pas-du-chiffre").build()));

        assertThat(installation.attendu()).isEmpty();
        assertThat(installation.reconnait("N_IMPORTE_QUOI")).isTrue();
    }

    @Test
    @DisplayName("La propriété de déploiement l'emporte sur un code illisible en base")
    void lecture_proprieteSauveUnCodeIllisible() {
        // C'est la porte de sortie : la propriété se change sans toucher à la base ni
        // reconstruire le produit, précisément quand la colonne est inexploitable.
        ReflectionTestUtils.setField(installation, "duDeploiement", "CHU-BF");
        ReflectionTestUtils.setField(installation, "attendu", null);
        when(structures.findAllByTypeStructure(TypeStructure.DIRECTION)).thenReturn(List.of(
                Structure.builder().codePartenaire("ceci-n-est-pas-du-chiffre").build()));

        assertThat(installation.attendu()).isEqualTo("CHU-BF");
        assertThat(installation.reconnait("MINSANTE")).isFalse();
    }

    @Test
    @DisplayName("Un code attendu écarte une licence qui n'en porte aucun")
    void licenceSansDestinataire() {
        declare("CHU-BF");

        assertThat(installation.reconnait(null)).isFalse();
        assertThat(installation.reconnait("  ")).isFalse();
    }

    @Test
    @DisplayName("L'essai gratuit, qui n'a pas de destinataire, n'est pas écarté par le contrôle")
    void lecture_essaiGratuit() {
        declare("MINSANTE");
        LicenceInstallee essai = LicenceInstallee.builder()
                .type("ESSAI")
                .reference("ESSAI-" + LocalDate.now())
                .partenaireNom("Essai gratuit")
                .debut(LocalDate.now())
                .fin(LocalDate.now().plusDays(7))
                .modules("NON_CONFORMITE,DOCUMENTAIRE")
                .installeeLe(LocalDateTime.now())
                .dernierJourVu(LocalDate.now())
                .build();
        when(repository.findTopByOrderByInstalleeLeDesc()).thenReturn(Optional.of(essai));

        EtatLicenceDto etat = service.etat();

        assertThat(etat.getStatut()).isEqualTo("ACTIVE");
        assertThat(etat.isActionsOuvertes()).isTrue();
    }
}
