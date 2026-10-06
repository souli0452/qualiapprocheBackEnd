package com.qualiapproche.evaluation.service;

import com.qualiapproche.evaluation.dto.ChecklistAuditDto;
import com.qualiapproche.evaluation.dto.PointControleDto;
import com.qualiapproche.evaluation.entities.ChecklistAudit;
import com.qualiapproche.evaluation.entities.PointControle;
import com.qualiapproche.evaluation.enumeration.StatutChecklist;
import com.qualiapproche.evaluation.repository.ChecklistAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Les grilles de points de contrôle : rédigées, publiées, archivées, dupliquées. */
@Service
@RequiredArgsConstructor
public class ChecklistAuditService {

    private static final String QUOI = "checklist";

    private final ChecklistAuditRepository depot;
    private final AuditeurService auditeurs;

    @Transactional(readOnly = true)
    public List<ChecklistAuditDto> lister() {
        return depot.findAllByDirectionIdOrderByCreatedAtAsc(Perimetre.exigerUneDirection())
                .stream().map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public ChecklistAuditDto lire(UUID id) {
        return versDto(sienneOuRien(id));
    }

    @Transactional
    public ChecklistAuditDto creer(ChecklistAuditDto saisie) {
        Perimetre.exigerUneDirection();
        ChecklistAudit checklist = ChecklistAudit.builder().statut(StatutChecklist.BROUILLON).build();
        recopier(saisie, checklist);
        return versDto(depot.save(checklist));
    }

    @Transactional
    public ChecklistAuditDto modifier(UUID id, ChecklistAuditDto saisie) {
        ChecklistAudit checklist = sienneOuRien(id);
        checklist.exigerModifiable();
        recopier(saisie, checklist);
        return versDto(depot.save(checklist));
    }

    @Transactional
    public ChecklistAuditDto publier(UUID id) {
        ChecklistAudit checklist = sienneOuRien(id);
        checklist.publier();
        return versDto(depot.save(checklist));
    }

    @Transactional
    public ChecklistAuditDto archiver(UUID id) {
        ChecklistAudit checklist = sienneOuRien(id);
        checklist.archiver();
        return versDto(depot.save(checklist));
    }

    @Transactional
    public ChecklistAuditDto dupliquer(UUID id) {
        return versDto(depot.save(sienneOuRien(id).dupliquer()));
    }

    /** Un brouillon se supprime ; une checklist publiée s'archive, des constats y renvoyant peut-être. */
    @Transactional
    public void supprimer(UUID id) {
        ChecklistAudit checklist = sienneOuRien(id);
        checklist.exigerModifiable();
        depot.delete(checklist);
    }

    private ChecklistAudit sienneOuRien(UUID id) {
        return Perimetre.sienneOuRien(depot, id, QUOI);
    }

    /**
     * Les points, par identifiant : un point repris garde son identité — des constats
     * pourraient le citer —, un nouveau est créé, un absent est retiré.
     */
    private void recopier(ChecklistAuditDto saisie, ChecklistAudit cible) {
        cible.setNom(saisie.getNom().strip());
        cible.setChapitreISO(saisie.getChapitreISO());
        Map<UUID, PointControle> existants = cible.getPoints().stream()
                .collect(Collectors.toMap(PointControle::getId, p -> p));
        List<PointControle> retenus = new ArrayList<>();
        for (PointControleDto p : saisie.getPoints() == null ? List.<PointControleDto>of() : saisie.getPoints()) {
            PointControle point = p.getId() == null ? null : existants.get(p.getId());
            if (point == null) {
                point = PointControle.builder().checklist(cible).build();
            }
            point.setChapitreISO(p.getChapitreISO());
            point.setLibelle(p.getLibelle());
            point.setQuestion(p.getQuestion());
            point.setPreuveAttendue(p.getPreuveAttendue());
            point.setAuditeurAssigne(p.getAuditeurAssigneId() == null ? null
                    : auditeurs.sienOuRien(p.getAuditeurAssigneId()));
            retenus.add(point);
        }
        cible.getPoints().clear();
        cible.getPoints().addAll(retenus);
    }

    private ChecklistAuditDto versDto(ChecklistAudit c) {
        return ChecklistAuditDto.builder()
                .id(c.getId())
                .nom(c.getNom())
                .chapitreISO(c.getChapitreISO())
                .statut(c.getStatut())
                .points(c.getPoints().stream().map(p -> PointControleDto.builder()
                        .id(p.getId())
                        .chapitreISO(p.getChapitreISO())
                        .libelle(p.getLibelle())
                        .question(p.getQuestion())
                        .preuveAttendue(p.getPreuveAttendue())
                        .auditeurAssigneId(p.getAuditeurAssigne() == null ? null : p.getAuditeurAssigne().getId())
                        .build()).toList())
                .build();
    }
}
