package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.RepartitionPointDto;
import com.qualiapproche.evaluation.entities.AffectationPoint;
import com.qualiapproche.evaluation.entities.Audit;
import com.qualiapproche.evaluation.entities.Auditeur;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.repository.AffectationPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Qui renseigne quel point de contrôle, sur un audit donné.
 *
 * <p>Trois sources, dans cet ordre : la répartition faite sur l'audit par le responsable d'équipe ;
 * à défaut, l'auditeur que le modèle de checklist désigne, s'il est de l'équipe ; à défaut, toute
 * l'équipe. L'auditeur du modèle valait seul : une équipe différente ne voyait aucun point
 * (recette du 17 oct. 2026, observation 20).</p>
 */
@Service
@RequiredArgsConstructor
public class RepartitionDesPoints {

    private final AffectationPointRepository affectations;
    private final AuditService audits;
    private final AuditeurService auditeurs;
    private final PermissionChecker permissions;

    /** Les affectations posées sur l'audit : point → auditeur. */
    public Map<UUID, UUID> affectationsDe(Audit audit) {
        Map<UUID, UUID> parPoint = new HashMap<>();
        affectations.findAllByAuditId(audit.getId())
                .forEach(a -> parPoint.put(a.getPointControle().getId(), a.getAuditeur().getId()));
        return parPoint;
    }

    /** L'auditeur qui renseigne ce point sur cet audit ; nul si toute l'équipe le peut. */
    public UUID attribueA(Audit audit, PointControle point, Map<UUID, UUID> surLAudit) {
        UUID affecte = surLAudit.get(point.getId());
        if (affecte != null) {
            return affecte;
        }
        Auditeur duModele = point.getAuditeurAssigne();
        return duModele != null && audit.aDansLEquipe(duModele) ? duModele.getId() : null;
    }

    @Transactional(readOnly = true)
    public List<RepartitionPointDto> repartition(UUID auditId) {
        Audit audit = audits.sienOuRien(auditId);
        Map<UUID, UUID> surLAudit = affectationsDe(audit);
        return points(audit).map(p -> {
            UUID qui = attribueA(audit, p, surLAudit);
            String origine = surLAudit.containsKey(p.getId()) ? "audit" : qui != null ? "modèle" : null;
            return RepartitionPointDto.builder()
                    .pointId(p.getId())
                    .checklist(p.getChecklist().getNom())
                    .chapitreISO(p.getChapitreISO())
                    .libelle(p.getLibelle())
                    .auditeurId(qui)
                    .origine(origine)
                    .build();
        }).toList();
    }

    /**
     * Répartit les points de l'audit entre les membres de l'équipe. Un point sans auditeur
     * revient au modèle, ou à toute l'équipe. Réservé au responsable d'équipe et au valideur.
     */
    @Transactional
    public List<RepartitionPointDto> repartir(UUID auditId, List<RepartitionPointDto> saisie) {
        Audit audit = audits.sienOuRien(auditId);
        if (audit.getStatut() == null || audit.getStatut().estTermine()) {
            throw new BusinessException("L'audit " + audit.getReference() + " est terminé : ses points ne se"
                    + " répartissent plus.", HttpStatus.CONFLICT);
        }
        if (!permissions.detient("audit-validate")) {
            Auditeur moi = auditeurs.auditeurCourant();
            if (audit.getResponsableEquipe() == null || !audit.getResponsableEquipe().getId().equals(moi.getId())) {
                throw new BusinessException("Seul le responsable d'équipe répartit les points de l'audit.",
                        HttpStatus.FORBIDDEN);
            }
        }
        Map<UUID, PointControle> pointsDeLAudit = new HashMap<>();
        points(audit).forEach(p -> pointsDeLAudit.put(p.getId(), p));
        Map<UUID, Auditeur> equipe = new HashMap<>();
        Stream.concat(Stream.ofNullable(audit.getResponsableEquipe()), audit.getMembresEquipe().stream())
                .forEach(a -> equipe.put(a.getId(), a));

        affectations.deleteAllByAuditId(audit.getId());
        affectations.flush();
        for (RepartitionPointDto r : saisie == null ? List.<RepartitionPointDto>of() : saisie) {
            if (r.getAuditeurId() == null) {
                continue;
            }
            PointControle point = pointsDeLAudit.get(r.getPointId());
            Auditeur auditeur = equipe.get(r.getAuditeurId());
            if (point == null) {
                throw new BusinessException("Un point de contrôle désigné n'appartient pas aux checklists de "
                        + audit.getReference() + ".", HttpStatus.BAD_REQUEST);
            }
            if (auditeur == null) {
                throw new BusinessException("Le point « " + point.getLibelle() + " » est confié à un auditeur"
                        + " qui n'est pas de l'équipe.", HttpStatus.BAD_REQUEST);
            }
            affectations.save(AffectationPoint.builder().audit(audit).pointControle(point).auditeur(auditeur).build());
        }
        return repartition(auditId);
    }

    private static Stream<PointControle> points(Audit audit) {
        return audit.getChecklists().stream().flatMap(c -> c.getPoints().stream()).filter(Objects::nonNull);
    }
}
