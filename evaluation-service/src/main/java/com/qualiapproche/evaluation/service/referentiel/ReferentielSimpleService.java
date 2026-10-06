package com.qualiapproche.evaluation.service.referentiel;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.repository.ReferentielRepository;
import com.qualiapproche.evaluation.service.Perimetre;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Le socle des référentiels paramétrables du module : types d'audit, sites, natures de constat...
 *
 * <p>Ils se ressemblent tous — une liste par direction, quelques champs, aucun cycle de vie — et
 * ne diffèrent que par ce qui se recopie de la saisie. C'est la seule chose que chaque
 * sous-classe dit. Le reste, et surtout le bornage à la direction, ne s'écrit qu'une fois.</p>
 *
 * <p>Seuls les champs que {@link #recopier} nomme passent de la saisie à la base : une direction
 * ou un identifiant glissé dans le corps de la requête est ignoré.</p>
 */
public abstract class ReferentielSimpleService<E extends AuditEntity> {

    private final ReferentielRepository<E> depot;

    protected ReferentielSimpleService(ReferentielRepository<E> depot) {
        this.depot = depot;
    }

    /** Le nom de l'entrée, dans les messages : « type d'audit ». */
    protected abstract String nom();

    protected abstract E nouveau();

    /** Recopie de la saisie vers l'entité les seuls champs modifiables. */
    protected abstract void recopier(E saisie, E cible);

    /** Vérifications propres au référentiel ; aucune par défaut. */
    protected void verifier(E entree) {
    }

    @Transactional(readOnly = true)
    public List<E> lister() {
        UUID directionId = Perimetre.exigerUneDirection();
        return depot.findAllByDirectionIdOrderByCreatedAtAsc(directionId);
    }

    @Transactional(readOnly = true)
    public E lire(UUID id) {
        return Perimetre.sienneOuRien(depot, id, nom());
    }

    @Transactional
    public E creer(E saisie) {
        Perimetre.exigerUneDirection();
        E entree = nouveau();
        recopier(saisie, entree);
        verifier(entree);
        return depot.save(entree);
    }

    @Transactional
    public E modifier(UUID id, E saisie) {
        E entree = lire(id);
        recopier(saisie, entree);
        verifier(entree);
        return depot.save(entree);
    }

    /**
     * Supprime l'entrée, ou dit pourquoi elle ne peut pas l'être.
     *
     * <p>Le {@code flush} est ici, et non au commit : c'est lui qui fait parler la base, et une
     * violation levée après la méthode échapperait au message — l'appelant recevrait un 500 au
     * lieu d'apprendre que l'entrée est encore employée.</p>
     */
    @Transactional
    public void supprimer(UUID id) {
        E entree = lire(id);
        try {
            depot.delete(entree);
            depot.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Ce(tte) " + nom() + " est encore employé(e) par un audit :"
                    + " il faut l'en retirer avant de le supprimer.", HttpStatus.CONFLICT);
        }
    }

    protected static String exigerTexte(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new BusinessException("Le champ « " + champ + " » est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        return valeur.strip();
    }
}
