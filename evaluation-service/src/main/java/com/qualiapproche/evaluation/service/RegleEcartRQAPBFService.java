package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.entities.RegleEcartRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.repository.RegleEcartRQAPBFRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * La règle « niveau RQAP-BF → non-conformité » d'une direction (voir {@link RegleEcartRQAPBF}).
 *
 * <p>Les niveaux sont ceux de la grille officielle : ils ne se créent ni ne se suppriment, seule
 * leur règle se modifie. Les trois lignes d'une direction se posent à sa première lecture, avec la
 * règle par défaut ; tant qu'elles n'existent pas, c'est cette règle qui vaut.</p>
 */
@Service
public class RegleEcartRQAPBFService extends ParametrageSimpleService<RegleEcartRQAPBF> {

    private final RegleEcartRQAPBFRepository regles;

    public RegleEcartRQAPBFService(RegleEcartRQAPBFRepository regles) {
        super(regles);
        this.regles = regles;
    }

    /**
     * Les niveaux qui, pour cette direction, ouvrent une non-conformité. Une lecture par appel :
     * l'appelant la fait une fois, puis l'applique à toutes ses notations.
     */
    @Transactional(readOnly = true)
    public Set<NiveauNotationRQAPBF> niveauxEnEcart(UUID directionId) {
        Set<NiveauNotationRQAPBF> niveaux = EnumSet.noneOf(NiveauNotationRQAPBF.class);
        List<RegleEcartRQAPBF> posees = directionId == null ? List.of()
                : regles.findAllByDirectionIdOrderByCreatedAtAsc(directionId);
        for (NiveauNotationRQAPBF niveau : NiveauNotationRQAPBF.parametrables()) {
            boolean ouvre = posees.stream().filter(r -> r.getNiveau() == niveau).findFirst()
                    .map(RegleEcartRQAPBF::isOuvreNonConformite)
                    .orElse(niveau.ouvreUneNonConformiteParDefaut());
            if (ouvre) {
                niveaux.add(niveau);
            }
        }
        return niveaux;
    }

    @Override
    @Transactional
    public List<RegleEcartRQAPBF> lister() {
        return assurerLesRegles(Perimetre.exigerUneDirection());
    }

    /** Trois lignes au plus : servies entières, dans l'ordre de la grille. */
    @Override
    @Transactional
    public Page<RegleEcartRQAPBF> page(String recherche, Pageable pageable) {
        List<RegleEcartRQAPBF> toutes = assurerLesRegles(Perimetre.exigerUneDirection());
        return new PageImpl<>(toutes, Pageable.unpaged(), toutes.size());
    }

    @Override
    public RegleEcartRQAPBF creer(RegleEcartRQAPBF saisie) {
        throw grilleFixe();
    }

    @Override
    public void supprimer(UUID id) {
        throw grilleFixe();
    }

    @Override
    protected String nom() {
        return "règle d'écart RQAP-BF";
    }

    @Override
    protected RegleEcartRQAPBF nouveau() {
        return new RegleEcartRQAPBF();
    }

    /** Seule la règle passe : le niveau d'une ligne ne change pas. */
    @Override
    protected void recopier(RegleEcartRQAPBF saisie, RegleEcartRQAPBF cible) {
        cible.setOuvreNonConformite(saisie.isOuvreNonConformite());
    }

    @Override
    protected List<String> champsRecherche() {
        return List.of();
    }

    private List<RegleEcartRQAPBF> assurerLesRegles(UUID directionId) {
        List<RegleEcartRQAPBF> posees = new ArrayList<>(regles.findAllByDirectionIdOrderByCreatedAtAsc(directionId));
        for (NiveauNotationRQAPBF niveau : NiveauNotationRQAPBF.parametrables()) {
            if (posees.stream().noneMatch(r -> r.getNiveau() == niveau)) {
                posees.add(regles.save(RegleEcartRQAPBF.builder()
                        .niveau(niveau)
                        .ouvreNonConformite(niveau.ouvreUneNonConformiteParDefaut())
                        .build()));
            }
        }
        posees.sort(Comparator.comparing(RegleEcartRQAPBF::getNiveau));
        return posees;
    }

    private static BusinessException grilleFixe() {
        return new BusinessException("Les niveaux sont ceux de la grille officielle RQAP-BF : ils ne se créent"
                + " ni ne se suppriment. Seule leur règle se modifie.", HttpStatus.METHOD_NOT_ALLOWED);
    }
}
