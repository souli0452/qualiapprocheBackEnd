package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.spec.Filtres;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.Retirable;
import com.qualiapproche.referentiel.repository.ParametrageParDirectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Le socle des paramétrages propres à chaque direction : sites, critères et niveaux d'évaluation
 * des auditeurs.
 *
 * <p>À la différence des autres référentiels de ce service, communs à toute l'installation,
 * ceux-ci sont bornés à la direction de l'appelant : chaque organisation a ses sites et son
 * échelle. Celle d'une autre direction est déclarée introuvable, et non refusée — « elle existe
 * mais n'est pas à vous » en dirait déjà trop.</p>
 *
 * <p>Supprimer retire : voir {@link Retirable}. Seuls les champs que {@link #recopier} nomme
 * passent de la saisie à la base ; une direction ou un identifiant glissé dans le corps est
 * ignoré.</p>
 */
public abstract class ParametrageParDirection<E extends AuditEntity & Retirable, D> {

    private final ParametrageParDirectionRepository<E> depot;
    private final Function<E, D> versDto;

    protected ParametrageParDirection(ParametrageParDirectionRepository<E> depot, Function<E, D> versDto) {
        this.depot = depot;
        this.versDto = versDto;
    }

    /** Le nom de l'entrée, dans les messages : « site ». */
    protected abstract String nom();

    protected abstract E nouveau();

    /** Recopie de la saisie vers l'entité les seuls champs modifiables. */
    protected abstract void recopier(D saisie, E cible);

    /** Les champs où la recherche d'un écran cherche son texte ; le libellé par défaut. */
    protected List<String> champsRecherche() {
        return List.of("libelle");
    }

    /** L'ordre des listes ; de la plus ancienne à la plus récente par défaut. */
    protected Sort ordre() {
        return Sort.by("createdAt");
    }

    /**
     * La liste de la direction.
     *
     * @param inactifs avec les entrées retirées : ce qu'il faut pour afficher un dossier qui en
     *                 cite une, et non pour proposer un choix
     */
    @Transactional(readOnly = true)
    public List<D> lister(boolean inactifs) {
        UUID directionId = exigerUneDirection();
        List<E> entrees = inactifs ? depot.findAllByDirectionId(directionId, ordre())
                : depot.findAllByDirectionIdAndActifTrue(directionId, ordre());
        return entrees.stream().map(versDto).toList();
    }

    /**
     * La liste d'un écran : une page, filtrée par le texte cherché.
     *
     * @param inactifs avec les entrées retirées, pour les montrer et les rétablir
     */
    @Transactional(readOnly = true)
    public Page<D> page(boolean inactifs, String recherche, Pageable pageable) {
        UUID directionId = exigerUneDirection();
        return depot.findAll(Filtres.tous(
                        Filtres.deLaDirection(directionId),
                        inactifs ? null : Filtres.egal("actif", true),
                        Filtres.contient(champsRecherche(), recherche)),
                Filtres.ordonnee(pageable, ordre())).map(versDto);
    }

    @Transactional(readOnly = true)
    public D lire(UUID id) {
        return versDto.apply(sienneOuRien(id));
    }

    @Transactional
    public D creer(D saisie) {
        exigerUneDirection();
        E entree = nouveau();
        recopier(saisie, entree);
        return versDto.apply(depot.save(entree));
    }

    @Transactional
    public D modifier(UUID id, D saisie) {
        E entree = sienneOuRien(id);
        recopier(saisie, entree);
        return versDto.apply(depot.save(entree));
    }

    /** Retire l'entrée des choix ; les dossiers qui la citent la lisent toujours. */
    @Transactional
    public void retirer(UUID id) {
        E entree = sienneOuRien(id);
        entree.setActif(false);
        depot.save(entree);
    }

    protected static String exigerTexte(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new BusinessException("Le champ « " + champ + " » est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        return valeur.strip();
    }

    private E sienneOuRien(UUID id) {
        UUID directionId = exigerUneDirection();
        return depot.findById(id)
                .filter(e -> directionId.equals(e.getDirectionId()))
                .orElseThrow(() -> new BusinessException(
                        "Aucun(e) " + nom() + " ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    private static UUID exigerUneDirection() {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        if (directionId == null) {
            throw new BusinessException("Votre compte n'est rattaché à aucune direction : ce paramétrage"
                    + " ne peut pas être géré depuis ce compte.", HttpStatus.FORBIDDEN);
        }
        return directionId;
    }
}
