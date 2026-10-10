package com.qualiapproche.common.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les filtres d'une liste paginée.
 *
 * <p>Ce qui compte ici est ce qui ne se voit pas : un filtre absent qui ne se poserait pas en
 * {@code IS NULL} (le {@code bytea} de PostgreSQL), une liste vide qui laisserait tout passer, et
 * l'ordre par défaut qui tiendrait tête à celui que l'écran demande.</p>
 */
class FiltresTest {

    @SuppressWarnings("unchecked")
    private final Root<Object> racine = mock(Root.class);
    private final CriteriaQuery<?> requete = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Predicate clause = mock(Predicate.class);
    @SuppressWarnings("unchecked")
    private final Path<Object> chemin = mock(Path.class);

    @SuppressWarnings("unchecked")
    @BeforeEach
    void preparer() {
        when(racine.get(anyString())).thenReturn(chemin);
        when(chemin.in(anyCollection())).thenReturn(clause);
        when(cb.equal(any(), any())).thenReturn(clause);
        when(cb.lower(any())).thenReturn((Expression) chemin);
        when(cb.coalesce(any(), any())).thenReturn((Expression) chemin);
        when(cb.like(any(), anyString())).thenReturn(clause);
        when(cb.or(any(Predicate[].class))).thenReturn(clause);
        when(cb.and(any(Predicate[].class))).thenReturn(clause);
        when(cb.disjunction()).thenReturn(clause);
    }

    @Test
    @DisplayName("Un filtre absent ne compte pas : seuls les filtres posés entrent dans la clause")
    void un_filtre_absent_ne_compte_pas() {
        Specification<Object> direction = Filtres.egal("directionId", "D1");

        Filtres.tous(direction, Filtres.egal("statut", null), Filtres.contient(List.of("libelle"), "  "))
                .toPredicate(racine, requete, cb);

        ArgumentCaptor<Predicate[]> posees = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(posees.capture());
        assertThat(posees.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("Le texte cherché se confronte à chacun des champs, sans égard à la casse")
    void le_texte_se_cherche_dans_chaque_champ() {
        Filtres.contient(List.of("nom", "adresse"), "  Siège ").toPredicate(racine, requete, cb);

        ArgumentCaptor<Predicate[]> alternatives = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).or(alternatives.capture());
        assertThat(alternatives.getValue()).hasSize(2);
        verify(cb, org.mockito.Mockito.times(2)).like(any(), org.mockito.ArgumentMatchers.eq("%siège%"));
    }

    @Test
    @DisplayName("Une liste de valeurs vide ne laisse rien passer, une liste absente laisse tout passer")
    void une_liste_vide_ne_laisse_rien_passer() {
        assertThat(Filtres.parmi("utilisateurId", null)).isNull();

        Filtres.parmi("utilisateurId", List.of()).toPredicate(racine, requete, cb);

        verify(cb).disjunction();
        verify(chemin, never()).in(anyCollection());
    }

    @Test
    @DisplayName("L'ordre demandé par l'écran prime ; sans lui, c'est l'ordre par défaut")
    void l_ordre_demande_prime() {
        Sort parDefaut = Sort.by("createdAt");

        Pageable demande = Filtres.ordonnee(PageRequest.of(2, 25, Sort.by(Sort.Direction.DESC, "nom")), parDefaut);
        assertThat(demande.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "nom"));

        Pageable imposee = Filtres.ordonnee(PageRequest.of(2, 25), parDefaut);
        assertThat(imposee.getPageNumber()).isEqualTo(2);
        assertThat(imposee.getPageSize()).isEqualTo(25);
        assertThat(imposee.getSort()).isEqualTo(parDefaut);

        assertThat(Filtres.ordonnee(Pageable.unpaged(), parDefaut).isUnpaged()).isTrue();
    }
}
