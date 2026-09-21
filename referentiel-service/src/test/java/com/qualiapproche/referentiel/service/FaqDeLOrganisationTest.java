package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.Faq;
import com.qualiapproche.referentiel.repository.FaqRepository;
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
    private FaqServiceImpl service;
    private MockedStatic<SecurityUtils> securite;

    @BeforeEach
    void preparer() {
        repository = mock(FaqRepository.class);
        // Les pièces jointes ont leurs propres cas : ici on éprouve le cloisonnement de la FAQ.
        FichierFaqService fichiers = mock(FichierFaqService.class);
        when(fichiers.desEntrees(any())).thenReturn(List.of());
        service = new FaqServiceImpl(repository, fichiers);
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
                .categorie("  Documents  ")
                .publiee(true).rang(2).build();
    }

    private Faq entreeDe(UUID directionId) {
        Faq entree = Faq.builder().question("q").reponse("r").publiee(true).rang(0).build();
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
        assertThat(rendue.getCategorie()).isEqualTo("Documents");
    }

    @Test
    @DisplayName("une catégorie vide devient absente, et non une chaîne de blancs")
    void categorieVide_devientAbsente() {
        FaqDto demande = demande();
        demande.setCategorie("   ");

        assertThat(service.create(demande).getCategorie()).isNull();
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
        when(repository.findAllByDirectionIdAndPublieeTrueOrderByRangAscCreatedAtAsc(MA_DIRECTION))
                .thenReturn(List.of(entreeDe(MA_DIRECTION)));

        assertThat(service.getPubliees()).hasSize(1);
        verify(repository).findAllByDirectionIdAndPublieeTrueOrderByRangAscCreatedAtAsc(MA_DIRECTION);
    }

    @Test
    @DisplayName("sans direction au jeton, la FAQ publiée est vide plutôt que celle de tous")
    void sansDirection_laFaqPublieeEstVide() {
        securite.when(SecurityUtils::getCurrentDirectionId).thenReturn(null);

        assertThat(service.getPubliees()).isEmpty();
        verify(repository, never())
                .findAllByDirectionIdAndPublieeTrueOrderByRangAscCreatedAtAsc(any());
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
    @DisplayName("une recherche vide ne filtre rien, plutôt que de ne rien rendre")
    void rechercheVide_neFiltreRien() {
        when(repository.rechercher(any(), any(), any())).thenReturn(Page.empty());

        service.getAll("   ", PageRequest.of(0, 20));

        verify(repository).rechercher(eq(MA_DIRECTION), eq(null), any());
    }
}
