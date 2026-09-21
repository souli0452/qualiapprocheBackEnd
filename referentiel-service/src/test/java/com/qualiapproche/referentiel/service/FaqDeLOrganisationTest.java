package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.Faq;
import com.qualiapproche.referentiel.repository.FaqRepository;
import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.referentiel.service.impl.FaqServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La foire aux questions : qui peut l'écrire, et qui peut la lire.
 *
 * <p>Ce qu'on y met devient la parole de l'organisation — affichée dans l'aide, et recopiée par
 * l'assistant IA dans sa consigne. Une entrée qui franchirait la direction ferait donc dire à
 * l'assistant, chez l'une, ce qu'on lui a appris chez l'autre. Le cloisonnement se vérifie ici
 * plutôt que de se déduire d'un filtre de requête qu'une évolution pourrait retirer.</p>
 */
class FaqDeLOrganisationTest {

    private static final UUID MA_DIRECTION = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID AUTRE_DIRECTION = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ENTREE = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private FaqRepository repository;
    private PermissionChecker permissions;
    private FaqServiceImpl service;
    private MockedStatic<SecurityUtils> securite;

    @BeforeEach
    void preparer() {
        repository = mock(FaqRepository.class);
        // Les pièces jointes ont leurs propres cas : ici on éprouve le cloisonnement de la FAQ.
        FichierFaqService fichiers = mock(FichierFaqService.class);
        when(fichiers.desEntrees(any())).thenReturn(List.of());
        permissions = mock(PermissionChecker.class);
        // Par défaut, l'appelant peut publier : les cas qui éprouvent le contraire le disent.
        when(permissions.detient(any(String[].class))).thenReturn(true);
        service = new FaqServiceImpl(repository, fichiers, permissions);
        securite = mockStatic(SecurityUtils.class);
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(MA_DIRECTION);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void ranger() {
        securite.close();
    }

    private FaqDto demande() {
        return FaqDto.builder()
                .id(ENTREE)
                .question("  Qui vise une procédure ?  ")
                .reponse("  Le pilote, puis la qualité.  ")
                .publiee(true).build();
    }

    private Faq entreeDe(UUID directionId) {
        Faq entree = Faq.builder().question("q").reponse("r").publiee(true).build();
        entree.setId(ENTREE);
        entree.setDirectionId(directionId);
        return entree;
    }

    @Test
    @DisplayName("une entrée créée est nettoyée de ses espaces de saisie")
    void entreeCreee_estNettoyee() {
        FaqDto rendue = service.create(demande());

        assertThat(rendue.getQuestion()).isEqualTo("Qui vise une procédure ?");
        assertThat(rendue.getReponse()).isEqualTo("Le pilote, puis la qualité.");
    }


    @Test
    @DisplayName("sans le droit de publier, une réponse créée reste un brouillon")
    void sansLeDroitDePublier_laReponseResteUnBrouillon() {
        when(permissions.detient(any(String[].class))).thenReturn(false);
        FaqDto demande = demande();
        demande.setPubliee(true);

        // L'écran masque déjà l'interrupteur ; un appel direct ne doit pas davantage y parvenir.
        assertThat(service.create(demande).isPubliee()).isFalse();
    }

    @Test
    @DisplayName("sans le droit de publier, on ne retire pas non plus une réponse en service")
    void sansLeDroitDePublier_onNeDepubliePas() {
        when(permissions.detient(any(String[].class))).thenReturn(false);
        Faq existante = entreeDe(MA_DIRECTION);
        existante.setPubliee(true);
        when(repository.findById(ENTREE)).thenReturn(Optional.of(existante));

        FaqDto demande = demande();
        demande.setPubliee(false);

        assertThat(service.update(demande).isPubliee()).isTrue();
    }

    @Test
    @DisplayName("publier un lot ne change que ce qui doit l'être")
    void publierUnLot_neChangeQueLeNecessaire() {
        Faq brouillon = entreeDe(MA_DIRECTION);
        when(repository.findById(ENTREE)).thenReturn(Optional.of(brouillon));

        assertThat(service.publier(List.of(ENTREE), true)).isEqualTo(0);

        brouillon.setPubliee(false);
        assertThat(service.publier(List.of(ENTREE), true)).isEqualTo(1);
        assertThat(brouillon.isPubliee()).isTrue();
    }

    @Test
    @DisplayName("un lot mêlant une autre organisation est refusé en entier")
    void lotMelant_estRefuseEnEntier() {
        when(repository.findById(ENTREE)).thenReturn(Optional.of(entreeDe(AUTRE_DIRECTION)));

        // Chaque entrée est vérifiée une à une : un lot ne passe pas parce qu'il est un lot.
        catchThrowableOfType(() -> service.publier(List.of(ENTREE), true), BusinessException.class);

        verify(repository, never()).saveAll(any());
    }

    @Test
    @DisplayName("l'entrée d'une autre organisation est introuvable, et non refusée")
    void entreeDUneAutreOrganisation_estIntrouvable() {
        when(repository.findById(ENTREE)).thenReturn(Optional.of(entreeDe(AUTRE_DIRECTION)));

        BusinessException refus = catchThrowableOfType(
                () -> service.update(demande()), BusinessException.class);

        // 404 et non 403 : répondre « elle existe mais n'est pas à vous » dirait déjà quelque
        // chose de cette autre organisation.
        assertThat(refus.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("on ne supprime pas davantage l'entrée d'une autre organisation")
    void suppressionDUneAutreOrganisation_estRefusee() {
        when(repository.findById(ENTREE)).thenReturn(Optional.of(entreeDe(AUTRE_DIRECTION)));

        catchThrowableOfType(() -> service.delete(ENTREE), BusinessException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("les entrées publiées ne sont demandées que pour sa propre organisation")
    void entreesPubliees_borneesALOrganisation() {
        when(repository.findAllByDirectionIdAndPublieeTrueOrderByCreatedAtAsc(MA_DIRECTION))
                .thenReturn(List.of(entreeDe(MA_DIRECTION)));

        assertThat(service.getPubliees()).hasSize(1);
        verify(repository).findAllByDirectionIdAndPublieeTrueOrderByCreatedAtAsc(MA_DIRECTION);
    }

    @Test
    @DisplayName("sans direction au jeton, la FAQ publiée est vide plutôt que celle de tous")
    void sansDirection_laFaqPublieeEstVide() {
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(null);

        assertThat(service.getPubliees()).isEmpty();
        verify(repository, never())
                .findAllByDirectionIdAndPublieeTrueOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("sans direction au jeton, on n'écrit pas dans le vide")
    void sansDirection_onNEcritPas() {
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(null);

        BusinessException refus = catchThrowableOfType(
                () -> service.create(demande()), BusinessException.class);

        assertThat(refus.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("une recherche absente devient une chaîne vide, jamais un nul")
    void rechercheAbsente_nEstJamaisNulle() {
        when(repository.rechercher(any(), anyBoolean(), any(), any())).thenReturn(Page.empty());

        service.getAll(true, null, PageRequest.of(0, 20));

        // Un nul non typé finit en bytea côté PostgreSQL, et le LIKE échoue sur un
        // « lower(bytea) does not exist » : la liste entière rendait 500 avant même qu'une
        // question soit écrite. La chaîne vide donne LIKE '%%', qui retient tout.
        verify(repository).rechercher(eq(MA_DIRECTION), eq(true), eq(""), any());
    }

    @Test
    @DisplayName("une recherche de blancs ne filtre rien non plus")
    void rechercheDeBlancs_neFiltreRien() {
        when(repository.rechercher(any(), anyBoolean(), any(), any())).thenReturn(Page.empty());

        service.getAll(true, "   ", PageRequest.of(0, 20));

        verify(repository).rechercher(eq(MA_DIRECTION), eq(true), eq(""), any());
    }

    @Test
    @DisplayName("la liste entière n'emprunte pas la requête de recherche")
    void listeEntiere_nEmprunteRienALaRecherche() {
        when(repository.findAllByDirectionIdOrderByCreatedAtAsc(MA_DIRECTION))
                .thenReturn(List.of(entreeDe(MA_DIRECTION)));

        assertThat(service.getAll()).hasSize(1);

        // L'y employer obligeait à lui passer un terme qui n'existe pas.
        verify(repository, never()).rechercher(any(), anyBoolean(), any(), any());
    }
}
