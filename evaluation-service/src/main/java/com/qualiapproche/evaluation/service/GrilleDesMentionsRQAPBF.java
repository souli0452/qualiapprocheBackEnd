package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.MentionRQAPBFDto;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.TrancheScoreRQAPBF;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.repository.TrancheScoreRQAPBFRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La grille qui traduit le score global RQAP-BF en niveau, mention et étoiles (voir
 * {@link TrancheScoreRQAPBF}), et les deux garde-fous qui la tempèrent.
 *
 * <p><b>Plancher par domaine.</b> Un domaine sous {@link #SEUIL_DOMAINE} % plafonne la mention à la
 * tranche où tombe ce seuil : une moyenne flatteuse ne doit pas cacher un domaine défaillant.</p>
 *
 * <p><b>Réserve.</b> Tant que l'évaluation relève des écarts, la mention est délivrée « sous réserve
 * de leur levée » : leur traitement se mène au module Non-conformités.</p>
 *
 * <p>La grille par défaut tient lieu de grille officielle, attendue (recette du 17 oct. 2026,
 * observation 28) ; seule sa tranche [50–70[ est connue. Les tranches d'une direction se posent à
 * sa première lecture ; tant qu'elles n'existent pas, la grille par défaut vaut.</p>
 */
@Service
public class GrilleDesMentionsRQAPBF extends ParametrageSimpleService<TrancheScoreRQAPBF> {

    /** Sous ce score, un domaine plafonne la mention. */
    static final double SEUIL_DOMAINE = 50;

    private final TrancheScoreRQAPBFRepository tranches;
    private final RegleEcartRQAPBFService regles;

    public GrilleDesMentionsRQAPBF(TrancheScoreRQAPBFRepository tranches, RegleEcartRQAPBFService regles) {
        super(tranches);
        this.tranches = tranches;
        this.regles = regles;
    }

    /** La grille par défaut, de la plus basse à la plus haute. */
    static List<TrancheScoreRQAPBF> grilleParDefaut() {
        return List.of(
                tranche(1, 0, "Niveau insuffisant", "Non certifiable — démarche qualité à engager", 0),
                tranche(2, 30, "Niveau initial", "Attestation d'engagement dans la démarche qualité", 1),
                tranche(3, 50, "Niveau intermédiaire", "Certificat de conformité moyenne", 2),
                tranche(4, 70, "Niveau avancé", "Certificat de conformité", 3),
                tranche(5, 90, "Niveau d'excellence", "Certificat de conformité avec mention d'excellence", 4));
    }

    private static TrancheScoreRQAPBF tranche(int rang, double min, String niveau, String mention, int etoiles) {
        return TrancheScoreRQAPBF.builder().rang(rang).scoreMin(min).niveau(niveau).mention(mention)
                .etoiles(etoiles).build();
    }

    /** La grille d'une direction, sans rien écrire : la grille par défaut tant qu'elle n'est pas posée. */
    @Transactional(readOnly = true)
    public List<TrancheScoreRQAPBF> grille(UUID directionId) {
        List<TrancheScoreRQAPBF> posees = directionId == null ? List.of()
                : tranches.findAllByDirectionIdOrderByCreatedAtAsc(directionId);
        List<TrancheScoreRQAPBF> grille = new ArrayList<>();
        for (TrancheScoreRQAPBF defaut : grilleParDefaut()) {
            grille.add(posees.stream().filter(t -> t.getRang() == defaut.getRang()).findFirst().orElse(defaut));
        }
        return grille;
    }

    /**
     * La mention d'une évaluation : officielle — sur les seuls critères publiés, comme le rapport —
     * ou provisoire, sur tout ce qui est noté.
     */
    @Transactional(readOnly = true)
    public MentionRQAPBFDto mentionDe(EvaluationRQAPBF evaluation, boolean officielle) {
        Set<NiveauNotationRQAPBF> enEcart = regles.niveauxEnEcart(evaluation.getDirectionId());
        int ecarts = (int) evaluation.getNotations().stream()
                .filter(n -> !officielle || n.estPublie())
                .filter(n -> n.constatRetenu(enEcart) == ConstatRQAPBF.NC).count();
        return mention(grille(evaluation.getDirectionId()),
                officielle ? evaluation.scoreOfficiel() : evaluation.calculerScoreProvisoire(),
                evaluation.genererResultatsParDomaine(officielle), ecarts);
    }

    /**
     * La mention d'un score : sa tranche, plafonnée si un domaine reste sous le seuil, et sous
     * réserve si des écarts sont relevés ; {@code null} sans score.
     */
    static MentionRQAPBFDto mention(List<TrancheScoreRQAPBF> grille, Double score, Map<String, Double> domaines,
                                    int ecarts) {
        if (score == null) {
            return null;
        }
        double arrondi = Math.round(score * 10) / 10.0;
        int retenue = indexDe(grille, arrondi);
        int plafond = indexDe(grille, SEUIL_DOMAINE);
        List<String> sousLeSeuil = domaines == null ? List.of() : domaines.entrySet().stream()
                .filter(d -> d.getValue() != null && d.getValue() < SEUIL_DOMAINE).map(Map.Entry::getKey).toList();
        boolean plafonnee = retenue > plafond && !sousLeSeuil.isEmpty();
        if (plafonnee) {
            retenue = plafond;
        }
        TrancheScoreRQAPBF t = grille.get(retenue);
        return MentionRQAPBFDto.builder()
                .rang(t.getRang())
                .niveau(t.getNiveau())
                .mention(t.getMention())
                .etoiles(t.getEtoiles())
                .scoreMin(t.getScoreMin())
                .scoreMax(retenue + 1 < grille.size() ? grille.get(retenue + 1).getScoreMin() : 100)
                .plafonnee(plafonnee)
                .domainesSousLeSeuil(plafonnee ? sousLeSeuil : null)
                .sousReserve(ecarts > 0)
                .ecarts(ecarts)
                .build();
    }

    private static int indexDe(List<TrancheScoreRQAPBF> grille, double score) {
        int index = 0;
        for (int i = 0; i < grille.size(); i++) {
            if (score >= grille.get(i).getScoreMin()) {
                index = i;
            }
        }
        return index;
    }

    @Override
    @Transactional
    public List<TrancheScoreRQAPBF> lister() {
        return assurerLaGrille(Perimetre.exigerUneDirection());
    }

    /** Cinq lignes : servies entières, de la plus basse à la plus haute. */
    @Override
    @Transactional
    public Page<TrancheScoreRQAPBF> page(String recherche, Pageable pageable) {
        List<TrancheScoreRQAPBF> toutes = assurerLaGrille(Perimetre.exigerUneDirection());
        return new PageImpl<>(toutes, Pageable.unpaged(), toutes.size());
    }

    @Override
    public TrancheScoreRQAPBF creer(TrancheScoreRQAPBF saisie) {
        throw grilleFixe();
    }

    @Override
    public void supprimer(UUID id) {
        throw grilleFixe();
    }

    @Override
    protected String nom() {
        return "tranche de score RQAP-BF";
    }

    @Override
    protected TrancheScoreRQAPBF nouveau() {
        return new TrancheScoreRQAPBF();
    }

    /** Bornes et libellés passent ; le rang d'une tranche ne change pas, la première reste à 0. */
    @Override
    protected void recopier(TrancheScoreRQAPBF saisie, TrancheScoreRQAPBF cible) {
        cible.setScoreMin(cible.getRang() == 1 ? 0 : saisie.getScoreMin());
        cible.setNiveau(exigerTexte(saisie.getNiveau(), "Niveau"));
        cible.setMention(exigerTexte(saisie.getMention(), "Mention"));
        cible.setEtoiles(saisie.getEtoiles());
    }

    /** Les bornes restent dans l'ordre des rangs : une tranche ne recouvre pas sa voisine. */
    @Override
    protected void verifier(TrancheScoreRQAPBF entree) {
        if (entree.getEtoiles() < 0 || entree.getEtoiles() > 5) {
            throw new BusinessException("Une tranche porte de 0 à 5 étoiles.", HttpStatus.BAD_REQUEST);
        }
        if (entree.getScoreMin() < 0 || entree.getScoreMin() > 100) {
            throw new BusinessException("Le score minimal d'une tranche est compris entre 0 et 100.",
                    HttpStatus.BAD_REQUEST);
        }
        for (TrancheScoreRQAPBF voisine : tranches.findAllByDirectionIdOrderByCreatedAtAsc(entree.getDirectionId())) {
            boolean desordre = voisine.getRang() < entree.getRang() && voisine.getScoreMin() >= entree.getScoreMin()
                    || voisine.getRang() > entree.getRang() && voisine.getScoreMin() <= entree.getScoreMin();
            if (desordre) {
                throw new BusinessException("Le score minimal doit rester entre celui de la tranche précédente et"
                        + " celui de la suivante (« " + voisine.getNiveau() + " » commence à "
                        + voisine.getScoreMin() + ").", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Override
    protected List<String> champsRecherche() {
        return List.of();
    }

    private List<TrancheScoreRQAPBF> assurerLaGrille(UUID directionId) {
        List<TrancheScoreRQAPBF> posees = new ArrayList<>(tranches.findAllByDirectionIdOrderByCreatedAtAsc(directionId));
        for (TrancheScoreRQAPBF defaut : grilleParDefaut()) {
            if (posees.stream().noneMatch(t -> t.getRang() == defaut.getRang())) {
                posees.add(tranches.save(defaut));
            }
        }
        posees.sort(Comparator.comparingInt(TrancheScoreRQAPBF::getRang));
        for (int i = 0; i < posees.size(); i++) {
            posees.get(i).setScoreMax(i + 1 < posees.size() ? posees.get(i + 1).getScoreMin() : 100.0);
        }
        return posees;
    }

    private static BusinessException grilleFixe() {
        return new BusinessException("La grille compte cinq tranches : elles ne se créent ni ne se suppriment."
                + " Seuls leurs bornes et libellés se modifient.", HttpStatus.METHOD_NOT_ALLOWED);
    }
}
