package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.SiteDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.Site;
import com.qualiapproche.referentiel.entities.mappers.SiteMapperImpl;
import com.qualiapproche.referentiel.repository.SiteRepository;
import com.qualiapproche.referentiel.service.impl.SiteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le paramétrage que lit le module Audit : borné à la direction de l'appelant, et retiré plutôt
 * qu'effacé, puisque d'autres services en gardent l'identifiant.
 */
class ParametrageParDirectionTest {

    private static final UUID MA_DIRECTION = UUID.randomUUID();

    private final SiteRepository depot = mock(SiteRepository.class);
    private final SiteService service = new SiteService(depot, new SiteMapperImpl());

    @BeforeEach
    void connecter() {
        Jwt jwt = Jwt.withTokenValue("jeton").header("alg", "none")
                .subject("admin").claim("direction_id", MA_DIRECTION.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(depot.save(any(Site.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void deconnecter() {
        SecurityContextHolder.clearContext();
    }

    private static Site site(UUID direction) {
        Site site = Site.builder().nom("Siège").actif(true).build();
        site.setId(UUID.randomUUID());
        site.setDirectionId(direction);
        return site;
    }

    @Test
    @DisplayName("Le site d'une autre direction est introuvable, et non refusé")
    void le_site_d_une_autre_direction_est_introuvable() {
        Site ailleurs = site(UUID.randomUUID());
        when(depot.findById(ailleurs.getId())).thenReturn(Optional.of(ailleurs));

        assertThatThrownBy(() -> service.lire(ailleurs.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.retirer(ailleurs.getId())).isInstanceOf(BusinessException.class);
        verify(depot, never()).save(any());
    }

    @Test
    @DisplayName("Supprimer un site le retire des choix, sans l'effacer")
    void supprimer_retire_sans_effacer() {
        Site siege = site(MA_DIRECTION);
        when(depot.findById(siege.getId())).thenReturn(Optional.of(siege));

        service.retirer(siege.getId());

        assertThat(siege.isActif()).isFalse();
        verify(depot, never()).delete(any(Site.class));
    }

    @Test
    @DisplayName("Les choix ne proposent que les sites actifs ; l'affichage les lit tous")
    void les_choix_ne_proposent_que_les_actifs() {
        service.lister(false);
        verify(depot).findAllByDirectionIdAndActifTrue(any(), any(Sort.class));
        service.lister(true);
        verify(depot).findAllByDirectionId(any(), any(Sort.class));
    }

    @Test
    @DisplayName("Un site créé est actif, et ni sa direction ni son identifiant ne viennent de la saisie")
    void un_site_cree_est_actif() {
        UUID glisse = UUID.randomUUID();
        SiteDto cree = service.creer(SiteDto.builder().id(glisse).nom("  Usine de Bobo  ").actif(false).build());

        assertThat(cree.isActif()).isTrue();
        assertThat(cree.getNom()).isEqualTo("Usine de Bobo");
        assertThat(cree.getId()).isNotEqualTo(glisse);
    }

    @Test
    @DisplayName("Sans direction dans le jeton, rien ne se lit")
    void sans_direction_rien_ne_se_lit() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> service.lister(false))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(depot, never()).findAllByDirectionIdAndActifTrue(any(), any(Sort.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("L'écran lit une page, dans l'ordre du paramétrage faute d'ordre demandé ; les choix lisent tout")
    void l_ecran_lit_une_page_les_choix_lisent_tout() {
        Site siege = site(MA_DIRECTION);
        when(depot.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(siege), PageRequest.of(0, 10), 1));

        Page<SiteDto> page = service.page(false, "siè", PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(SiteDto::getNom).containsExactly("Siège");
        ArgumentCaptor<Pageable> demande = ArgumentCaptor.forClass(Pageable.class);
        verify(depot).findAll(any(Specification.class), demande.capture());
        assertThat(demande.getValue().getSort()).isEqualTo(Sort.by("createdAt"));

        service.lister(false);
        verify(depot).findAllByDirectionIdAndActifTrue(MA_DIRECTION, Sort.by("createdAt"));
    }
}
