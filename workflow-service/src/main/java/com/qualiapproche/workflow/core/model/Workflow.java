package com.qualiapproche.workflow.core.model;

import lombok.Getter;
import lombok.Setter;
import com.qualiapproche.workflow.core.interfaces.IData;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Graphe d'etats et de transitions.
 *
 * <p>Le parametre {@code T} — absent de l'implementation historique — permet aux
 * appelants de recuperer directement leur type concret de transition, sans le
 * transtypage non verifie que pratiquait le moteur.</p>
 *
 * @param <D> type de la donnee pilotee
 * @param <T> type concret des transitions du workflow
 */
@Getter
@Setter
public class Workflow<D extends IData, T extends Transition<D>> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final LinkedHashMap<String, Etat> etats = new LinkedHashMap<>();
    private final LinkedHashMap<Etat, SequencedSet<T>> etatTransitions = new LinkedHashMap<>();
    private String code;
    private String libelle;
    private String description;
    private Etat etatInitial;

    /**
     * Construit un workflow vide.
     */
    public Workflow() {
        // constructeur par defaut requis par les couches de mapping
    }

    /**
     * Construit un workflow identifie par son code.
     *
     * @param pCode code du workflow
     */
    public Workflow(final String pCode) {
        this.code = pCode;
    }

    /**
     * Ajoute un etat au workflow.
     *
     * @param pEtat etat a ajouter
     */
    public void addEtat(final Etat pEtat) {
        Objects.requireNonNull(pEtat, "L'etat ne peut etre null.");
        Objects.requireNonNull(pEtat.getCode(), "Le code de l'etat ne peut etre null.");
        this.etats.put(pEtat.getCode(), pEtat);
    }

    /**
     * Recupere un etat par son code.
     *
     * @param pCode code de l'etat recherche
     * @return l'etat correspondant, ou null si le code est inconnu
     */
    public Etat getEtat(final String pCode) {
        return this.etats.get(pCode);
    }

    /**
     * Etats du workflow, dans leur ordre de declaration.
     *
     * @return une vue non modifiable des etats
     */
    public SequencedSet<Etat> getEtats() {
        return Collections.unmodifiableSequencedSet(new LinkedHashSet<>(this.etats.values()));
    }

    /**
     * Ajoute une transition, indexee par son etat d'origine.
     *
     * @param pTransition transition a ajouter
     */
    public void addTransition(final T pTransition) {
        Objects.requireNonNull(pTransition, "La transition ne peut etre null.");
        Objects.requireNonNull(pTransition.getEtatOrigine(),
                "La transition " + pTransition.getCode() + " n'a pas d'etat d'origine.");
        this.etatTransitions
                .computeIfAbsent(pTransition.getEtatOrigine(), pEtat -> new LinkedHashSet<>())
                .add(pTransition);
    }

    /**
     * Ajoute un ensemble de transitions.
     *
     * @param pTransitions transitions a ajouter
     */
    public void addTransitions(final Collection<? extends T> pTransitions) {
        Objects.requireNonNull(pTransitions, "La collection de transitions ne peut etre null.");
        pTransitions.forEach(this::addTransition);
    }

    /**
     * Transitions sortantes d'un etat. Renvoie un ensemble vide — jamais {@code null} —
     * si l'etat est inconnu ou terminal.
     *
     * @param pEtat etat d'origine
     * @return une vue non modifiable des transitions sortantes
     */
    public SequencedSet<T> getTransitionsFromEtat(final Etat pEtat) {
        SequencedSet<T> aTransitions = this.etatTransitions.get(pEtat);
        return aTransitions == null
                ? Collections.unmodifiableSequencedSet(new LinkedHashSet<>())
                : Collections.unmodifiableSequencedSet(aTransitions);
    }

    /**
     * Toutes les transitions du workflow, groupees par etat d'origine.
     *
     * @return une vue non modifiable de toutes les transitions
     */
    public SequencedSet<T> getTransitions() {
        return this.etatTransitions.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        Collections::unmodifiableSequencedSet));
    }

    /**
     * Definit l'etat initial du workflow.
     *
     * @param pEtatInitial etat initial, qui doit avoir ete ajoute au prealable
     * @throws IllegalArgumentException si l'etat n'a pas ete ajoute au prealable
     */
    public void setEtatInitial(final Etat pEtatInitial) {
        Objects.requireNonNull(pEtatInitial, "L'etat initial ne peut etre null.");
        if (!this.etats.containsValue(pEtatInitial)) {
            throw new IllegalArgumentException(
                    "L'etat initial " + pEtatInitial.getCode() + " n'appartient pas au workflow "
                            + this.code + ".");
        }
        this.etatInitial = pEtatInitial;
    }

    /**
     * Empreinte calculee sur le seul code.
     *
     * @return l'empreinte du workflow
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(this.code);
    }

    /**
     * Egalite par code, tolerante aux sous-classes.
     *
     * @param pObj objet compare
     * @return vrai si l'objet compare est un workflow de meme code
     */
    @Override
    public boolean equals(final Object pObj) {
        return this == pObj
                || pObj instanceof Workflow<?, ?> aAutre && Objects.equals(this.code, aAutre.code);
    }

    /**
     * Representation lisible du workflow.
     *
     * @return le code, le libelle, le nombre d'etats et l'etat initial
     */
    @Override
    public String toString() {
        return "Workflow [code=%s, libelle=%s, etats=%d, etatInitial=%s]"
                .formatted(this.code, this.libelle, this.etats.size(), this.etatInitial);
    }

    /**
     * Nombre total de transitions, tous etats confondus.
     *
     * @return le nombre total de transitions
     */
    public int nombreTransitions() {
        return this.etatTransitions.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Etats sans transition sortante, dans leur ordre de declaration.
     *
     * @return une vue non modifiable des etats terminaux
     */
    public SequencedSet<Etat> getEtatsTerminaux() {
        return this.etats.values().stream()
                .filter(pEtat -> this.etatTransitions.getOrDefault(pEtat, new LinkedHashSet<>()).isEmpty())
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        Collections::unmodifiableSequencedSet));
    }
}
