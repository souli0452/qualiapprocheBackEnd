package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.evaluation.dto.ActionMaitriseRisqueDto;
import com.qualiapproche.evaluation.dto.AuditDto;
import com.qualiapproche.evaluation.dto.FiltreProgramme;
import com.qualiapproche.evaluation.dto.RedactionRapportDto;
import com.qualiapproche.evaluation.entities.ActionMaitriseRisque;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.EvaluationRQAPBF;
import com.qualiapproche.evaluation.entities.NoeudReferentiel;
import com.qualiapproche.evaluation.entities.Signature;
import com.qualiapproche.evaluation.enumeration.NiveauEfficacite;
import com.qualiapproche.evaluation.enumeration.OrigineDocument;
import com.qualiapproche.evaluation.enumeration.StatutAudit;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.enumeration.StatutConstat;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.enumeration.StatutSignature;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import com.qualiapproche.evaluation.repository.ConstatRepository;
import com.qualiapproche.evaluation.repository.NoeudReferentielRepository;
import com.qualiapproche.evaluation.repository.EvaluationRQAPBFRepository;
import com.qualiapproche.evaluation.repository.SignatureRepository;
import com.qualiapproche.evaluation.repository.SiteRepository;
import com.qualiapproche.evaluation.repository.TypeAuditRepository;
import com.qualiapproche.evaluation.service.referentiel.CodificationDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Le programme d'audit : planifier, composer l'équipe, conduire, clôturer.
 *
 * <p>Les transitions de statut sont celles de l'entité ; ce service vérifie ce qu'elle ne peut
 * pas voir seule — l'équipe disponible et indépendante, les constats en suspens, le circuit de
 * signature — puis lui délègue le changement.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String QUOI = "audit";
    private static final Sort ORDRE = Sort.by(Sort.Order.asc("dateDebutPrevue"), Sort.Order.asc("reference"));

    private final AuditRepository depot;
    private final TypeAuditRepository types;
    private final SiteRepository sites;
    private final ChecklistAuditRepository checklists;
    private final ConstatRepository constats;
    private final SignatureRepository signatures;
    private final NoeudReferentielRepository noeuds;
    private final EvaluationRQAPBFRepository evaluations;
    private final AuditeurService auditeurs;
    private final CodificationDocumentService codification;
    private final FichiersAudit fichiers;

    @Transactional(readOnly = true)
    public Page<AuditDto> rechercher(FiltreProgramme filtre, Pageable pageable) {
        UUID directionId = Perimetre.exigerUneDirection();
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDRE);
        Page<Audit> page = depot.findAll(SpecificationsAudit.du(directionId, filtre,
                SecurityUtils.getCurrentUserId(), LocalDate.now()), range);
        Map<UUID, Double> scores = scoresDeMaturite(page.getContent().stream().map(Audit::getId).toList());
        return page.map(a -> {
            AuditDto dto = versDto(a);
            dto.setScoreMaturite(scores.get(a.getId()));
            return dto;
        });
    }

    /**
     * Le score de maturité de chaque audit : sa dernière évaluation RQAP-BF validée, à défaut la
     * plus récente. En une requête pour toute la page, et non une par ligne.
     */
    public Map<UUID, Double> scoresDeMaturite(List<UUID> auditIds) {
        if (auditIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Double> scores = new java.util.HashMap<>();
        evaluations.findAllByAuditIdIn(auditIds).stream()
                .filter(e -> e.getScoreGlobal() != null)
                .collect(Collectors.groupingBy(e -> e.getAudit().getId()))
                .forEach((auditId, liste) -> liste.stream()
                        .max(java.util.Comparator
                                .comparing((EvaluationRQAPBF e) -> e.getStatut() == StatutEvaluation.VALIDEE)
                                .thenComparing(EvaluationRQAPBF::getDateEvaluation))
                        .ifPresent(e -> scores.put(auditId, e.getScoreGlobal())));
        return scores;
    }

    @Transactional(readOnly = true)
    public AuditDto lire(UUID id) {
        Audit audit = sienOuRien(id);
        AuditDto dto = versDto(audit);
        dto.setScoreMaturite(scoresDeMaturite(List.of(audit.getId())).get(audit.getId()));
        return dto;
    }

    @Transactional
    public AuditDto creer(AuditDto saisie) {
        Perimetre.exigerUneDirection();
        Audit audit = Audit.builder()
                .reference(codification.genererReference(CodificationDocumentService.AUDIT))
                .statut(StatutAudit.PLANIFIE)
                .build();
        recopier(saisie, audit);
        return versDto(depot.save(audit));
    }

    @Transactional
    public AuditDto modifier(UUID id, AuditDto saisie) {
        Audit audit = sienOuRien(id);
        audit.exigerModifiable();
        recopier(saisie, audit);
        return versDto(depot.save(audit));
    }

    /** Ne supprime qu'un audit encore planifié : au-delà, il a une histoire, et s'annule. */
    @Transactional
    public void supprimer(UUID id) {
        Audit audit = sienOuRien(id);
        if (audit.getStatut() != StatutAudit.PLANIFIE) {
            throw new BusinessException("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : il s'annule, il ne se supprime plus.", HttpStatus.CONFLICT);
        }
        depot.delete(audit);
    }

    @Transactional
    public AuditDto valider(UUID id) {
        Audit audit = sienOuRien(id);
        verifierLEquipe(audit);
        audit.valider();
        return versDto(depot.save(audit));
    }

    /**
     * Valide d'un geste une série d'audits du programme.
     *
     * <p>Tout ou rien : un programme validé à moitié laisserait croire, à la relecture, que les
     * audits restés planifiés ont été écartés. Le premier audit qui ne passe pas arrête le lot,
     * et son message dit lequel et pourquoi.</p>
     */
    @Transactional
    public List<AuditDto> validerProgramme(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().distinct().map(this::valider).toList();
    }

    @Transactional
    public AuditDto demarrer(UUID id) {
        Audit audit = sienOuRien(id);
        audit.demarrer();
        return versDto(depot.save(audit));
    }

    @Transactional
    public AuditDto annuler(UUID id) {
        Audit audit = sienOuRien(id);
        audit.annuler();
        return versDto(depot.save(audit));
    }

    /**
     * Clôt l'audit, ou dit ce qui l'en empêche.
     *
     * <p>Trois conditions, vérifiées dans l'ordre où on les lève en pratique :</p>
     * <ol>
     *   <li>aucun constat ne reste en brouillon ou en compilation : tout ce qui a été relevé a été
     *       tranché par l'équipe ;</li>
     *   <li>le rapport existe — rédigé ici, ou chargé quand le type d'audit le fait signer
     *       ailleurs ;</li>
     *   <li>sauf circuit externe, le circuit de signature est posé et entièrement signé.</li>
     * </ol>
     */
    @Transactional
    public AuditDto cloturer(UUID id) {
        Audit audit = sienOuRien(id);
        List<String> raisons = raisonsDeNePasCloturer(audit);
        if (!raisons.isEmpty()) {
            throw new BusinessException(raisons.get(0), HttpStatus.CONFLICT);
        }
        audit.cloturer();
        return versDto(depot.save(audit));
    }

    /**
     * Ce qui empêche encore la clôture, dans l'ordre où on le lève en pratique ; vide si rien.
     *
     * <p>La même liste sert au refus de clôturer et à l'écran de réalisation, qui grise « Terminer
     * l'audit » en disant pourquoi : les deux ne peuvent pas diverger.</p>
     */
    public List<String> raisonsDeNePasCloturer(Audit audit) {
        List<String> raisons = new ArrayList<>();
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            raisons.add("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : seul un audit en cours se clôture.");
        }
        long enSuspens = constats.countByAuditIdAndStatutIn(audit.getId(),
                EnumSet.of(StatutConstat.BROUILLON, StatutConstat.COMPILE));
        if (enSuspens > 0) {
            raisons.add(enSuspens + " constat(s) de l'audit " + audit.getReference()
                    + " restent en brouillon ou en compilation : l'équipe doit les publier ou les"
                    + " écarter avant la clôture.");
        }
        boolean circuitExterne = audit.getType() != null && audit.getType().isCircuitExterne();
        if (circuitExterne) {
            if (audit.getFichierRapport() == null) {
                raisons.add("Ce type d'audit fait signer son rapport hors de l'application : chargez le"
                        + " rapport signé avant de clôturer.");
            }
        } else {
            List<Signature> circuit = signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId());
            if (circuit.isEmpty()) {
                raisons.add("Aucun circuit de signature n'est posé sur le rapport de " + audit.getReference() + ".");
            } else if (circuit.stream().anyMatch(s -> s.getStatut() != StatutSignature.SIGNEE)) {
                raisons.add("Le rapport de " + audit.getReference() + " n'est pas encore signé par tout le circuit.");
            }
        }
        return raisons;
    }

    /** Les conclusions et recommandations, tant que le rapport n'est pas parti en signature. */
    @Transactional
    public AuditDto rediger(UUID id, RedactionRapportDto redaction) {
        Audit audit = sienOuRien(id);
        if (audit.getStatut() != StatutAudit.EN_COURS) {
            throw new BusinessException("Le rapport se rédige pendant l'audit ; " + audit.getReference()
                    + " est " + audit.getStatut() + ".", HttpStatus.CONFLICT);
        }
        if (signatures.findAllByAuditIdOrderByOrdreAsc(audit.getId()).stream()
                .anyMatch(s -> s.getStatut() == StatutSignature.SIGNEE)) {
            throw new BusinessException("Le rapport de " + audit.getReference() + " a déjà reçu un visa :"
                    + " reposez le circuit avant de le reprendre.", HttpStatus.CONFLICT);
        }
        audit.setConclusionsRapport(redaction.getConclusions());
        audit.setRecommandationsRapport(redaction.getRecommandations());
        return versDto(depot.save(audit));
    }

    @Transactional
    public AuditDto chargerRapport(UUID id, MultipartFile fichier) {
        Audit audit = sienOuRien(id);
        if (audit.getStatut().estTermine()) {
            throw new BusinessException("L'audit " + audit.getReference() + " est " + audit.getStatut()
                    + " : son rapport ne se remplace plus.", HttpStatus.CONFLICT);
        }
        audit.setFichierRapport(fichiers.deposer(fichier, audit.getId().toString(), "rapport"));
        if (audit.getReferenceRapport() == null) {
            audit.setReferenceRapport(codification.genererReference(CodificationDocumentService.RAPPORT_AUDIT));
        }
        audit.setOrigineRapport(OrigineDocument.CHARGE);
        return versDto(depot.save(audit));
    }

    @Transactional(readOnly = true)
    public InputStream contenuDuRapport(UUID id) {
        return fichiers.contenu(sienOuRien(id).getFichierRapport());
    }

    @Transactional
    public AuditDto evaluerEfficacite(UUID auditId, UUID actionId, NiveauEfficacite niveau) {
        Audit audit = sienOuRien(auditId);
        ActionMaitriseRisque action = audit.getActionsMaitriseRisques().stream()
                .filter(a -> a.getId().equals(actionId)).findFirst()
                .orElseThrow(() -> new BusinessException("Aucune action de maîtrise du risque ne porte"
                        + " cet identifiant dans l'audit " + audit.getReference() + ".", HttpStatus.NOT_FOUND));
        action.evaluerEfficacite(niveau);
        return versDto(depot.save(audit));
    }

    public Audit sienOuRien(UUID id) {
        return Perimetre.sienneOuRien(depot, id, QUOI);
    }

    /**
     * L'équipe peut-elle conduire cet audit ?
     *
     * <p>Chacun de ses membres doit être actif, indépendant du processus audité, et libre sur la
     * période. Vérifié à la validation, et non à chaque saisie : un programme se compose par
     * retouches, et refuser chaque brouillon incomplet le rendrait impossible à écrire.</p>
     */
    void verifierLEquipe(Audit audit) {
        Set<Auditeur> equipe = new LinkedHashSet<>(audit.getMembresEquipe());
        if (audit.getResponsableEquipe() != null) {
            equipe.add(audit.getResponsableEquipe());
        }
        for (Auditeur auditeur : equipe) {
            if (auditeur.aConflitInteret(audit)) {
                throw new BusinessException("L'auditeur " + auditeur.getUtilisateurId() + " appartient au"
                        + " processus audité ou le pilote : il ne peut pas l'auditer.", HttpStatus.CONFLICT);
            }
            if (!auditeurs.estDisponible(auditeur, audit.getDateDebutPrevue(), audit.getDateFinPrevue(),
                    audit.getId())) {
                throw new BusinessException("L'auditeur " + auditeur.getUtilisateurId() + " est inactif ou"
                        + " déjà engagé sur un autre audit pendant cette période.", HttpStatus.CONFLICT);
            }
        }
    }

    private void recopier(AuditDto saisie, Audit cible) {
        if (saisie.getDateDebutPrevue() != null && saisie.getDateFinPrevue() != null
                && saisie.getDateFinPrevue().isBefore(saisie.getDateDebutPrevue())) {
            throw new BusinessException("La date de fin précède la date de début.", HttpStatus.BAD_REQUEST);
        }
        cible.setObjectifsAudit(saisie.getObjectifsAudit());
        cible.setPorteeAudit(saisie.getPorteeAudit());
        cible.setCriteresAudit(saisie.getCriteresAudit());
        cible.setDateDebutPrevue(saisie.getDateDebutPrevue());
        cible.setDateFinPrevue(saisie.getDateFinPrevue());
        cible.setDureeEstimeeJours(saisie.getDureeEstimeeJours() != null ? saisie.getDureeEstimeeJours()
                : dureeEntre(saisie.getDateDebutPrevue(), saisie.getDateFinPrevue()));
        cible.setNiveauRisque(saisie.getNiveauRisque());
        cible.setOrganismeExterne(saisie.getOrganismeExterne());
        cible.setReferentiels(saisie.getReferentiels() == null ? new ArrayList<>()
                : new ArrayList<>(saisie.getReferentiels().stream().filter(r -> r != null && !r.isBlank())
                        .map(String::strip).distinct().toList()));
        cible.setDescriptionRisque(saisie.getDescriptionRisque());
        cible.setProcessusId(saisie.getProcessusId());
        cible.setType(Perimetre.sienneOuNul(types, saisie.getTypeAuditId(), "type d'audit"));
        cible.setResponsableEquipe(saisie.getResponsableEquipeId() == null ? null
                : auditeurs.sienOuRien(saisie.getResponsableEquipeId()));

        cible.getSitesConcernes().clear();
        ids(saisie.getSiteIds()).forEach(s -> cible.getSitesConcernes().add(Perimetre.sienneOuRien(sites, s, "site")));
        cible.getMembresEquipe().clear();
        ids(saisie.getMembreEquipeIds()).forEach(m -> cible.getMembresEquipe().add(auditeurs.sienOuRien(m)));
        cible.getChecklists().clear();
        for (UUID c : ids(saisie.getChecklistIds())) {
            ChecklistAudit checklist = Perimetre.sienneOuRien(checklists, c, "checklist");
            if (checklist.getStatut() != StatutChecklist.PUBLIEE) {
                throw new BusinessException("La checklist « " + checklist.getNom() + " » n'est pas publiée :"
                        + " elle ne peut pas encore servir à un audit.", HttpStatus.CONFLICT);
            }
            cible.getChecklists().add(checklist);
        }
        cible.getDomainesRqapbf().clear();
        for (UUID d : ids(saisie.getDomaineRqapbfIds())) {
            NoeudReferentiel domaine = Perimetre.sienneOuRien(noeuds, d, "domaine RQAP-BF");
            if (domaine.getParent() != null) {
                throw new BusinessException("« " + domaine.getCode() + " » n'est pas un domaine du référentiel"
                        + " RQAP-BF, mais l'une de ses subdivisions.", HttpStatus.BAD_REQUEST);
            }
            cible.getDomainesRqapbf().add(domaine);
        }
        recopierLesActions(saisie.getActionsMaitriseRisques(), cible);
    }

    /**
     * Les actions de maîtrise du risque, par identifiant : une action reprise garde son
     * évaluation d'efficacité, une nouvelle part non évaluée, une absente est retirée.
     */
    private void recopierLesActions(List<ActionMaitriseRisqueDto> saisies, Audit cible) {
        Map<UUID, ActionMaitriseRisque> existantes = cible.getActionsMaitriseRisques().stream()
                .collect(Collectors.toMap(ActionMaitriseRisque::getId, a -> a));
        List<ActionMaitriseRisque> retenues = new ArrayList<>();
        for (ActionMaitriseRisqueDto saisie : saisies == null ? List.<ActionMaitriseRisqueDto>of() : saisies) {
            ActionMaitriseRisque action = saisie.getId() == null ? null : existantes.get(saisie.getId());
            if (action == null) {
                action = ActionMaitriseRisque.builder().audit(cible).efficacite(NiveauEfficacite.NON_EVALUEE).build();
            }
            action.setLibelle(saisie.getLibelle());
            action.setResponsable(saisie.getResponsable());
            action.setDelai(saisie.getDelai());
            retenues.add(action);
        }
        cible.getActionsMaitriseRisques().clear();
        cible.getActionsMaitriseRisques().addAll(retenues);
    }

    private static List<UUID> ids(List<UUID> ids) {
        return ids == null ? List.of() : ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private static Integer dureeEntre(LocalDate debut, LocalDate fin) {
        return debut == null || fin == null ? null : (int) ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    AuditDto versDto(Audit audit) {
        return AuditDto.builder()
                .id(audit.getId())
                .reference(audit.getReference())
                .objectifsAudit(audit.getObjectifsAudit())
                .porteeAudit(audit.getPorteeAudit())
                .criteresAudit(audit.getCriteresAudit())
                .dateDebutPrevue(audit.getDateDebutPrevue())
                .dateFinPrevue(audit.getDateFinPrevue())
                .dureeEstimeeJours(audit.getDureeEstimeeJours())
                .niveauRisque(audit.getNiveauRisque())
                .descriptionRisque(audit.getDescriptionRisque())
                .statut(audit.getStatut())
                .enRetard(audit.estEnRetard(LocalDate.now()))
                .joursDeRetard(audit.joursDeRetard(LocalDate.now()))
                .origineRapport(audit.getOrigineRapport())
                .rapportCharge(audit.getFichierRapport() != null)
                .referenceRapport(audit.getReferenceRapport())
                .conclusionsRapport(audit.getConclusionsRapport())
                .recommandationsRapport(audit.getRecommandationsRapport())
                .organismeExterne(audit.getOrganismeExterne())
                .referentiels(List.copyOf(audit.getReferentiels()))
                .domaineRqapbfIds(audit.getDomainesRqapbf().stream().map(NoeudReferentiel::getId).toList())
                .typeAuditId(audit.getType() == null ? null : audit.getType().getId())
                .typeAuditLibelle(audit.getType() == null ? null : audit.getType().getLibelle())
                .circuitExterne(audit.getType() != null && audit.getType().isCircuitExterne())
                .processusId(audit.getProcessusId())
                .siteIds(audit.getSitesConcernes().stream().map(s -> s.getId()).toList())
                .responsableEquipeId(audit.getResponsableEquipe() == null ? null : audit.getResponsableEquipe().getId())
                .membreEquipeIds(audit.getMembresEquipe().stream().map(Auditeur::getId).toList())
                .checklistIds(audit.getChecklists().stream().map(ChecklistAudit::getId).toList())
                .actionsMaitriseRisques(audit.getActionsMaitriseRisques().stream()
                        .map(a -> ActionMaitriseRisqueDto.builder()
                                .id(a.getId())
                                .libelle(a.getLibelle())
                                .responsable(a.getResponsable())
                                .delai(a.getDelai())
                                .efficacite(a.getEfficacite())
                                .build())
                        .toList())
                .build();
    }
}
