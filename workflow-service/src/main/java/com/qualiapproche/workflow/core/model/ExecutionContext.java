package com.qualiapproche.workflow.core.model;

import lombok.Getter;
import lombok.Setter;
import com.qualiapproche.workflow.core.interfaces.IData;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Porte la donnee soumise au workflow et les parametres libres de l'appel.
 *
 * <p>La map de parametres est le point d'extension prevu pour tout ce qui est propre a
 * l'application hote (utilisateur courant, motif, pieces jointes) sans que le moteur
 * n'ait a connaitre ces types.</p>
 *
 * @param <D> type de la donnee pilotee
 */
public class ExecutionContext<D extends IData> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private D data;

    // Aucune annotation : la map n'est jamais exposee telle quelle. Elle est pilotee par
    // l'API de parametres ci-dessous, et seule une vue non modifiable en sort.
    // Type concret et non l'interface Map : le champ appartient a une classe serialisable
    // et doit donc etre declare avec un type qui l'est.
    private HashMap<String, Object> parametres = new HashMap<>();

    /**
     * Construit un contexte d'execution vide.
     */
    public ExecutionContext() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit un contexte d'execution sur une donnee.
     *
     * @param pData donnee pilotee
     */
    public ExecutionContext(final D pData) {
        this.data = pData;
    }

    /**
     * Construit un contexte sur une map de parametres existante, partagee et non copiee.
     *
     * <p>Visibilite de paquetage : sert a {@link ActionExecutionContext} pour deriver un
     * contexte d'action sans perdre le lien avec les parametres de l'appelant. Passer par
     * un constructeur plutot que par un setter evite toute invocation de methode sur
     * {@code this} pendant la construction de la sous-classe.</p>
     *
     * @param pData       donnee pilotee
     * @param pParametres map de parametres partagee, une map vide est creee si elle est null
     */
    ExecutionContext(final D pData, final HashMap<String, Object> pParametres) {
        this.data = pData;
        this.parametres = pParametres == null ? new HashMap<>() : pParametres;
    }

    /**
     * Verifie la coherence minimale du contexte avant execution.
     */
    public void controle() {
        if (this.data == null) {
            throw new IllegalArgumentException("La donnee ne peut etre null.");
        }
    }

    /**
     * Recupere la valeur brute d'un parametre.
     *
     * @param pCle cle du parametre
     * @return la valeur associee, ou null si la cle est absente
     */
    public Object getParametre(final String pCle) {
        return this.parametres.get(pCle);
    }

    /**
     * Recupere un parametre en le transtypant, sans lever d'exception s'il est absent
     * ou d'un autre type.
     *
     * @param <V>   type attendu de la valeur
     * @param pCle  cle du parametre
     * @param pType classe du type attendu
     * @return la valeur transtypee, ou un optionnel vide si absente ou d'un autre type
     */
    public <V> Optional<V> getParametre(final String pCle, final Class<V> pType) {
        Object aValeur = this.parametres.get(pCle);
        return pType.isInstance(aValeur) ? Optional.of(pType.cast(aValeur)) : Optional.empty();
    }

    /**
     * Enregistre un parametre, en ecrasant la valeur eventuellement presente.
     *
     * @param pCle    cle du parametre
     * @param pValeur valeur a enregistrer
     * @return l'ancienne valeur associee, ou null si la cle etait absente
     */
    public Object putParametre(final String pCle, final Object pValeur) {
        return this.parametres.put(pCle, pValeur);
    }

    /**
     * Enregistre un parametre uniquement si la cle est absente.
     *
     * @param pCle    cle du parametre
     * @param pValeur valeur a enregistrer
     * @return la valeur deja presente, ou null si l'enregistrement a eu lieu
     */
    public Object putParametreIfAbsent(final String pCle, final Object pValeur) {
        return this.parametres.putIfAbsent(pCle, pValeur);
    }

    /**
     * Enregistre l'ensemble des parametres fournis.
     *
     * @param pParametres parametres a ajouter
     */
    public void putAllParametres(final Map<String, ?> pParametres) {
        this.parametres.putAll(pParametres);
    }

    /**
     * Retire un parametre.
     *
     * @param pCle cle du parametre
     * @return la valeur retiree, ou null si la cle etait absente
     */
    public Object removeParametre(final String pCle) {
        return this.parametres.remove(pCle);
    }

    /**
     * Retire un parametre uniquement s'il est associe a la valeur attendue.
     *
     * @param pCle    cle du parametre
     * @param pValeur valeur attendue
     * @return vrai si le parametre a ete retire
     */
    public boolean removeParametre(final String pCle, final Object pValeur) {
        return this.parametres.remove(pCle, pValeur);
    }

    /**
     * Remplace la valeur d'un parametre existant.
     *
     * @param pCle    cle du parametre
     * @param pValeur nouvelle valeur
     * @return l'ancienne valeur associee, ou null si la cle etait absente
     */
    public Object replaceParametre(final String pCle, final Object pValeur) {
        return this.parametres.replace(pCle, pValeur);
    }

    /**
     * Remplace la valeur d'un parametre uniquement si l'ancienne valeur correspond.
     *
     * @param pCle      cle du parametre
     * @param pAncienne valeur attendue
     * @param pNouvelle nouvelle valeur
     * @return vrai si le remplacement a eu lieu
     */
    public boolean replaceParametre(final String pCle, final Object pAncienne, final Object pNouvelle) {
        return this.parametres.replace(pCle, pAncienne, pNouvelle);
    }

    /**
     * Indique si une cle de parametre est presente.
     *
     * @param pCle cle recherchee
     * @return vrai si la cle est presente
     */
    public boolean containsParametreKey(final String pCle) {
        return this.parametres.containsKey(pCle);
    }

    /**
     * Indique si une valeur de parametre est presente.
     *
     * @param pValeur valeur recherchee
     * @return vrai si la valeur est presente
     */
    public boolean containsParametreValue(final Object pValeur) {
        return this.parametres.containsValue(pValeur);
    }

    /**
     * Applique une action a chaque parametre.
     *
     * @param pAction action appliquee a chaque couple cle / valeur
     */
    public void forEachParametre(final BiConsumer<? super String, ? super Object> pAction) {
        this.parametres.forEach(pAction);
    }

    /**
     * Vue non modifiable des parametres.
     *
     * @return une vue non modifiable de la map de parametres
     */
    public Map<String, Object> getParametres() {
        return Collections.unmodifiableMap(this.parametres);
    }

    /**
     * Map de parametres sous-jacente, partagee et non copiee.
     *
     * <p>Reduite a la visibilite de paquetage : son seul appelant legitime est
     * {@link ActionExecutionContext}, qui partage les parametres du contexte source.
     * Exposee en {@code protected}, elle laissait toute sous-classe externe rompre
     * l'immuabilite promise par {@link #getParametres()}.</p>
     *
     * @return la map de parametres sous-jacente
     */
    HashMap<String, Object> getParametresInternes() {
        return this.parametres;
    }

    /**
     * Representation lisible du contexte d'execution.
     *
     * @return la donnee et les parametres du contexte
     */
    @Override
    public String toString() {
        return "ExecutionContext [data=" + this.data + ", parametres=" + this.parametres + "]";
    }
}
