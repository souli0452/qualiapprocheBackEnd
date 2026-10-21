package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.EvaluationRQAPBFDto;
import com.qualiapproche.evaluation.dto.NoeudReferentielDto;
import com.qualiapproche.evaluation.dto.NotationCritereDto;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.enumeration.ConstatRQAPBF;
import com.qualiapproche.evaluation.enumeration.NiveauNotationRQAPBF;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.enumeration.StatutNotation;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.NoeudReferentielRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Le référentiel RQAPBF et les évaluations menées à son regard.
 *
 * <p>Le référentiel est un arbre — domaines, sous-domaines, critères —, et seules ses feuilles
 * se notent. Une évaluation se saisit critère par critère en brouillon ; ses scores se
 * recalculent à chaque note, puis elle se valide et ne bouge plus.</p>
 */
@Service
@RequiredArgsConstructor
public class ReferentielRQAPBFService {

    private final NoeudReferentielRepository noeuds;
    private final EvaluationRQAPBFRepository evaluations;
    private final AuditService audits;
    private final ApplicationEventPublisher evenements;
    private final RegleEcartRQAPBFService regles;
    private final GrilleDesMentionsRQAPBF mentions;

    // ------------------------------------------------------------------ Référentiel

    /** L'arbre entier, à partir de ses domaines. */
    @Transactional(readOnly = true)
    public List<NoeudReferentielDto> arbre() {
        return noeuds.findAllByDirectionIdOrderByCreatedAtAsc(Perimetre.exigerUneDirection()).stream()
                .filter(n -> n.getParent() == null)
                .sorted(Comparator.comparingInt(NoeudReferentiel::getOrdre))
                .map(this::versDtoAvecEnfants).toList();
    }

    @Transactional
    public NoeudReferentielDto creerNoeud(NoeudReferentielDto saisie) {
        Perimetre.exigerUneDirection();
        NoeudReferentiel noeud = new NoeudReferentiel();
        recopier(saisie, noeud);
        return versDtoAvecEnfants(enregistrer(noeud));
    }

    @Transactional
    public NoeudReferentielDto modifierNoeud(UUID id, NoeudReferentielDto saisie) {
        NoeudReferentiel noeud = Perimetre.sienneOuRien(noeuds, id, "nœud du référentiel");
        recopier(saisie, noeud);
        return versDtoAvecEnfants(enregistrer(noeud));
    }

    /** Un nœud se supprime sans descendants, et s'il n'a jamais été noté. */
    @Transactional
    public void supprimerNoeud(UUID id) {
        NoeudReferentiel noeud = Perimetre.sienneOuRien(noeuds, id, "nœud du référentiel");
        if (!noeud.estFeuille()) {
            throw new BusinessException("Le nœud " + noeud.getCode() + " a des descendants : supprimez-les"
                    + " d'abord.", HttpStatus.CONFLICT);
        }
        try {
            noeuds.delete(noeud);
            noeuds.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Le critère " + noeud.getCode() + " a déjà été noté dans une"
                    + " évaluation : il ne se supprime plus.", HttpStatus.CONFLICT);
        }
    }

    private void recopier(NoeudReferentielDto saisie, NoeudReferentiel cible) {
        cible.setCode(saisie.getCode().strip());
        cible.setLibelle(saisie.getLibelle().strip());
        cible.setTypeNoeud(saisie.getTypeNoeud());
        cible.setPreuveAttendue(saisie.getPreuveAttendue());
        cible.setOrdre(saisie.getOrdre());
        NoeudReferentiel parent = Perimetre.sienneOuNul(noeuds, saisie.getParentId(), "nœud parent");
        // Un nœud ne peut pas descendre de lui-même : l'arbre deviendrait une boucle, et le calcul
        // du domaine d'un critère ne finirait jamais.
        for (NoeudReferentiel p = parent; p != null; p = p.getParent()) {
            if (cible.getId() != null && p.getId().equals(cible.getId())) {
                throw new BusinessException("Un nœud ne peut pas être rangé sous l'un de ses descendants.",
                        HttpStatus.BAD_REQUEST);
            }
        }
        cible.setParent(parent);
    }

    private NoeudReferentiel enregistrer(NoeudReferentiel noeud) {
        try {
            return noeuds.saveAndFlush(noeud);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Le code " + noeud.getCode() + " est déjà employé dans le référentiel.",
                    HttpStatus.CONFLICT);
        }
    }

    // ------------------------------------------------------------------ Évaluations

    @Transactional(readOnly = true)
    public List<EvaluationRQAPBFDto> evaluationsDeLAudit(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        return evaluations.findAllByAuditIdOrderByDateEvaluationDesc(audit.getId()).stream()
                .map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public EvaluationRQAPBFDto lireEvaluation(UUID id) {
        return versDto(evaluation(id));
    }

    @Transactional
    public EvaluationRQAPBFDto ouvrirEvaluation(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut().estTermine()) {
            throw new BusinessException("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : il ne s'évalue plus.", HttpStatus.CONFLICT);
        }
        return versDto(evaluations.save(EvaluationRQAPBF.builder()
                .audit(audit)
                .dateEvaluation(LocalDate.now())
                .statut(StatutEvaluation.BROUILLON)
                .build()));
    }

    /**
     * Pose ou corrige les notes d'une série de critères, puis recalcule le score.
     *
     * <p>Un critère déjà noté voit sa note remplacée, et non doublée : une évaluation note
     * chaque critère une fois.</p>
     */
    @Transactional
    public EvaluationRQAPBFDto noter(UUID evaluationId, List<NotationCritereDto> notes) {
        EvaluationRQAPBF evaluation = brouillon(evaluationId);
        Set<UUID> vus = new HashSet<>();
        for (NotationCritereDto note : notes) {
            if (!vus.add(note.getNoeudId())) {
                throw new BusinessException("Un même critère est noté deux fois dans la saisie.", HttpStatus.BAD_REQUEST);
            }
            NoeudReferentiel critere = Perimetre.sienneOuRien(noeuds, note.getNoeudId(), "critère du référentiel");
            if (!critere.estFeuille()) {
                throw new BusinessException("Le nœud " + critere.getCode() + " regroupe d'autres critères :"
                        + " seuls les critères terminaux se notent.", HttpStatus.BAD_REQUEST);
            }
            NotationCritere notation = evaluation.getNotations().stream()
                    .filter(n -> n.getNoeud().getId().equals(critere.getId())).findFirst()
                    .orElseGet(() -> {
                        NotationCritere nouvelle = NotationCritere.builder().evaluation(evaluation).noeud(critere)
                                .statut(StatutNotation.BROUILLON).build();
                        evaluation.getNotations().add(nouvelle);
                        return nouvelle;
                    });
            if (notation.estPublie()) {
                throw new BusinessException("Le critère " + critere.getCode() + " est publié : sa note ne se"
                        + " corrige plus.", HttpStatus.CONFLICT);
            }
            notation.setNiveau(note.getNiveau());
            notation.setPreuve(note.getPreuve());
            notation.setCommentaire(note.getCommentaire());
            notation.calculerScore();
        }
        evaluation.calculerScoreGlobal();
        return versDto(evaluations.save(evaluation));
    }

    /** « Publier ce critère » : il entre au rapport officiel, et sa note se fige. */
    @Transactional
    public EvaluationRQAPBFDto publierCritere(UUID evaluationId, UUID noeudId) {
        EvaluationRQAPBF evaluation = brouillon(evaluationId);
        NotationCritere notation = evaluation.getNotations().stream()
                .filter(n -> n.getNoeud().getId().equals(noeudId)).findFirst()
                .orElseThrow(() -> new BusinessException("Ce critère n'est pas noté dans cette évaluation.",
                        HttpStatus.NOT_FOUND));
        notation.publier();
        EvaluationRQAPBF enregistree = evaluations.save(evaluation);
        annoncerLesEcarts(enregistree);
        return versDto(enregistree);
    }

    /**
     * « Publier ce groupe » : les critères en brouillon d'un niveau, ou de tous sans niveau.
     *
     * <p>C'est le geste de la prévisualisation des résultats : on relit les critères groupés par
     * niveau, puis on les publie ensemble. Les critères non notés ne sont pas concernés.</p>
     */
    @Transactional
    public EvaluationRQAPBFDto publierGroupe(UUID evaluationId, NiveauNotationRQAPBF niveau) {
        EvaluationRQAPBF evaluation = brouillon(evaluationId);
        evaluation.getNotations().stream()
                .filter(n -> !n.estPublie() && n.getNiveau() != null)
                .filter(n -> niveau == null || n.getNiveau() == niveau)
                .forEach(NotationCritere::publier);
        EvaluationRQAPBF enregistree = evaluations.save(evaluation);
        annoncerLesEcarts(enregistree);
        return versDto(enregistree);
    }

    /**
     * « Publier tout et voir les résultats officiels » : publie tout ce qui est noté, puis fige
     * l'évaluation.
     */
    @Transactional
    public EvaluationRQAPBFDto validerEvaluation(UUID id) {
        EvaluationRQAPBF evaluation = brouillon(id);
        // Le rapport reprend la dernière évaluation validée : en valider une après un visa
        // changerait son annexe RQAP-BF.
        audits.exigerRapportSansVisa(evaluation.getAudit(), "valider une évaluation RQAP-BF");
        if (evaluation.getNotations().isEmpty()) {
            throw new BusinessException("Une évaluation sans aucune note ne se valide pas.", HttpStatus.CONFLICT);
        }
        evaluation.getNotations().stream().filter(n -> !n.estPublie()).forEach(NotationCritere::publier);
        evaluation.calculerScoreGlobal();
        evaluation.setStatut(StatutEvaluation.VALIDEE);
        EvaluationRQAPBF enregistree = evaluations.save(evaluation);
        annoncerLesEcarts(enregistree);
        return versDto(enregistree);
    }

    /**
     * Les écarts à transmettre au module Non-conformités : les critères publiés dont le constat
     * retenu est NC.
     */
    @Transactional(readOnly = true)
    public List<NotationCritereDto> ecarts(UUID evaluationId) {
        EvaluationRQAPBF evaluation = evaluation(evaluationId);
        Set<NiveauNotationRQAPBF> enEcart = regles.niveauxEnEcart(evaluation.getDirectionId());
        return evaluation.getNotations().stream()
                .filter(NotationCritere::estPublie)
                .filter(n -> n.constatRetenu(enEcart) == ConstatRQAPBF.NC)
                .map(n -> versDto(n, enEcart))
                .toList();
    }

    @Transactional
    public void supprimerEvaluation(UUID id) {
        evaluations.delete(brouillon(id));
    }

    /**
     * Les critères publiés au constat NC partent vers M05, une fois la publication committée. Les
     * critères déjà transmis sont écartés par la transmission elle-même : on peut annoncer large.
     */
    private void annoncerLesEcarts(EvaluationRQAPBF evaluation) {
        Set<NiveauNotationRQAPBF> enEcart = regles.niveauxEnEcart(evaluation.getDirectionId());
        List<UUID> ecarts = evaluation.getNotations().stream()
                .filter(n -> n.estPublie() && n.constatRetenu(enEcart) == ConstatRQAPBF.NC && n.getNonConformiteId() == null)
                .map(NotationCritere::getId).toList();
        if (!ecarts.isEmpty()) {
            evenements.publishEvent(EcartsPublies.deNotations(ecarts));
        }
    }

    private EvaluationRQAPBF evaluation(UUID id) {
        return Perimetre.sienneOuRien(evaluations, id, "évaluation RQAPBF");
    }

    private EvaluationRQAPBF brouillon(UUID id) {
        EvaluationRQAPBF evaluation = evaluation(id);
        if (evaluation.getStatut() != StatutEvaluation.BROUILLON) {
            throw new BusinessException("Cette évaluation est validée : elle ne se modifie plus.", HttpStatus.CONFLICT);
        }
        return evaluation;
    }

    private NoeudReferentielDto versDtoAvecEnfants(NoeudReferentiel n) {
        return NoeudReferentielDto.builder()
                .id(n.getId())
                .code(n.getCode())
                .libelle(n.getLibelle())
                .typeNoeud(n.getTypeNoeud())
                .preuveAttendue(n.getPreuveAttendue())
                .ordre(n.getOrdre())
                .parentId(n.getParent() == null ? null : n.getParent().getId())
                .enfants(n.getEnfants().stream().map(this::versDtoAvecEnfants).toList())
                .build();
    }

    private EvaluationRQAPBFDto versDto(EvaluationRQAPBF e) {
        Set<NiveauNotationRQAPBF> enEcart = regles.niveauxEnEcart(e.getDirectionId());
        return EvaluationRQAPBFDto.builder()
                .id(e.getId())
                .auditId(e.getAudit().getId())
                .dateEvaluation(e.getDateEvaluation())
                .scoreGlobal(e.getScoreGlobal())
                .niveauConformite(e.getNiveauConformite())
                .statut(e.getStatut())
                .resultatsParDomaine(e.genererResultatsParDomaine())
                .scoreOfficiel(e.scoreOfficiel())
                .resultatsOfficielsParDomaine(e.genererResultatsParDomaine(true))
                .mention(mentions.mentionDe(e, false))
                .mentionOfficielle(mentions.mentionDe(e, true))
                .scoresParNoeud(e.scoresParNoeud(false))
                .criteresNotes(e.getNotations().size())
                .criteresPublies((int) e.getNotations().stream().filter(NotationCritere::estPublie).count())
                .notations(e.getNotations().stream().map(n -> versDto(n, enEcart)).toList())
                .build();
    }

    private NotationCritereDto versDto(NotationCritere n, Set<NiveauNotationRQAPBF> enEcart) {
        return NotationCritereDto.builder()
                .id(n.getId())
                .noeudId(n.getNoeud().getId())
                .noeudCode(n.getNoeud().getCode())
                .noeudLibelle(n.getNoeud().getLibelle())
                .preuveAttendue(n.getNoeud().getPreuveAttendue())
                .niveau(n.getNiveau())
                .score(n.getScore())
                .preuve(n.getPreuve())
                .commentaire(n.getCommentaire())
                .constatRetenu(n.constatRetenu(enEcart))
                .statut(n.getStatut())
                .nonConformiteId(n.getNonConformiteId())
                .transmissionEnAttente(n.estPublie() && n.constatRetenu(enEcart) == ConstatRQAPBF.NC
                        && n.getNonConformiteId() == null)
                .build();
    }
}
