package com.qualiapproche.amelioration.specification;

import com.qualiapproche.amelioration.entities.NonConformite;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce qui, dans la recherche des non-conformités, ne peut pas être générique : la borne de
 * visibilité.
 *
 * <p>Les critères de l'appelant sont traduits par {@code GenericSpecification}, vérifié de son
 * côté. Celle-ci ne lui appartient pas : elle est combinée en dehors de ce qu'il envoie, et
 * aucun filtre reçu ne peut l'élargir. Sans elle, il suffirait de chercher pour lire les dossiers
 * de toutes les structures — la restriction ne s'appliquant qu'à la liste générale.</p>
 */
class RechercheDesNonConformitesTest {

    private static final String STRUCTURE = "8f1d0c4a-0000-4000-8000-0000000000a1";

    @SuppressWarnings("unchecked")
    private final Root<NonConformite> root = mock(Root.class);
    private final CriteriaQuery<?> requete = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Predicate clause = mock(Predicate.class);

    @SuppressWarnings("unchecked")
    private final Path<Object> chemin = mock(Path.class);

    @BeforeEach
    void setUp() {
        when(root.get(anyString())).thenReturn(chemin);
        when(cb.equal(any(), any())).thenReturn(clause);
        when(cb.or(any(Predicate[].class))).thenReturn(clause);
        when(cb.disjunction()).thenReturn(clause);
    }

    private void appliquer(Specification<NonConformite> critere) {
        critere.toPredicate(root, requete, cb);
    }

    @Test
    @DisplayName("La visibilité couvre la structure du dossier et l'implication personnelle")
    void visibilite_structureEtPersonne() {
        appliquer(NonConformiteSpecification.visiblesPar(STRUCTURE, "utilisateur-1"));

        // Les quatre rattachements de la règle : émise par ma structure, adressée à ma structure,
        // déclarée par moi, imputée à moi. Un dossier déclaré depuis une structure quittée reste
        // ainsi visible à son auteur.
        verify(root).get("structureSoumissionId");
        verify(root).get("origineId");
        verify(root).get("createdById");
        verify(root).get("userImputId");
    }

    @Test
    @DisplayName("Un appelant que rien ne rattache ne voit rien — et non pas tout")
    void appelantInconnu_neVoitRien() {
        // Le repli inverse ouvrirait la table entière à un jeton sans structure : c'est le contraire
        // de ce que cette borne garantit.
        appliquer(NonConformiteSpecification.visiblesPar(null, null));

        verify(cb).disjunction();
        verify(cb, never()).or(any(Predicate[].class));
    }

    @Test
    @DisplayName("Une structure connue suffit, un utilisateur connu aussi")
    void unSeulRattachementSuffit() {
        appliquer(NonConformiteSpecification.visiblesPar(STRUCTURE, null));
        verify(root).get("structureSoumissionId");
        verify(root, never()).get("createdById");

        appliquer(NonConformiteSpecification.visiblesPar(null, "utilisateur-1"));
        verify(root).get("createdById");
    }

    @Test
    @DisplayName("Les colonnes de la borne sont des champs de l'entité")
    void colonnes_existentSurLEntite() {
        // Une spécification désigne ses colonnes par des chaînes : une faute de frappe compile, se
        // déploie, et n'échoue qu'à la première recherche — en erreur serveur, sans avertissement.
        List.of("structureSoumissionId", "origineId", "createdById", "userImputId")
                .forEach(colonne -> assertThat(existeSurLEntite(colonne))
                        .as("le champ « %s » doit exister sur NonConformite", colonne)
                        .isTrue());
    }

    private boolean existeSurLEntite(String champ) {
        for (Class<?> type = NonConformite.class; type != null; type = type.getSuperclass()) {
            for (Field declare : type.getDeclaredFields()) {
                if (declare.getName().equals(champ)) {
                    return true;
                }
            }
        }
        return false;
    }
}
