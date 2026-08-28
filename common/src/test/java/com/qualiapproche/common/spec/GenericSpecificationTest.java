package com.qualiapproche.common.spec;

import com.qualiapproche.common.api.CriteriaDto;
import com.qualiapproche.common.api.FilterExtra;
import com.qualiapproche.common.api.FilterOperator;
import com.qualiapproche.common.exception.BusinessException;
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
import org.mockito.ArgumentMatchers;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La traduction des critères d'un écran en clause SQL.
 *
 * <p>Ce que ces vérifications tiennent, c'est ce qui ne se voit pas autrement qu'en production : les
 * valeurs arrivent d'un corps JSON, donc en <b>texte</b> — un identifiant, une date, une valeur
 * d'énumération — et comparer une chaîne à une colonne typée échoue à l'exécution, sur la première
 * recherche, après le déploiement.</p>
 *
 * <p>Et le comportement d'un critère <b>posé sans valeur exploitable</b> : rendre alors la liste
 * entière est le défaut le plus difficile à voir, puisque l'utilisateur obtient des résultats et
 * que rien ne lui dit que son filtre n'a pas porté.</p>
 */
class GenericSpecificationTest {

    private static final String IDENTIFIANT = "8f1d0c4a-0000-4000-8000-0000000000a1";

    @SuppressWarnings("unchecked")
    private final Root<Object> root = mock(Root.class);
    private final CriteriaQuery<?> requete = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Predicate clause = mock(Predicate.class);

    @SuppressWarnings("unchecked")
    private final Path<Object> chemin = mock(Path.class);

    private final GenericSpecification<Object> specification = new GenericSpecification<>();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(root.get(anyString())).thenReturn(chemin);
        when(chemin.get(anyString())).thenReturn(chemin);
        when(chemin.in(anyCollection())).thenReturn(clause);
        when(chemin.as(any())).thenReturn((Expression) chemin);
        when(cb.conjunction()).thenReturn(clause);
        when(cb.disjunction()).thenReturn(clause);
        when(cb.and(ArgumentMatchers.<Predicate>any())).thenReturn(clause);
        when(cb.or(ArgumentMatchers.<Predicate>any())).thenReturn(clause);
        when(cb.equal(any(Expression.class), any(Object.class))).thenReturn(clause);
        when(cb.like(any(), anyString())).thenReturn(clause);
        when(cb.lower(any())).thenReturn((Expression) chemin);
        when(cb.greaterThanOrEqualTo(any(), any(Comparable.class))).thenReturn(clause);
        when(cb.lessThanOrEqualTo(any(), any(Comparable.class))).thenReturn(clause);
    }

    private void appliquer(CriteriaDto criteres) {
        Specification<Object> spec = specification.query(criteres, List.of("numeroReference"));
        spec.toPredicate(root, requete, cb);
    }

    private CriteriaDto avec(FilterExtra... filtres) {
        return CriteriaDto.builder().filters(List.of(filtres)).build();
    }

    private FilterExtra filtre(String champ, FilterOperator operateur, Object valeur) {
        return FilterExtra.builder().field(champ).operator(operateur).value(valeur).build();
    }

    /** Les valeurs comparées à une colonne, telles que la clause {@code IN} les reçoit. */
    @SuppressWarnings("unchecked")
    private List<Collection<?>> ensemblesCompares() {
        ArgumentCaptor<Collection<?>> valeurs = ArgumentCaptor.forClass(Collection.class);
        verify(chemin, org.mockito.Mockito.atLeastOnce()).in(valeurs.capture());
        return (List<Collection<?>>) (List<?>) valeurs.getAllValues();
    }

    // ------------------------------------------------------------------ aucun critère

    @Test
    @DisplayName("Une recherche sans critère ne restreint rien et ne touche à aucune colonne")
    void aucunCritere_neRestreintRien() {
        appliquer(null);
        appliquer(new CriteriaDto());

        verify(root, never()).get(anyString());
        verify(cb, org.mockito.Mockito.times(2)).conjunction();
    }

    @Test
    @DisplayName("Un critère incomplet est ignoré, sans faire échouer la recherche ni l'élargir")
    void critereIncomplet_ignore() {
        appliquer(avec(filtre(null, FilterOperator.EQ, "x"),
                FilterExtra.builder().field("status").value("x").build()));

        verify(root, never()).get(anyString());
    }

    // ------------------------------------------------------------------ la conversion des valeurs

    @Test
    @DisplayName("Une sélection multiple d'identifiants est comparée sous leur forme UUID")
    void selectionMultiple_convertieEnIdentifiants() {
        when(chemin.getJavaType()).thenReturn((Class) UUID.class);

        appliquer(avec(filtre("niveauNonConformiteId", FilterOperator.IN, List.of(IDENTIFIANT))));

        verify(root).get("niveauNonConformiteId");
        assertThat(ensemblesCompares()).containsExactly(List.of(UUID.fromString(IDENTIFIANT)));
    }

    @Test
    @DisplayName("Une date seule vaut le début de la journée sur une colonne horodatée")
    void dateSeule_ramenceeAuDebutDeJournee() {
        when(chemin.getJavaType()).thenReturn((Class) LocalDateTime.class);

        appliquer(avec(filtre("createdAt", FilterOperator.GTE, "2026-08-01")));

        ArgumentCaptor<Comparable> borne = ArgumentCaptor.forClass(Comparable.class);
        verify(cb).greaterThanOrEqualTo(any(), borne.capture());
        // L'écran envoie un jour, pas un instant : exiger l'heure de sa part n'apporterait rien.
        assertThat(borne.getValue()).isEqualTo(LocalDate.of(2026, 8, 1).atStartOfDay());
    }

    @Test
    @DisplayName("Une valeur d'énumération arrive en texte et repart en constante")
    void enumeration_convertie() {
        when(chemin.getJavaType()).thenReturn((Class) Saison.class);

        appliquer(avec(filtre("saison", FilterOperator.EQ, "ETE")));

        ArgumentCaptor<Object> valeur = ArgumentCaptor.forClass(Object.class);
        verify(cb).equal(any(Expression.class), valeur.capture());
        assertThat(valeur.getValue()).isEqualTo(Saison.ETE);
    }

    private enum Saison { ETE, HIVER }

    // ------------------------------------------------------------------ le filtre qui ne porte pas

    @Test
    @DisplayName("Une sélection dont aucune valeur n'est exploitable ne rend rien, et non pas tout")
    void selectionSansValeurUtilisable_neRendRien() {
        when(chemin.getJavaType()).thenReturn((Class) UUID.class);

        // L'utilisateur a filtré ; lui rendre la liste entière lui ferait croire que le filtre a
        // porté, et la réponse paraîtrait valide.
        appliquer(avec(filtre("niveauNonConformiteId", FilterOperator.IN,
                List.of("pas-un-identifiant"))));

        verify(cb).disjunction();
    }

    // ------------------------------------------------------------------ le périmètre en OU

    @Test
    @DisplayName("Une même comparaison peut valoir sur l'une ou l'autre de plusieurs colonnes")
    void plusieursColonnes_enDisjonction() {
        when(chemin.getJavaType()).thenReturn((Class) String.class);

        // « Mes dossiers » n'est pas une colonne : ce sont ceux que j'ai déclarés ou ceux qui me
        // sont imputés. Des critères cumulés ne savent pas le dire — ils se combinent par un ET —
        // et chaque écran devait donc s'appuyer sur un point d'entrée écrit pour lui.
        appliquer(avec(FilterExtra.builder()
                .fields(List.of("createdById", "userImputId"))
                .operator(FilterOperator.EQ)
                .value("utilisateur-1")
                .build()));

        // Les deux colonnes sont interrogées avec la même valeur, et réunies par un OU — non
        // cumulées par un ET. Le tableau explicite désigne la variante variadique de « or », que
        // deux arguments nus feraient prendre pour la surcharge à deux expressions.
        verify(root).get("createdById");
        verify(root).get("userImputId");
        verify(cb, org.mockito.Mockito.times(2)).equal(any(Expression.class), any(Object.class));
        verify(cb).or(new Predicate[]{clause, clause});
    }

    @Test
    @DisplayName("Les colonnes alternatives priment sur la colonne unique, elles ne s'y ajoutent pas")
    void plusieursColonnes_remplacentLaColonneUnique() {
        when(chemin.getJavaType()).thenReturn((Class) String.class);

        appliquer(avec(FilterExtra.builder()
                .field("ignore")
                .fields(List.of("createdById"))
                .operator(FilterOperator.EQ)
                .value("utilisateur-1")
                .build()));

        verify(root).get("createdById");
        verify(root, never()).get("ignore");
    }

    // ------------------------------------------------------------------ critère inexploitable

    @Test
    @DisplayName("Un critère qui ne désigne aucune donnée est refusé à l'appelant, pas au serveur")
    void champInconnu_refuseEnQuatreCent() {
        when(root.get("colonneInconnue")).thenThrow(new IllegalArgumentException("Unable to locate Attribute"));

        // Laissée telle quelle, l'exception était traduite par la couche de persistance en
        // InvalidDataAccessApiUsageException et repartait en 500 : une faute de nom de colonne
        // passait pour une panne du serveur, et l'écran n'avait aucun moyen d'apprendre laquelle.
        assertThatThrownBy(() -> appliquer(avec(filtre("colonneInconnue", FilterOperator.EQ, "x"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("colonneInconnue")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ chemins et texte libre

    @Test
    @DisplayName("Un critère peut désigner un attribut traversé par une relation")
    void cheminImbrique_traverse() {
        when(chemin.getJavaType()).thenReturn((Class) String.class);

        appliquer(avec(filtre("structure.libelle", FilterOperator.EQ, "DSI")));

        verify(root).get("structure");
        verify(chemin).get("libelle");
    }

    @Test
    @DisplayName("Le texte libre cherche par préfixe, sur les champs que la ressource déclare")
    void texteLibre_parPrefixe() {
        appliquer(CriteriaDto.builder().search("NC-2026").build());

        ArgumentCaptor<String> motif = ArgumentCaptor.forClass(String.class);
        verify(cb).like(any(), motif.capture());
        // Un « contient » impose un parcours complet de la table : la recherche s'effondre à mesure
        // que le référentiel grandit, au moment précis où elle devient utile.
        assertThat(motif.getValue()).isEqualTo("nc-2026%");
    }

    @Test
    @DisplayName("Le texte libre reste sans effet sur une ressource qui ne déclare aucun champ")
    void texteLibre_sansChampDeclare() {
        specification.query(CriteriaDto.builder().search("NC-2026").build(), List.of())
                .toPredicate(root, requete, cb);

        verify(cb, never()).like(any(), anyString());
    }
}
