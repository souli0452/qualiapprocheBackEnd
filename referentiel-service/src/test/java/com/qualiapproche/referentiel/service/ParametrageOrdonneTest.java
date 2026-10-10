package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.DomaineApplicationDto;
import com.qualiapproche.common.dto.NiveauConfidentialiteDto;
import com.qualiapproche.common.dto.PrioriteDocumentDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.DomaineApplication;
import com.qualiapproche.referentiel.entities.NiveauConfidentialite;
import com.qualiapproche.referentiel.entities.PrioriteDocument;
import com.qualiapproche.referentiel.entities.mappers.DomaineApplicationMapperImpl;
import com.qualiapproche.referentiel.entities.mappers.NiveauConfidentialiteMapperImpl;
import com.qualiapproche.referentiel.entities.mappers.PrioriteDocumentMapperImpl;
import com.qualiapproche.referentiel.repository.DomaineApplicationRepository;
import com.qualiapproche.referentiel.repository.NiveauConfidentialiteRepository;
import com.qualiapproche.referentiel.repository.PrioriteDocumentRepository;
import com.qualiapproche.referentiel.service.impl.DomaineApplicationServiceImpl;
import com.qualiapproche.referentiel.service.impl.NiveauConfidentialiteServiceImpl;
import com.qualiapproche.referentiel.service.impl.PrioriteDocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les listes ordonnées du paramétrage documentaire : niveaux de confidentialité, domaines
 * d'application, priorités.
 *
 * <p>Elles alimentent les listes de choix des écrans : un libellé vide y apparaîtrait comme une
 * ligne blanche, et l'ordre que l'administrateur a fixé doit être celui que l'utilisateur voit.
 * Les correspondances réelles (MapStruct) sont employées : c'est par elles qu'une modification
 * pourrait effacer l'auteur d'une ligne.</p>
 */
class ParametrageOrdonneTest {

    private static final Sort PAR_ORDRE = Sort.by(Sort.Order.asc("ordre").nullsLast(), Sort.Order.asc("libelle"));

    @Nested
    @DisplayName("Niveaux de confidentialité")
    class NiveauxDeConfidentialite {

        private NiveauConfidentialiteRepository depot;
        private NiveauConfidentialiteServiceImpl service;

        @BeforeEach
        void preparer() {
            depot = mock(NiveauConfidentialiteRepository.class);
            service = new NiveauConfidentialiteServiceImpl(new NiveauConfidentialiteMapperImpl(), depot);
            when(depot.save(any(NiveauConfidentialite.class))).thenAnswer(appel -> appel.getArgument(0));
        }

        private NiveauConfidentialiteDto saisie(UUID id, String libelle, List<String> roles) {
            NiveauConfidentialiteDto dto = new NiveauConfidentialiteDto();
            dto.setId(id);
            dto.setLibelle(libelle);
            dto.setOrdre(2);
            dto.setRolesAutorises(roles == null ? null : new ArrayList<>(roles));
            return dto;
        }

        @Test
        @DisplayName("Un libellé vide est refusé en 400, à la création comme à la modification")
        void libelleObligatoire() {
            assertThatThrownBy(() -> service.create(saisie(null, "  ", List.of())))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
            assertThatThrownBy(() -> service.update(saisie(UUID.randomUUID(), null, List.of())))
                    .isInstanceOf(BusinessException.class);
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("La modification reprend libellé et rôles admis, sans effacer qui a créé la ligne ni quand")
        void modificationGardeLaCreation() {
            UUID id = UUID.randomUUID();
            LocalDateTime creeLe = LocalDateTime.of(2026, 3, 1, 9, 0);
            NiveauConfidentialite existant = new NiveauConfidentialite();
            existant.setId(id);
            existant.setLibelle("Interne");
            existant.setRolesAutorises(new ArrayList<>(List.of("PILOTE")));
            existant.setCreatedAt(creeLe);
            existant.setCreatedById("createur");
            when(depot.findById(id)).thenReturn(Optional.of(existant));

            NiveauConfidentialiteDto rendu = service.update(saisie(id, "Restreint", List.of("RESPONSABLE_QUALITE")));

            assertThat(rendu.getLibelle()).isEqualTo("Restreint");
            assertThat(existant.getRolesAutorises()).containsExactly("RESPONSABLE_QUALITE");
            assertThat(existant.getCreatedById()).isEqualTo("createur");
            assertThat(existant.getCreatedAt()).isEqualTo(creeLe);
            verify(depot).save(existant);
        }

        @Test
        @DisplayName("Modifier, lire ou supprimer un niveau inconnu : 404, rien n'est écrit")
        void inconnu() {
            UUID id = UUID.randomUUID();
            when(depot.findById(id)).thenReturn(Optional.empty());
            when(depot.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> service.update(saisie(id, "X", List.of())))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(BusinessException.class);
            assertThatThrownBy(() -> service.delete(id)).isInstanceOf(BusinessException.class);
            verify(depot, never()).save(any());
            verify(depot, never()).deleteById(any());
        }

        @Test
        @DisplayName("Sans ordre demandé, la page suit le rang puis le libellé ; un ordre demandé est respecté")
        void ordre() {
            when(depot.findAll(any(Pageable.class))).thenReturn(Page.empty());
            ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

            service.getAll(null, PageRequest.of(1, 10));
            service.getAll("", PageRequest.of(0, 10, Sort.by("libelle").descending()));

            verify(depot, org.mockito.Mockito.times(2)).findAll(page.capture());
            assertThat(page.getAllValues().get(0).getSort()).isEqualTo(PAR_ORDRE);
            assertThat(page.getAllValues().get(0).getPageNumber()).isEqualTo(1);
            assertThat(page.getAllValues().get(1).getSort()).isEqualTo(Sort.by("libelle").descending());
        }

        @Test
        @DisplayName("La recherche, épurée de ses blancs, porte sur le libellé et la description")
        void recherche() {
            when(depot.findByLibelleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(anyString(), anyString(),
                    any(Pageable.class))).thenReturn(Page.empty());

            service.getAll("  secret ", PageRequest.of(0, 10));

            verify(depot).findByLibelleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(eq("secret"),
                    eq("secret"), any(Pageable.class));
            verify(depot, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("La liste des choix suit le même ordre que la page")
        void listeDesChoix() {
            when(depot.findAll(any(Sort.class))).thenReturn(List.of());

            service.getAll();

            verify(depot).findAll(PAR_ORDRE);
        }
    }

    @Nested
    @DisplayName("Domaines d'application")
    class DomainesDApplication {

        private final DomaineApplicationRepository depot = mock(DomaineApplicationRepository.class);
        private final DomaineApplicationServiceImpl service =
                new DomaineApplicationServiceImpl(new DomaineApplicationMapperImpl(), depot);

        @Test
        @DisplayName("Un libellé vide est refusé ; un domaine inconnu est introuvable")
        void reglesDeSaisie() {
            DomaineApplicationDto vide = new DomaineApplicationDto();
            vide.setLibelle(" ");
            assertThatThrownBy(() -> service.create(vide))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

            UUID id = UUID.randomUUID();
            DomaineApplicationDto inconnu = new DomaineApplicationDto();
            inconnu.setId(id);
            inconnu.setLibelle("Hygiène");
            when(depot.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(inconnu))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
            verify(depot, never()).save(any());
        }

        @Test
        @DisplayName("Créé, le domaine garde sa saisie ; la liste des choix suit le rang")
        void creationEtOrdre() {
            when(depot.save(any(DomaineApplication.class))).thenAnswer(appel -> appel.getArgument(0));
            DomaineApplicationDto dto = new DomaineApplicationDto();
            dto.setLibelle("Hygiène");
            dto.setOrdre(3);

            assertThat(service.create(dto).getLibelle()).isEqualTo("Hygiène");

            when(depot.findAll(any(Sort.class))).thenReturn(List.of());
            service.getAll();
            verify(depot).findAll(PAR_ORDRE);
        }
    }

    @Nested
    @DisplayName("Priorités des documents")
    class Priorites {

        private final PrioriteDocumentRepository depot = mock(PrioriteDocumentRepository.class);
        private final PrioriteDocumentServiceImpl service =
                new PrioriteDocumentServiceImpl(new PrioriteDocumentMapperImpl(), depot);

        @Test
        @DisplayName("Les priorités se rangent par score, puis par libellé")
        void ordreParScore() {
            when(depot.findAll(any(Pageable.class))).thenReturn(Page.empty());
            ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);

            service.getAll(" ", PageRequest.of(0, 10));

            verify(depot).findAll(page.capture());
            assertThat(page.getValue().getSort())
                    .isEqualTo(Sort.by(Sort.Order.asc("score").nullsLast(), Sort.Order.asc("libelle")));
        }

        @Test
        @DisplayName("Un libellé vide est refusé ; supprimer une priorité inconnue : 404")
        void reglesDeSaisie() {
            PrioriteDocumentDto vide = new PrioriteDocumentDto();
            assertThatThrownBy(() -> service.create(vide)).isInstanceOf(BusinessException.class);

            UUID id = UUID.randomUUID();
            when(depot.existsById(id)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(id))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
            verify(depot, never()).deleteById(any());
        }

        @Test
        @DisplayName("La modification reprend score et couleur, sans toucher à la date de création")
        void modification() {
            UUID id = UUID.randomUUID();
            LocalDateTime creeLe = LocalDateTime.of(2026, 1, 5, 8, 0);
            PrioriteDocument existante = new PrioriteDocument();
            existante.setId(id);
            existante.setLibelle("Haute");
            existante.setScore(1);
            existante.setCreatedAt(creeLe);
            existante.setCreatedById("createur");
            when(depot.findById(id)).thenReturn(Optional.of(existante));
            when(depot.save(any(PrioriteDocument.class))).thenAnswer(appel -> appel.getArgument(0));
            PrioriteDocumentDto dto = new PrioriteDocumentDto();
            dto.setId(id);
            dto.setLibelle("Critique");
            dto.setScore(0);
            dto.setCouleur("#D32F2F");

            service.update(dto);

            assertThat(existante.getLibelle()).isEqualTo("Critique");
            assertThat(existante.getScore()).isZero();
            assertThat(existante.getCouleur()).isEqualTo("#D32F2F");
            assertThat(existante.getCreatedAt()).isEqualTo(creeLe);
            assertThat(existante.getCreatedById()).isEqualTo("createur");
        }
    }
}
