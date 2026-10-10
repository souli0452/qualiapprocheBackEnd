package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.spec.Filtres;
import com.qualiapproche.evaluation.dto.AvancementDto;
import com.qualiapproche.evaluation.dto.ComparaisonDto;
import com.qualiapproche.evaluation.dto.FiltreProgramme;
import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.dto.TableauDeBordDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Constat;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.entities.PlanAudit;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import com.qualiapproche.evaluation.enumeration.StatutPlanAudit;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.NoeudReferentielRepository;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
import com.qualiapproche.evaluation.repository.PlanAuditRepository;
import com.qualiapproche.evaluation.repository.RappelEcheanceRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Les vues de pilotage du module : tableau de bord (A1), suivi du programme (D16), avancement
 * d'un audit (C9), historique comparé (D17) et écarts RQAP-BF de toute la direction (D18).
 *
 * <p>Tout est calculé à la demande, depuis les données de l'audit : rien de ce qui s'affiche
 * ici n'est saisi, et un indicateur stocké finirait par mentir sur ce qu'il résume.</p>
 */
@Service
@RequiredArgsConstructor
public class PilotageAuditService {

    private final AuditRepository audits;
    private final AuditService auditService;
    private final EvaluationRQAPBFRepository evaluations;
    private final ConstatRepository constats;
    private final NotationCritereRepository notations;
    private final PlanAuditRepository plans;
    private final NoeudReferentielRepository noeuds;
    private final RappelEcheanceRepository rappels;

    @Transactional(readOnly = true)
    public TableauDeBordDto tableauDeBord(int annee) {
        UUID directionId = Perimetre.exigerUneDirection();
        LocalDate aujourdHui = LocalDate.now();
        List<Audit> deLAnnee = audits.findAll(SpecificationsAudit.du(directionId,
                        FiltreProgramme.builder().annee(annee).build(), null, aujourdHui)).stream()
                .filter(a -> a.getStatut() != StatutAudit.ANNULE)
                .toList();
        List<UUID> ids = deLAnnee.stream().map(Audit::getId).toList();

        long realises = deLAnnee.stream().filter(a -> a.getStatut() == StatutAudit.CLOTURE).count();
        long enRetard = deLAnnee.stream().filter(a -> a.estEnRetard(aujourdHui)).count();

        List<EvaluationRQAPBF> evaluees = ids.isEmpty() ? List.of() : evaluations.findAllByAuditIdIn(ids);
        long ecartsRqapbf = evaluees.stream().flatMap(e -> e.getNotations().stream())
                .filter(n -> n.estPublie() && n.constatRetenu() == ConstatRQAPBF.NC).count();
        long ecartsConstats = ids.isEmpty() ? 0 : constats.ecartsPublies(ids);

        Map<UUID, Double> scores = auditService.scoresDeMaturite(ids);

        return TableauDeBordDto.builder()
                .annee(annee)
                .auditsPlanifies(deLAnnee.size())
                .auditsRealises(realises)
                .tauxRealisation(deLAnnee.isEmpty() ? null : 100.0 * realises / deLAnnee.size())
                .auditsEnRetard(enRetard)
                .ecartsDetectes(ecartsConstats + ecartsRqapbf)
                .scoreMaturiteMoyen(moyenne(scores.values()))
                .radarParDomaine(moyenneParDomaine(evaluees.stream()
                        .filter(e -> e.getStatut() == StatutEvaluation.VALIDEE).toList()))
                .rappelsRecents(rappels.findTop10ByDirectionIdOrderByCreatedAtDesc(directionId).stream()
                        .map(r -> TableauDeBordDto.Rappel.builder()
                                .auditId(r.getAuditId())
                                .auditReference(r.getAuditReference())
                                .regle(r.getRegleLibelle())
                                .delaiJours(r.getDelaiJours())
                                .destinataires(r.getDestinataires())
                                .dateRappel(r.getDateRappel())
                                .build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public AvancementDto avancement(UUID auditId) {
        Audit audit = auditService.sienOuRien(auditId);
        PlanAudit plan = plans.findByAuditId(audit.getId()).orElse(null);

        Set<UUID> points = audit.getChecklists().stream().flatMap(c -> c.getPoints().stream())
                .map(PointControle::getId).collect(Collectors.toSet());
        List<Constat> releves = constats.findAllByAuditId(audit.getId());
        long renseignes = releves.stream().map(Constat::getPointControle).filter(Objects::nonNull)
                .map(PointControle::getId).filter(points::contains).distinct().count();

        long aEvaluer = criteresAEvaluer(audit);
        EvaluationRQAPBF derniere = evaluations.findAllByAuditIdOrderByDateEvaluationDesc(audit.getId()).stream()
                .findFirst().orElse(null);
        long notes = derniere == null ? 0 : derniere.getNotations().size();
        long publies = derniere == null ? 0 : derniere.getNotations().stream().filter(NotationCritere::estPublie).count();

        List<String> raisons = auditService.raisonsDeNePasCloturer(audit);
        return AvancementDto.builder()
                .statut(audit.getStatut())
                .statutPlan(plan == null ? null : plan.getStatut())
                .planPartage(plan != null && plan.getStatut() == StatutPlanAudit.PARTAGE)
                .pointsDeControle(points.size())
                .pointsRenseignes(renseignes)
                .pourcentageConstats(points.isEmpty() ? null : 100.0 * renseignes / points.size())
                .constatsBrouillon(compter(releves, StatutConstat.BROUILLON))
                .constatsCompiles(compter(releves, StatutConstat.COMPILE))
                .constatsPublies(compter(releves, StatutConstat.PUBLIE))
                .criteresAEvaluer(aEvaluer)
                .criteresNotes(notes)
                .criteresPublies(publies)
                .pourcentageRqapbf(aEvaluer == 0 ? null : 100.0 * notes / aEvaluer)
                .cloturable(raisons.isEmpty())
                .raisonsDeNePasCloturer(raisons)
                .build();
    }

    /**
     * Deux périodes côte à côte : la maturité RQAP-BF (évaluations validées) et les écarts relevés
     * par chapitre ISO (constats publiés générant une non-conformité).
     */
    @Transactional(readOnly = true)
    public ComparaisonDto comparer(LocalDate debut1, LocalDate fin1, LocalDate debut2, LocalDate fin2) {
        UUID directionId = Perimetre.exigerUneDirection();
        if (fin1.isBefore(debut1) || fin2.isBefore(debut2)) {
            throw new BusinessException("Une période finit avant d'avoir commencé.", HttpStatus.BAD_REQUEST);
        }
        Map<String, Long> ecarts1 = ecartsParChapitre(directionId, debut1, fin1);
        Map<String, Long> ecarts2 = ecartsParChapitre(directionId, debut2, fin2);
        Set<String> chapitres = new java.util.TreeSet<>(ecarts1.keySet());
        chapitres.addAll(ecarts2.keySet());
        return ComparaisonDto.builder()
                .periode1(periode(directionId, debut1, fin1))
                .periode2(periode(directionId, debut2, fin2))
                .ecartsParChapitre(chapitres.stream().map(c -> {
                    long avant = ecarts1.getOrDefault(c, 0L);
                    long apres = ecarts2.getOrDefault(c, 0L);
                    return ComparaisonDto.EcartsParChapitre.builder()
                            .chapitre(c).periode1(avant).periode2(apres)
                            .evolution(apres < avant ? "EN_AMELIORATION" : apres > avant ? "EN_DEGRADATION" : "STABLE")
                            .build();
                }).toList())
                .build();
    }

    /**
     * Les critères RQAP-BF publiés au constat « NC », de toute la direction ou d'un audit (D18),
     * page par page et du plus récent au plus ancien.
     */
    @Transactional(readOnly = true)
    public Page<NotationCritereDto> ecartsRqapbf(UUID auditId, Pageable pageable) {
        UUID directionId = Perimetre.exigerUneDirection();
        Specification<NotationCritere> ecarts = (racine, requete, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(racine.get("evaluation").get("directionId"), directionId));
            conditions.add(cb.equal(racine.get("statut"), StatutNotation.PUBLIE));
            conditions.add(racine.get("niveau").in(NiveauNotationRQAPBF.nonConformes()));
            if (auditId != null) {
                conditions.add(cb.equal(racine.get("evaluation").get("audit").get("id"), auditId));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return notations.findAll(ecarts, Filtres.ordonnee(pageable, Sort.by(Sort.Direction.DESC, "evaluation.dateEvaluation")))
                .map(n -> NotationCritereDto.builder()
                        .id(n.getId())
                        .auditId(n.getEvaluation().getAudit().getId())
                        .auditReference(n.getEvaluation().getAudit().getReference())
                        .noeudId(n.getNoeud().getId())
                        .noeudCode(n.getNoeud().getCode())
                        .noeudLibelle(n.getNoeud().getLibelle())
                        .niveau(n.getNiveau())
                        .score(n.getScore())
                        .preuve(n.getPreuve())
                        .commentaire(n.getCommentaire())
                        .constatRetenu(n.constatRetenu())
                        .statut(n.getStatut())
                        .nonConformiteId(n.getNonConformiteId())
                        .transmissionEnAttente(n.getNonConformiteId() == null)
                        .build());
    }

    private ComparaisonDto.Periode periode(UUID directionId, LocalDate debut, LocalDate fin) {
        List<EvaluationRQAPBF> validees = evaluations.findAllByDirectionIdAndDateEvaluationBetween(directionId, debut, fin)
                .stream().filter(e -> e.getStatut() == StatutEvaluation.VALIDEE).toList();
        return ComparaisonDto.Periode.builder()
                .debut(debut).fin(fin)
                .evaluations(validees.size())
                .scoreMoyen(moyenne(validees.stream().map(EvaluationRQAPBF::scoreOfficiel).toList()))
                .parDomaine(moyenneParDomaine(validees))
                .build();
    }

    /** Le chapitre d'un écart : celui du point de contrôle, à défaut celui de sa checklist. */
    private Map<String, Long> ecartsParChapitre(UUID directionId, LocalDate debut, LocalDate fin) {
        return constats.ecartsPubliesEntre(directionId, debut.atStartOfDay(), fin.plusDays(1).atStartOfDay().minusNanos(1))
                .stream()
                .collect(Collectors.groupingBy(c -> {
                    if (c.getPointControle() != null && c.getPointControle().getChapitreISO() != null) {
                        return c.getPointControle().getChapitreISO();
                    }
                    if (c.getChecklist() != null && c.getChecklist().getChapitreISO() != null) {
                        return c.getChecklist().getChapitreISO();
                    }
                    return "Hors checklist";
                }, TreeMap::new, Collectors.counting()));
    }

    /** Les critères terminaux sous les domaines de l'audit ; sous tout le référentiel s'il n'en vise aucun. */
    private long criteresAEvaluer(Audit audit) {
        List<NoeudReferentiel> tous = noeuds.findAllByDirectionIdOrderByCreatedAtAsc(audit.getDirectionId());
        Set<UUID> parents = tous.stream().map(NoeudReferentiel::getParent).filter(Objects::nonNull)
                .map(NoeudReferentiel::getId).collect(Collectors.toSet());
        Set<UUID> domaines = new HashSet<>();
        audit.getDomainesRqapbf().forEach(d -> domaines.add(d.getId()));
        return tous.stream()
                .filter(n -> !parents.contains(n.getId()))
                .filter(n -> domaines.isEmpty() || domaines.contains(n.racine().getId()))
                .count();
    }

    private static long compter(List<Constat> releves, StatutConstat statut) {
        return releves.stream().filter(c -> c.getStatut() == statut).count();
    }

    /** Moyenne, par domaine, des résultats officiels de plusieurs évaluations. */
    private static Map<String, Double> moyenneParDomaine(List<EvaluationRQAPBF> evaluees) {
        Map<String, List<Double>> parDomaine = new LinkedHashMap<>();
        evaluees.stream()
                .sorted(Comparator.comparing(EvaluationRQAPBF::getDateEvaluation))
                .forEach(e -> e.genererResultatsParDomaine(true).forEach((domaine, score) -> {
                    if (score != null) {
                        parDomaine.computeIfAbsent(domaine, k -> new ArrayList<>()).add(score);
                    }
                }));
        Map<String, Double> resultat = new HashMap<>();
        parDomaine.forEach((domaine, valeurs) -> resultat.put(domaine, moyenne(valeurs)));
        return new TreeMap<>(resultat);
    }

    private static Double moyenne(java.util.Collection<Double> valeurs) {
        return valeurs.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue).average()
                .stream().boxed().findFirst().orElse(null);
    }
}
