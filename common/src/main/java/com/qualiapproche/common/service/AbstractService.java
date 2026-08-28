package com.qualiapproche.common.service;

import com.qualiapproche.common.api.CriteriaDto;
import com.qualiapproche.common.spec.GenericSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/**
 * Socle des services de ressource : ce qu'ils font tous, écrit une fois.
 *
 * <p>Un module en dérive une classe et ne déclare que ce qui lui est propre — d'où viennent ses
 * entités, comment elles deviennent des objets de transfert, ce que l'appelant a le droit de voir.
 * Le reste est ici, et les opérations communes s'y ajouteront au fil du temps.</p>
 *
 * <p>La première est la <b>recherche</b> filtrée et paginée. Les critères viennent de l'écran et
 * n'ont pas à être prévus colonne par colonne : {@link com.qualiapproche.common.spec.GenericSpecification}
 * les traduit quels qu'ils soient.</p>
 *
 * <p><b>La borne de visibilité n'est pas un critère.</b> Elle est combinée <b>en dehors</b> de ce
 * que l'appelant envoie, par un ET : aucun filtre reçu ne peut donc l'élargir. C'est la différence
 * entre restreindre une liste et sécuriser une ressource — la première se fait dans l'écran, la
 * seconde ici, faute de quoi il suffirait de chercher pour lire ce que consulter refuse.</p>
 *
 * @param <E> entité persistée
 * @param <D> objet de transfert rendu à l'appelant
 */
public abstract class AbstractService<E, D> implements GenericService<D> {

    private final GenericSpecification<E> criteres = new GenericSpecification<>();

    /** Dépôt de l'entité, qui doit savoir exécuter une spécification. */
    protected abstract JpaSpecificationExecutor<E> depot();

    /** Conversion d'une entité en ce que l'appelant reçoit. */
    protected abstract Function<E, D> versDto();

    /**
     * Attributs confrontés au texte libre de la recherche.
     *
     * <p>Vide par défaut : une ressource qui n'en déclare aucun ignore simplement le texte, plutôt
     * que d'en chercher la trace dans toutes ses colonnes.</p>
     */
    protected List<String> champsRecherchables() {
        return List.of();
    }

    /**
     * Ce que l'appelant a le droit de voir, indépendamment de ce qu'il demande.
     *
     * <p>Sans borne par défaut : une ressource dont la lecture n'est pas restreinte n'a rien à
     * déclarer. Une ressource qui l'est doit rendre ici sa clause — et non l'ajouter aux critères,
     * qui appartiennent à l'appelant.</p>
     */
    protected Specification<E> bornesDeVisibilite() {
        return (root, query, cb) -> cb.conjunction();
    }

    /**
     * Page de ressources retenues par les critères, dans les bornes de visibilité de l'appelant.
     *
     * <p><b>La transaction est déclarée ici, et elle est indispensable.</b> La conversion en objets
     * de transfert lit des colonnes que le fournisseur de persistance ne matérialise qu'à
     * l'intérieur d'une session — les {@code @Lob} d'une non-conformité, les collections
     * différées. Sans transaction, chaque appel au dépôt ouvre puis referme la sienne, les entités
     * rendues sont détachées, et la conversion échoue sur « Unable to access lob stream ».</p>
     *
     * <p>Elle ne peut pas être héritée d'un {@code @Transactional} posé sur la classe du service
     * concret : Spring résout l'attribut de transaction sur la méthode la plus spécifique, qui est
     * <b>celle-ci</b>, déclarée ici. L'annotation du service dérivé ne s'y applique donc pas, et le
     * défaut ne se voit qu'à l'exécution, sur une ressource qui porte de telles colonnes.</p>
     *
     * @param criteresRecus critères de l'appelant ; {@code null} vaut « aucun filtre »
     */
    @Override
    @Transactional(readOnly = true)
    public Page<D> rechercher(CriteriaDto criteresRecus, Pageable pageable) {
        Specification<E> requete = Specification.allOf(
                criteres.query(criteresRecus, champsRecherchables()),
                bornesDeVisibilite());
        return depot().findAll(requete, pageable).map(versDto());
    }
}
