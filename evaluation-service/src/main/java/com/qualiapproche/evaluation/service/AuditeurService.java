package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.spec.Filtres;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.evaluation.dto.AuditeurDto;
import com.qualiapproche.evaluation.dto.FiltreVivier;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.EvaluationAuditeur;
import com.qualiapproche.evaluation.enumeration.StatutAuditeur;
import com.qualiapproche.evaluation.repository.AuditRepository;
import com.qualiapproche.evaluation.repository.AuditeurRepository;
import com.qualiapproche.evaluation.repository.EvaluationAuditeurRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le vivier d'auditeurs d'une direction : qui peut auditer, quand, et quoi.
 *
 * <p>Les gestes que le diagramme prête à l'auditeur lui-même — valider un programme, valider un
 * constat — sont portés par les services de l'audit et du constat : ce sont eux qui savent ce
 * qu'ils valident. Ici ne restent que ce qui le décrit et ce qui se calcule sur lui.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditeurService {

    private static final String QUOI = "auditeur";

    private final AuditeurRepository depot;
    private final AuditRepository audits;
    private final EvaluationAuditeurRepository evaluations;

    @Transactional(readOnly = true)
    public List<AuditeurDto> lister() {
        return avecSesChiffres(depot.findAllByDirectionIdOrderByCreatedAtAsc(Perimetre.exigerUneDirection())
                .stream().map(this::versDto).toList());
    }

    /**
     * Le vivier page par page, filtré comme l'écran le demande (A4). Le nom d'un auditeur vit dans
     * l'annuaire et non ici : la recherche par nom se résout en amont en identifiants
     * d'utilisateurs ({@code utilisateurIds}).
     */
    @Transactional(readOnly = true)
    public Page<AuditeurDto> page(FiltreVivier filtre, Pageable pageable) {
        FiltreVivier f = filtre == null ? new FiltreVivier() : filtre;
        Page<AuditeurDto> page = depot.findAll(Filtres.tous(
                        Filtres.<Auditeur>deLaDirection(Perimetre.exigerUneDirection()),
                        Filtres.parmi("utilisateurId", f.getUtilisateurIds()),
                        Filtres.egal("statut", f.getStatut()),
                        Filtres.egal("structureAppartenance", f.getStructureId()),
                        detenantLaCertification(f.getCertification()),
                        selonSonEvaluation(f)),
                Filtres.ordonnee(pageable, Sort.by("createdAt")))
                .map(this::versDto);
        avecSesChiffres(page.getContent());
        return page;
    }

    /** Les auditeurs qui détiennent cette certification, sans tenir compte de la casse. */
    private static Specification<Auditeur> detenantLaCertification(String certification) {
        if (certification == null || certification.isBlank()) {
            return null;
        }
        String cherchee = certification.strip().toLowerCase();
        return (racine, requete, cb) -> {
            Subquery<Integer> detient = requete.subquery(Integer.class);
            Root<Auditeur> lui = detient.from(Auditeur.class);
            Join<Auditeur, String> certifications = lui.join("certifications");
            detient.select(cb.literal(1)).where(cb.equal(lui, racine), cb.equal(cb.lower(certifications), cherchee));
            return cb.exists(detient);
        };
    }

    /**
     * Le filtre par évaluation : les seuls non évalués, ou ceux dont la moyenne tombe dans la
     * tranche d'un niveau. La moyenne est celle que la liste affiche, sur la valeur recopiée de
     * chaque note.
     */
    private static Specification<Auditeur> selonSonEvaluation(FiltreVivier f) {
        if (f.isNonEvalues()) {
            return (racine, requete, cb) -> {
                Subquery<Integer> note = requete.subquery(Integer.class);
                Root<EvaluationAuditeur> e = note.from(EvaluationAuditeur.class);
                note.select(cb.literal(1)).where(cb.equal(e.get("auditeur"), racine));
                return cb.not(cb.exists(note));
            };
        }
        if (f.getMoyenneMin() == null && f.getMoyenneMax() == null) {
            return null;
        }
        return (racine, requete, cb) -> {
            Subquery<Double> moyenne = requete.subquery(Double.class);
            Root<EvaluationAuditeur> e = moyenne.from(EvaluationAuditeur.class);
            moyenne.select(cb.avg(e.get("valeurNiveau"))).where(cb.equal(e.get("auditeur"), racine));
            List<Predicate> bornes = new ArrayList<>();
            if (f.getMoyenneMin() != null) {
                bornes.add(cb.greaterThan(moyenne, f.getMoyenneMin()));
            }
            if (f.getMoyenneMax() != null) {
                bornes.add(cb.lessThanOrEqualTo(moyenne, f.getMoyenneMax()));
            }
            return cb.and(bornes.toArray(new Predicate[0]));
        };
    }

    /**
     * Pose sur chaque auditeur sa moyenne d'évaluation et ses audits des douze derniers mois.
     *
     * <p>Seule la fiche d'un auditeur les calculait : le portefeuille affichait « Non évalué » et
     * zéro audit pour tout le monde. Trois requêtes groupées pour toute la liste, plutôt que deux
     * par auditeur.</p>
     */
    private List<AuditeurDto> avecSesChiffres(List<AuditeurDto> auditeurs) {
        if (auditeurs.isEmpty()) {
            return auditeurs;
        }
        List<UUID> ids = auditeurs.stream().map(AuditeurDto::getId).toList();
        LocalDate depuis = LocalDate.now().minusMonths(12);
        Map<UUID, Double> moyennes = new HashMap<>();
        evaluations.moyennes(ids).forEach(l -> moyennes.put((UUID) l[0], ((Number) l[1]).doubleValue()));
        Map<UUID, Long> realises = new HashMap<>();
        audits.realisesCommeResponsablesDepuis(ids, depuis)
                .forEach(l -> realises.merge((UUID) l[0], ((Number) l[1]).longValue(), Long::sum));
        audits.realisesCommeMembresDepuis(ids, depuis)
                .forEach(l -> realises.merge((UUID) l[0], ((Number) l[1]).longValue(), Long::sum));
        auditeurs.forEach(a -> {
            a.setScoreEvaluationMoyen(moyennes.get(a.getId()));
            a.setAuditsRealises12Mois(realises.getOrDefault(a.getId(), 0L));
        });
        return auditeurs;
    }

    @Transactional(readOnly = true)
    public AuditeurDto lire(UUID id) {
        Auditeur auditeur = sienOuRien(id);
        AuditeurDto dto = versDto(auditeur);
        dto.setScoreEvaluationMoyen(evaluations.moyenne(auditeur.getId()));
        dto.setAuditsRealises12Mois(audits.realisesDepuis(auditeur.getId(), LocalDate.now().minusMonths(12)));
        return dto;
    }

    @Transactional
    public AuditeurDto creer(AuditeurDto saisie) {
        UUID directionId = Perimetre.exigerUneDirection();
        String utilisateurId = saisie.getUtilisateurId().strip();
        if (depot.existsByDirectionIdAndUtilisateurId(directionId, utilisateurId)) {
            throw new BusinessException("Cette personne est déjà inscrite au vivier d'auditeurs.",
                    HttpStatus.CONFLICT);
        }
        Auditeur auditeur = Auditeur.builder().utilisateurId(utilisateurId).build();
        recopier(saisie, auditeur);
        return versDto(depot.save(auditeur));
    }

    @Transactional
    public AuditeurDto modifier(UUID id, AuditeurDto saisie) {
        Auditeur auditeur = sienOuRien(id);
        recopier(saisie, auditeur);
        return versDto(depot.save(auditeur));
    }

    /**
     * Retire un auditeur du vivier : il passe inactif, et n'est jamais effacé.
     *
     * <p>Ses constats, ses évaluations et les équipes passées le citent ; l'effacer les rendrait
     * orphelins, ou ferait échouer la suppression sur une clé étrangère.</p>
     */
    @Transactional
    public void desactiver(UUID id) {
        Auditeur auditeur = sienOuRien(id);
        auditeur.setStatut(StatutAuditeur.INACTIF);
        depot.save(auditeur);
    }

    /**
     * L'auditeur est-il libre sur cette période ?
     *
     * <p>Libre s'il est actif et ne figure dans l'équipe d'aucun audit vivant dont les dates
     * chevauchent la période. {@code exclu} écarte l'audit qu'on est en train de composer, qui
     * ne doit pas se compter comme un conflit avec lui-même.</p>
     */
    @Transactional(readOnly = true)
    public boolean estDisponible(UUID id, LocalDate debut, LocalDate fin, UUID exclu) {
        return estDisponible(sienOuRien(id), debut, fin, exclu);
    }

    boolean estDisponible(Auditeur auditeur, LocalDate debut, LocalDate fin, UUID exclu) {
        if (auditeur.getStatut() != StatutAuditeur.ACTIF) {
            return false;
        }
        if (debut == null || fin == null) {
            return true;
        }
        return audits.chevauchant(auditeur.getId(), debut, fin).stream()
                .noneMatch(a -> !a.getId().equals(exclu));
    }

    /**
     * Pourquoi l'auditeur ne peut pas être affecté sur la période, ou rien s'il le peut. L'alerte
     * « inactif ou déjà engagé » ne disait ni lequel des deux ni sur quel audit (observation 8).
     */
    @Transactional(readOnly = true)
    public String raisonDIndisponibilite(UUID id, LocalDate debut, LocalDate fin, UUID exclu) {
        Auditeur auditeur = sienOuRien(id);
        if (auditeur.getStatut() != StatutAuditeur.ACTIF) {
            return "est inactif dans le vivier d'auditeurs";
        }
        if (debut == null || fin == null) {
            return null;
        }
        java.time.format.DateTimeFormatter jour = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return audits.chevauchant(auditeur.getId(), debut, fin).stream()
                .filter(a -> !a.getId().equals(exclu))
                .findFirst()
                .map(a -> "est déjà " + (a.getResponsableEquipe() != null && a.getResponsableEquipe().getId().equals(id)
                        ? "responsable" : "membre de l'équipe") + " de l'audit " + a.getReference()
                        + (a.getDateDebutPrevue() == null ? "" : " du " + a.getDateDebutPrevue().format(jour))
                        + (a.getDateFinPrevue() == null ? "" : " au " + a.getDateFinPrevue().format(jour)))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean aConflitInteret(UUID id, UUID auditId) {
        Audit audit = Perimetre.sienneOuRien(audits, auditId, "audit");
        return sienOuRien(id).aConflitInteret(audit);
    }

    /** L'auditeur que l'appelant est dans cette direction, s'il l'est. */
    @Transactional(readOnly = true)
    public Auditeur auditeurCourant() {
        String utilisateurId = SecurityUtils.getCurrentUserId();
        return depot.findByDirectionIdAndUtilisateurId(Perimetre.exigerUneDirection(), utilisateurId)
                .orElseThrow(() -> new BusinessException("Vous n'êtes pas inscrit au vivier"
                        + " d'auditeurs de votre direction : seuls ses membres saisissent des constats.",
                        HttpStatus.FORBIDDEN));
    }

    public Auditeur sienOuRien(UUID id) {
        return Perimetre.sienneOuRien(depot, id, QUOI);
    }

    private void recopier(AuditeurDto saisie, Auditeur cible) {
        cible.setDomainesHabilites(copie(saisie.getDomainesHabilites()));
        cible.setCertifications(copie(saisie.getCertifications()));
        cible.setDisponibilite(saisie.getDisponibilite());
        cible.setStructureAppartenance(saisie.getStructureAppartenance());
        cible.setStatut(saisie.getStatut() == null ? StatutAuditeur.ACTIF : saisie.getStatut());
        cible.setNiveauHabilitation(saisie.getNiveauHabilitation());
        cible.setProcessusGeres(copie(saisie.getProcessusGeres()));
    }

    private static List<String> copie(List<String> valeurs) {
        return valeurs == null ? new ArrayList<>() : new ArrayList<>(valeurs.stream()
                .filter(v -> v != null && !v.isBlank()).map(String::strip).distinct().toList());
    }

    AuditeurDto versDto(Auditeur auditeur) {
        return AuditeurDto.builder()
                .id(auditeur.getId())
                .utilisateurId(auditeur.getUtilisateurId())
                .domainesHabilites(List.copyOf(auditeur.getDomainesHabilites()))
                .certifications(List.copyOf(auditeur.getCertifications()))
                .disponibilite(auditeur.getDisponibilite())
                .structureAppartenance(auditeur.getStructureAppartenance())
                .statut(auditeur.getStatut())
                .niveauHabilitation(auditeur.getNiveauHabilitation())
                .processusGeres(List.copyOf(auditeur.getProcessusGeres()))
                .build();
    }
}
