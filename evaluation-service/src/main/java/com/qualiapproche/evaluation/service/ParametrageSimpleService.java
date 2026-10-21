package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.spec.Filtres;
import com.qualiapproche.evaluation.repository.ParametrageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Le socle du paramétrage propre au module : types d'audit, natures de constat, règles de
 * notification, codification.
 *
 * <p>Ce qui se partage avec d'autres modules — les sites, l'échelle et les critères d'évaluation
 * des auditeurs — se paramètre dans referentiel-service, et se lit par {@code ParametrageClient}.
 * Ne restent ici que les réglages qui commandent une règle du module, et qu'une panne d'un autre
 * service ne doit pas suspendre.</p>
 *
 * <p>Ils se ressemblent tous — une liste par direction, quelques champs, aucun cycle de vie — et
 * ne diffèrent que par ce qui se recopie de la saisie. C'est la seule chose que chaque
 * sous-classe dit. Le reste, et surtout le bornage à la direction, ne s'écrit qu'une fois.</p>
 *
 * <p>Seuls les champs que {@link #recopier} nomme passent de la saisie à la base : une direction
 * ou un identifiant glissé dans le corps de la requête est ignoré.</p>
 */
public abstract class ParametrageSimpleService<E extends AuditEntity> {

    private final ParametrageRepository<E> depot;

    protected ParametrageSimpleService(ParametrageRepository<E> depot) {
        this.depot = depot;
    }

    /** Le nom de l'entrée, dans les messages : « type d'audit ». */
    protected abstract String nom();

    protected abstract E nouveau();

    /** Recopie de la saisie vers l'entité les seuls champs modifiables. */
    protected abstract void recopier(E saisie, E cible);

    /** Les champs où la recherche d'un écran cherche son texte ; le libellé par défaut. */
    protected List<String> champsRecherche() {
        return List.of("libelle");
    }

    /** Vérifications propres au référentiel ; aucune par défaut. */
    protected void verifier(E entree) {
    }

    @Transactional(readOnly = true)
    public List<E> lister() {
        UUID directionId = Perimetre.exigerUneDirection();
        return depot.findAllByDirectionIdOrderByCreatedAtAsc(directionId);
    }

    /** La liste d'un écran : une page, filtrée par le texte cherché, la plus ancienne en tête. */
    @Transactional(readOnly = true)
    public Page<E> page(String recherche, Pageable pageable) {
        UUID directionId = Perimetre.exigerUneDirection();
        return depot.findAll(Filtres.tous(
                        Filtres.deLaDirection(directionId),
                        Filtres.contient(champsRecherche(), recherche)),
                Filtres.ordonnee(pageable, Sort.by("createdAt")));
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
