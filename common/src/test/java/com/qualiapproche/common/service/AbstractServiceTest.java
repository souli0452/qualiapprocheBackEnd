package com.qualiapproche.common.service;

import com.qualiapproche.common.api.CriteriaDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que la recherche générique doit déclarer, et qui ne se voit qu'à l'exécution.
 *
 * <p>La conversion des entités en objets de transfert lit des colonnes que le fournisseur de
 * persistance ne matérialise qu'à l'intérieur d'une session — les {@code @Lob} d'une
 * non-conformité, les collections différées. Sans transaction sur cette méthode, chaque appel au
 * dépôt ouvre puis referme la sienne, les entités rendues sont détachées, et la conversion échoue
 * en {@code « Unable to access lob stream »} — en erreur serveur, à la première recherche, sur une
 * ressource qui porte de telles colonnes.</p>
 *
 * <p>Et l'annotation ne peut pas être héritée : Spring résout l'attribut de transaction sur la
 * méthode la plus spécifique, qui est celle déclarée ici. Un {@code @Transactional} posé sur la
 * classe du service concret ne s'y applique pas — c'est précisément ce qui avait été supposé.</p>
 */
class AbstractServiceTest {

    @Test
    @DisplayName("La recherche est transactionnelle, et en lecture seule")
    void rechercher_estTransactionnelle() throws NoSuchMethodException {
        Method rechercher = AbstractService.class.getMethod("rechercher", CriteriaDto.class, Pageable.class);

        Transactional transaction = rechercher.getAnnotation(Transactional.class);
        assertThat(transaction)
                .as("sans transaction, la conversion des entités échoue sur leurs colonnes différées")
                .isNotNull();
        assertThat(transaction.readOnly())
                .as("une recherche ne modifie rien : la lecture seule le dit, et l'optimise")
                .isTrue();
    }
}
