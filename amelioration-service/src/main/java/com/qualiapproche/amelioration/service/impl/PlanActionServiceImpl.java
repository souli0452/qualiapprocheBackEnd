package com.qualiapproche.amelioration.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.qualiapproche.amelioration.entities.*;

import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.common.utils.StatutEnum;
import com.qualiapproche.referentiel.entities.ConfigGlobal;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.ConfigGlobalRepository;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.common.service.SendMailService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private  final SendMailService sendMailService;
    private  final StructureRepository structureRepository;
    private  final ConfigGlobalRepository configGlobalRepository;
    private final PieceJointeService fichierService;
    @Override
    public void delete(UUID id) {
        PlanAction planAction=planActionRepository.getReferenceById(id);
        planActionRepository.delete(planAction);
    }

    @Override
    public PlanActionDto createPlanActionDto(PlanActionDto dto) throws IOException {
        PlanAction planAction = planActionMapper.toEntity(dto);

        NonConformite nonConformite = nonConformiteRepository.findById(dto.getNonConformeId())
                .orElseThrow(() -> new RuntimeException("Ce plan d'action n'est associé à aucune non-conformité!"));

        planAction.setNonConformeId(nonConformite.getId());  // Associer la NonConformité à PlanAction
        PlanActionDto result = planActionMapper.toDto((planAction));
        result.setFichiers(fichierService.getPjByEntityId(result.getId()));
        return result;
    }



    @Override
    public List<PlanActionDto> allPlanActions() {
        return planActionMapper.toDtos(planActionRepository.findAll()).stream()
                .peek(dto -> dto.setFichiers(fichierService.getPjByEntityId(dto.getId())))
                .toList();
    }

    @Override
    public List<PlanActionDto> planActionByResponsable(String responsable, StatutEnum statut) {
        return planActionMapper.toDtos(planActionRepository.findPlanActionsByResponsableEmailAndStatus(responsable, statut)).stream()
                .peek(dto -> dto.setFichiers(fichierService.getPjByEntityId(dto.getId())))
                .toList();
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
    public List<PlanActionDto> planActionByResponsableAll(String responsable) {
        return planActionMapper.toDtos(planActionRepository.findPlanActionsByResponsableEmail(responsable)).stream()
                .peek(dto -> dto.setFichiers(fichierService.getPjByEntityId(dto.getId())))
                .toList();
    }

    @Override
    public PlanActionDto changeStatus(PlanActionDto planActionDto) throws IOException {

        Optional<ConfigGlobal> configGlobal=configGlobalRepository.findAll().stream().findFirst();
        PlanAction planAction = planActionRepository.getReferenceById(planActionDto.getId());
        NonConformite nonConformite = nonConformiteRepository.getReferenceById(planActionDto.getNonConformeId());
        Structure structure=structureRepository.getReferenceById(UUID.fromString(nonConformite.getOrigineId()));
        String subject = "Traitement terminé – Non-conformité N°"+nonConformite.getNumeroReference()+" prête à être validée ";
        String object="Suivi qualité – Plan d’action réalisé par l’agent " +planAction.getResponsableNomComplet();
        String link = "https://sgq-quali.horeb.tech/page/validation";
        String linkPlan = "https://sgq-quali.horeb.tech/traitement-action/non-traiter";
        planAction.setStatus(planActionDto.getStatus());
        planAction.setObservation(planActionDto.getObservation());
        planAction.setCauseIdentifiees(planActionDto.getCauseIdentifiees());
        planAction.setSolutionRetenues(planActionDto.getSolutionRetenues());

        if (planActionDto.getStatus() == StatutEnum.TRAITER) {
            planAction.setDateTraitement(LocalDate.now());
        }

        if (planActionDto.getFichiers() != null) {
           fichierService.savePj(planActionDto.getFichiers(),planActionDto.getId());
        }
        PlanAction savedPlanAction = planActionRepository.save(planAction);
        //sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), object,linkPlan,"emailRqPlan",configGlobal.get().getNomCompletRq(),nonConformite.getNumeroReference(), planAction.getResponsableNomComplet());
        sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), object,linkPlan,"emailRqPlan",structure.getAutoriteSignataire(),nonConformite.getNumeroReference(), "");

        List<PlanAction> allPlans = planActionRepository.findPlanActionsByNonConformeId(nonConformite.getId());

        boolean allTreated = allPlans.stream()
                .allMatch(plan -> plan.getStatus() == StatutEnum.TRAITER);

        if (allTreated) {
            sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"validationAfterPlan",structure.getAutoriteSignataire(),nonConformite.getNumeroReference(), "");
        }
        PlanActionDto result = planActionMapper.toDto(savedPlanAction);
        result.setFichiers(fichierService.getPjByEntityId(result.getId()));
        return result;
    }

    @Override
    public PlanActionDto rejet(PlanActionDto dto) throws IOException {
        PlanAction planAction = planActionRepository.getReferenceById(dto.getId());
        planAction.setStatus(StatutEnum.REJECTED);
        planAction.setObservationRejet(dto.getObservationRejet());
        planAction.setDateRejet(dto.getDateRejet());
        String link = "https://sgq-quali.horeb.tech/page/traitement-actions/rejeter";
        String object = " Rejet de l’évaluation d’efficacité – Non-conformité "+planAction.getNumeroNc();
      List<PieceJointeDTO> fichiers=new ArrayList<>();
        if (dto.getDocRejet() != null) {
            fichiers.add(dto.getDocRejet());
         fichierService.savePj(fichiers,dto.getId());
        }
        sendMailService.sendMailToUserAfterDemandImputed(planAction.getResponsableEmail(), object,link,"rejectPlanAction",planAction.getResponsableNomComplet(),planAction.getNumeroNc(),planAction.getObservationRejet());

        PlanActionDto result = planActionMapper.toDto((planAction));
        result.setFichiers(fichierService.getPjByEntityId(result.getId()));
        return result;
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
            String subject = "Traitement du plan d'action N° ordre "+action.getNumeroOdre();
            String link = "https://sgq-quali.horeb.tech/traitement-action/non-traiter";

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
    private static final List<String> MOIS = List.of(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    );

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

