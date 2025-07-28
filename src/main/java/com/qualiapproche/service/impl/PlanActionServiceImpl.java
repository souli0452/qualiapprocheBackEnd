package com.qualiapproche.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.qualiapproche.entities.Structure;
import com.qualiapproche.service.FichierService;
import com.qualiapproche.service.SendMailService;
import com.qualiapproche.utils.StatutEnum;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.dto.PlanActionDto;
import com.qualiapproche.entities.NonConformite;
import com.qualiapproche.entities.PlanAction;
import com.qualiapproche.entities.mappers.PlanActionMapper;
import com.qualiapproche.repository.NonConformiteRepository;
import com.qualiapproche.repository.PlanActionRepository;
import com.qualiapproche.service.PlanActionService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class PlanActionServiceImpl implements PlanActionService {

    private final PlanActionMapper planActionMapper;
    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;
    private  final SendMailService sendMailService;
    private  final FichierServiceImpl fichierServiceImpl;

    @Override
    public void delete(UUID id) {
        PlanAction planAction=planActionRepository.getReferenceById(id);
        planActionRepository.delete(planAction);
    }

    @Override
    public PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException {
        PlanAction planAction = planActionMapper.toEntity(dto);

        NonConformite nonConformite = nonConformiteRepository.findById(dto.getNonConformiteID())
                .orElseThrow(() -> new RuntimeException("Ce plan d'action n'est associé à aucune non-conformité!"));

        planAction.setNonConformeId(nonConformite.getId());  // Associer la NonConformité à PlanAction
        return planActionMapper.toDto(planActionRepository.save(planAction));
    }



    @Override
    public List<PlanActionDto> allPlanActions() {
        return  planActionMapper.toDtos(planActionRepository.findAll()) ;
    }

    @Override
    public List<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut) {
        return planActionMapper.toDtos(planActionRepository.findPlanActionsByResponsableEmailAndStatus(responsable,statut)).stream()
                .peek(planActionDto -> {


                }).toList();
    }

    @Override
    public PlanActionDto getPlanActionDtoById(UUID id) {
        if (planActionRepository.existsById(id)) {
            return planActionMapper.toDto(planActionRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce plan d'action n'existe pas.");
        }
    }

    @Override
    public List<PlanActionDto> planActionByResponsableAll(String responsable) {
        return planActionMapper.toDtos(planActionRepository.findPlanActionsByResponsableEmail(responsable)).stream()
                .peek(planActionDto -> {

                }).toList();
    }

    @Override
    public PlanActionDto changeStatus(PlanActionDto planActionDto) throws IOException {
       PlanAction planAction=planActionRepository.getReferenceById(planActionDto.getId());
       planAction.setStatus(planActionDto.getStatus());
       planAction.setObservation(planActionDto.getObservation());
       planAction.setCauseIdentifiees(planActionDto.getCauseIdentifiees());
       planAction.setSolutionRetenues(planActionDto.getSolutionRetenues());
       if (planActionDto.getStatus()==StatutEnum.TRAITER){
           planAction.setDateTraitement(LocalDate.now());
       }
       if (planActionDto.getFichiers() != null) {
           planAction.setFichiers(fichierServiceImpl.convertBase64(planActionDto.getFichiers()));

       }
       return planActionMapper.toDto(planActionRepository.save(planAction));
    }


    @Scheduled(cron = "0 0 0 * * *")
    public void rappelEcheance() {
        List<PlanAction> planActions = planActionRepository.findAll();
        LocalDate today = LocalDate.now();

        for (PlanAction action : planActions) {
            String subject = "Traitement du plan d'action N° ordre "+action.getNumeroOdre();
            String link = "http://localhost:4200/traitement-action/non-traiter";

            LocalDate echeance = action.getDateEcheance();
            long joursRestants = ChronoUnit.DAYS.between(today, echeance);

            if (joursRestants >= 2) {
                // Pas encore urgent, tu peux ignorer ou loguer si tu veux
                continue;
            }

            if (joursRestants >= 1) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject,link,"alertePlanAction",action.getResponsableNomComplet(),action.getNumeroNc(), String.valueOf(joursRestants));
            } else if (joursRestants == 0) {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject,link,"alerteLastDay",action.getResponsableNomComplet(),action.getNumeroNc(), String.valueOf(joursRestants));
            } else {
                sendMailService.sendMailToUserAfterDemandImputed(action.getResponsableEmail(), subject,link,"alerteEpuise",action.getResponsableNomComplet(),action.getNumeroNc(), String.valueOf(joursRestants));
            }
        }
    }


    /*public void deletePlanAction(UUID id) {
        PlanAction planAction = planActionRepository.getReferenceById(id);
        if (planAction != null) {
            // Retirer la planAction de la collection
            NonConformite nonConformite = planAction.getNonConformite();
            nonConformite.getPlanActions().remove(planAction); // Retirer de la liste
            // Supprimer la planAction
            planActionRepository.delete(planAction);
        }
    }*/
}

