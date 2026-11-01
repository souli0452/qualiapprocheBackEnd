package com.qualiapproche.common.cloisonnement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Base de tous les dépôts JPA : le chargement par identifiant passe par une requête, que
 * Hibernate filtre sur la direction, au lieu de {@code em.find}, qu'il ne filtre pas.
 *
 * <p>Un identifiant d'un autre client rend donc « vide », comme un identifiant inconnu.
 * {@code getReferenceById} renonce du même coup à la référence paresseuse : elle aurait différé la
 * lecture hors de tout contrôle.</p>
 */
public class DepotCloisonne<T, ID> extends SimpleJpaRepository<T, ID> {

    private final JpaEntityInformation<T, ?> information;
    private final EntityManager em;
    private final boolean cloisonne;

    public DepotCloisonne(JpaEntityInformation<T, ?> information, EntityManager em) {
        super(information, em);
        this.information = information;
        this.em = em;
        this.cloisonne = Cloisonnable.class.isAssignableFrom(information.getJavaType());
    }

    @Override
    public Optional<T> findById(ID id) {
        if (!cloisonne || id == null || estLaRacine()) {
            return super.findById(id);
        }
        String requete = "select e from " + information.getEntityName() + " e where e."
                + information.getIdAttribute().getName() + " = :id";
        List<T> trouves = em.createQuery(requete, information.getJavaType())
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();
        return trouves.stream().findFirst();
    }

    @Override
    public T getReferenceById(ID id) {
        if (!cloisonne) {
            return super.getReferenceById(id);
        }
        return findById(id).orElseThrow(() -> new EntityNotFoundException(
                "Aucun(e) " + information.getJavaType().getSimpleName() + " ne porte l'identifiant " + id));
    }

    @Override
    @Deprecated
    public T getById(ID id) {
        return getReferenceById(id);
    }

    @Override
    @Deprecated
    public T getOne(ID id) {
        return getReferenceById(id);
    }

    private boolean estLaRacine() {
        return Cloisonnement.estLaRacine(em.unwrap(Session.class).getTenantIdentifierValue());
    }
}
