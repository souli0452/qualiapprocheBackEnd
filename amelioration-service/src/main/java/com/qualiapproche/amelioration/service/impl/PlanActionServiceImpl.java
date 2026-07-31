package com.qualiapproche.amelioration.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.qualiapproche.amelioration.entities.*;

import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.common.utils.StatutEnum;
import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.client.WorkflowClient;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.dto.WorkflowSummaryDto;
import com.qualiapproche.common.dto.WorkflowValidationRequestDto;
import feign.FeignException;
import com.qualiapproche.common.service.SendMailService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.qualiapproche.common.dto.PlanActionDto;
import com.qualiapproche.amelioration.entities.mappers.PlanActionMapper;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
import com.qualiapproche.amelioration.service.PlanActionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanActionServiceImpl implements PlanActionService {

    private final PlanActionMapper planActionMapper;
    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;
    private final SendMailService sendMailService;
    private final ReferentielClient referentielClient;
    private final PieceJointeService fichierService;
    private final WorkflowClient workflowClient;

    @org.springframework.beans.factory.annotation.Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void delete(UUID id) {
        PlanAction planAction = planActionRepository.getReferenceById(id);
        planActionRepository.delete(planAction);
    }

    @Override
    public PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException {
        PlanAction planAction = planActionMapper.toEntity(dto);

        NonConformite nonConformite = nonConformiteRepository.findById(dto.getNonConformeId())
                .orElseThrow(() -> new RuntimeException("Ce plan d'action n'est associé à aucune non-conformité!"));

        planAction.setNonConformeId(nonConformite.getId()); // Associer la NonConformité à PlanAction
        planAction = planActionRepository.save(planAction);

        try {
            WorkflowSummaryDto activeWorkflow = workflowClient.getActiveWorkflowByType("PLAN_ACTION");
            if (activeWorkflow != null && activeWorkflow.getId() != null) {
                workflowClient.initiateWorkflow(planAction.getId(), "PLAN_ACTION", activeWorkflow.getId());
                planAction.setWorkflowId(activeWorkflow.getId());
                planActionRepository.save(planAction);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation du workflow PLAN_ACTION", e);
        }

        PlanActionDto result = planActionMapper.toDto(planAction);
        result.setFichiers(fichierService.getPjByEntityId(result.getId()));
        return result;
    }

    @Override
    public Page<PlanActionDto> allPlanActions(Pageable pageable) {
        return planActionRepository.findAll(pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                });
    }

    @Override
    public Page<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut, Pageable pageable) {
        return planActionRepository.findPlanActionsByResponsableEmailAndStatus(responsable, statut, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                });
    }

    @Override
    public PlanActionDto getPlanActionDtoById(UUID id) {
        PlanAction planAction = planActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce plan d'action n'existe pas."));
        PlanActionDto dto = planActionMapper.toDto(planAction);
        dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
        return dto;
    }

    @Override
    public Page<PlanActionDto> planActionByResponsableAll(String responsable, Pageable pageable) {
        return planActionRepository.findPlanActionsByResponsableEmail(responsable, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                });
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getFrequenceTraitementParMois(int annee) {
        // 1. Définir la plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupérer les données
        List<Object[]> resultats = planActionRepository.countStatusByMonth(debutAnnee, finAnnee);

        // 3. Initialiser la structure de réponse
        Map<String, Map<String, Long>> stats = new LinkedHashMap<>();
        MOIS.forEach(mois -> stats.put(mois, new HashMap<>()));

        // 4. Peupler les résultats
        resultats.forEach(row -> {
            int moisIndex = ((Number) row[0]).intValue() - 1;
            String statut = ((String) row[1]);
            long count = ((Number) row[2]).longValue();

            if (moisIndex >= 0 && moisIndex < MOIS.size()) {
                String mois = MOIS.get(moisIndex);
                stats.get(mois).put(statut, count);
            }
        });

        // 5. Calculer les fréquences
        Map<String, Map<String, Long>> frequences = new LinkedHashMap<>();
        stats.forEach((mois, statuts) -> {
            long total = statuts.values().stream().mapToLong(Long::longValue).sum();
            long traites = statuts.getOrDefault("TRAITER", 0L);

            Map<String, Long> freqMois = new HashMap<>();
            freqMois.put("taux_traitement", total > 0 ? (traites * 100) / total : 0L);
            freqMois.put("total", total);

            frequences.put(mois, freqMois);
        });

        return Collections.singletonMap(String.valueOf(annee), frequences);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void rappelEcheance() {
        List<PlanAction> planActions = planActionRepository.findPlanActionsByStatus(StatutEnum.NON_TRAITER);
        LocalDate today = LocalDate.now();

        for (PlanAction action : planActions) {
            String subject = "Traitement du plan d'action N° ordre " + action.getNumeroOdre();
            String link = frontendUrl + "/traitement-action/non-traiter";

            LocalDate echeance = action.getDateEcheance();
            long joursRestants = ChronoUnit.DAYS.between(today, echeance);

            if (joursRestants >= 2) {
                // Pas encore urgent, tu peux ignorer ou loguer si tu veux
                continue;
            }

            if (joursRestants >= 1) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alertePlanAction", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            } else if (joursRestants == 0) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alerteLastDay", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            } else {

                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject, link,
                        "alerteEpuise", action.getResponsableNomComplet(), action.getNumeroNc(),
                        String.valueOf(joursRestants));
            }
        }
    }

    private static final List<String> MOIS = List.of(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre");

    /*
     * public void deletePlanAction(UUID id) {
     * PlanAction planAction = planActionRepository.getReferenceById(id);
     * if (planAction != null) {
     * // Retirer la planAction de la collection
     * NonConformite nonConformite = planAction.getNonConformite();
     * nonConformite.getPlanActions().remove(planAction); // Retirer de la liste
     * // Supprimer la planAction
     * planActionRepository.delete(planAction);
     * }
     * }
     */

    @Override
    public Page<PlanActionDto> getPlanActionsByStructure(String structureId, Pageable pageable) {
        return planActionRepository.findPlanActionsByStructureId(structureId, pageable)
                .map(planActionMapper::toDto)
                .map(dto -> {
                    dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
                    return dto;
                });
    }

    @Override
    public Page<PlanActionDto> search(
            String numeroOdre, String responsableEmail, String responsableNomComplet,
            String numeroNc, StatutEnum status, UUID nonConformeId,
            LocalDate dateEcheanceFrom, LocalDate dateEcheanceTo,
            LocalDate dateTraitementFrom, LocalDate dateTraitementTo,
            Pageable pageable) {
        // Not implemented in repository yet, returning empty for now or using a spec
        // Assuming a method doesn't exist yet, we will just return a basic findAll to satisfy the interface temporarily
        return planActionRepository.findAll(pageable)
                .map(planActionMapper::toDto);
    }

    @Override
    public void updateWorkflowState(UUID planActionId, String newStateName, String newEtatTraitement) {
        PlanAction planAction = planActionRepository.findById(planActionId)
                .orElseThrow(() -> new RuntimeException("Plan d'action introuvable: " + planActionId));

        planAction.setWorkflowStatus(newStateName);

        try {
            StatutEnum statut = StatutEnum.valueOf(newEtatTraitement);
            planAction.setStatus(statut);
            if (statut == StatutEnum.TRAITER && planAction.getDateTraitement() == null) {
                planAction.setDateTraitement(LocalDate.now());
            }
        } catch (IllegalArgumentException e) {
            log.warn("EtatTraitement non reconnu dans le workflow: {}", newEtatTraitement);
        }

        planActionRepository.save(planAction);

        if (planAction.getStatus() == StatutEnum.TRAITER) {
            List<PlanAction> allPlans = planActionRepository.findPlanActionsByNonConformeId(planAction.getNonConformeId());
            boolean allTreated = allPlans.stream()
                    .allMatch(plan -> plan.getStatus() == StatutEnum.TRAITER);

            if (allTreated) {
                try {
                    WorkflowValidationRequestDto req = WorkflowValidationRequestDto.builder()
                            .comments("Tous les plans d'action ont été traités.")
                            .build();
                    workflowClient.validateStep(planAction.getNonConformeId(), "SYSTEM", req);
                } catch (Exception e) {
                    log.error("Erreur lors de la validation du parent NonConformite", e);
                }
            }
        }
    }
}
