package com.qualiapproche.common.serie;

import com.qualiapproche.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Attribue les numéros d'un service, d'après le patron des séries de sigatt.
 *
 * <p>L'attribution se fait <b>dans la transaction métier</b> de l'appelant : la série est
 * verrouillée jusqu'à son terme, si bien que deux créations simultanées passent l'une après
 * l'autre ; si la création échoue, la série revient en arrière avec elle — aucun numéro perdu,
 * aucun trou. Hors transaction, l'attribution est refusée : le verrou n'y tiendrait pas.</p>
 *
 * <p>Un numéro déjà porté par un document (saisi à la main, importé, laissé par l'ancien calcul)
 * est sauté plutôt que dupliqué, dans une limite de tentatives. Une série épuisée est refusée,
 * jamais débordée.</p>
 *
 * @param <E> l'entité de série du service
 */
public class AttributionDeNumero<E extends AbstractSerieEntity> {

    /** Au-delà, trop de numéros pris d'affilée : mieux vaut caler la série que boucler. */
    public static final int TENTATIVES_MAX = 50;

    private final SerieRepository<E> series;

    public AttributionDeNumero(SerieRepository<E> series) {
        this.series = series;
    }

    /**
     * Le prochain numéro de la série que désigne la stratégie.
     *
     * @param dejaPris vrai si un document porte déjà ce numéro
     */
    public <C> String attribuer(StrategieDeSerie<C> strategie, C contexte, Predicate<String> dejaPris) {
        exigerUneTransaction();
        String cle = strategie.cle(contexte);
        E serie = series.verrouiller(cle).orElseGet(() -> {
            series.creerSiAbsente(cle, strategie.valeurInitiale(contexte));
            return series.verrouiller(cle).orElseThrow();
        });
        long rang = serie.getValeurCourante();
        for (int i = 0; i < TENTATIVES_MAX; i++) {
            rang++;
            if (rang > strategie.rangMaximal()) {
                throw new BusinessException("La série « " + cle + " » est épuisée (" + strategie.rangMaximal()
                        + " numéros) : aucun numéro ne peut plus y être attribué.", HttpStatus.CONFLICT);
            }
            String numero = strategie.formater(contexte, rang);
            if (!dejaPris.test(numero)) {
                serie.setValeurCourante(rang);
                series.save(serie);
                return numero;
            }
        }
        throw new BusinessException("La série « " + cle + " » bute sur " + TENTATIVES_MAX
                + " numéros déjà pris d'affilée : calez-la au-delà des numéros existants.", HttpStatus.CONFLICT);
    }

    /**
     * Cale une série sur une valeur — reprise d'un existant, saut volontaire. Jamais en arrière :
     * un recul rattribuerait des numéros déjà émis.
     */
    public E caler(String cle, long valeur, Function<String, E> fabrique) {
        exigerUneTransaction();
        E serie = series.verrouiller(cle).orElseGet(() -> {
            E neuve = fabrique.apply(cle);
            neuve.setCleSerie(cle);
            neuve.setValeurCourante(0);
            return neuve;
        });
        if (valeur < serie.getValeurCourante()) {
            throw new BusinessException("Une série ne recule pas : « " + cle + " » est à "
                    + serie.getValeurCourante() + ", la caler à " + valeur + " rattribuerait des numéros déjà émis.",
                    HttpStatus.CONFLICT);
        }
        serie.setValeurCourante(valeur);
        return series.save(serie);
    }

    public List<E> lister() {
        return series.findAll();
    }

    private static void exigerUneTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Une attribution de numéro exige la transaction de la création : "
                    + "hors transaction, le verrou de la série ne tiendrait pas.");
        }
    }
}
