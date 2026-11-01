package com.qualiapproche.common.cloisonnement;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;

/**
 * Dernier rempart : refuse toute entité chargée hors de la direction de la session.
 *
 * <p>Hibernate filtre les requêtes sur la direction, mais pas le chargement par identifiant
 * ({@code em.find}, associations paresseuses) : sans cette garde, quiconque connaît l'identifiant
 * d'un dossier d'un autre client le lirait. Les dépôts passent déjà par une requête filtrée
 * ({@link DepotCloisonne}) ; ce qui y échapperait est arrêté ici, et déclaré introuvable plutôt
 * que refusé — « il existe mais n'est pas à vous » dirait déjà quelque chose de l'autre client.</p>
 */
public class GardeDeChargement implements PostLoadEventListener {

    @Override
    public void onPostLoad(PostLoadEvent event) {
        if (!(event.getEntity() instanceof Cloisonnable entite)) {
            return;
        }
        Object direction = event.getSession().getTenantIdentifierValue();
        if (direction == null || Cloisonnement.estLaRacine(direction)) {
            return;
        }
        if (!direction.equals(entite.getDirectionId())) {
            throw new EntityNotFoundException("Aucun(e) " + event.getEntity().getClass().getSimpleName()
                    + " ne porte l'identifiant " + event.getId());
        }
    }
}
