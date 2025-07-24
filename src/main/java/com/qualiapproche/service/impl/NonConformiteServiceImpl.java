package com.qualiapproche.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.qualiapproche.dto.NcStats;
import com.qualiapproche.dto.RejectNonConformiteDto;
import com.qualiapproche.entities.*;
import com.qualiapproche.entities.mappers.*;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import com.qualiapproche.enumeration.TypeDemande;
import com.qualiapproche.repository.*;
import com.qualiapproche.service.FichierService;
import com.qualiapproche.service.SendMailService;
import com.qualiapproche.utils.StatutEnum;
import com.qualiapproche.utils.UtilsClass;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.qualiapproche.service.NonConformiteService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import com.qualiapproche.dto.NonConformiteDto;
import lombok.RequiredArgsConstructor;

import static com.qualiapproche.utils.UtilsClass.generateNumeroReferences;


@Service
@RequiredArgsConstructor
@Slf4j
public class NonConformiteServiceImpl implements NonConformiteService {

    private final NonConformiteRepository nonConformiteRepository;
    private final NonConformiteMapper nonConformiteMapper;
    private final PlanActionMapper planActionMapper;
    private final TypeNonConformiteRepository typeNonConformiteRepository;
    private final TypeProcessusRepository typeProcessusRepository;
    private final EfficaciteRepository efficaciteRepository;
    private final ActionRepository actionRepository;
    private final NiveauNonConformiteRepository niveauNonConformiteRepository;
    private final FichierServiceImpl fichierServiceImpl;
    private final SendMailService sendMailService;
    private  final  StructureRepository structureRepository;
    private  final ConfigGlobalRepository configGlobalRepository;
    /**
     * Recherche les entités en base et renvoie une exception si l'ID est invalide.
     */
    private UUID findEfficaciteById(UUID id) {
        return id != null ? efficaciteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Efficacité non trouvée avec l'ID : " + id)).getId()
                : null;
    }

    private UUID findNiveauNonConformiteById(UUID id) {
        return id != null ? niveauNonConformiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Niveau de non-conformité non trouvé avec l'ID : " + id)).getId() : null;
    }

    private UUID findActionById(UUID id) {
        return id != null ? actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action non trouvée avec l'ID : " + id)).getId() : null;
    }

    private UUID findTypeNonConformiteById(UUID id) {
        return id != null ? typeNonConformiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de non-conformité non trouvé avec l'ID : " + id)).getId() : null;
    }

    private UUID findTypeProcessusById(UUID id) {
        return id != null ? typeProcessusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de processus non trouvé avec l'ID : " + id)).getId() : null;
    }

    /**
     * Crée une nouvelle NonConformité après validation.
     * @param dto Les données de la NonConformité.
     * @return Le NonConformiteDto correspondant.
     */
    @Override
    public NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException {
        NonConformite nonConformite = nonConformiteMapper.toEntity(dto);
        nonConformite.setTypeDemande(TypeDemande.NON_CONFORMITE);
        nonConformite.setVersion("1.0");
        nonConformite.setOriginNonConformiteLibelle(dto.getOriginNonConformiteLibelle());
        nonConformite.setNumeroReference(genererNumeroReference(dto.getOrigineServiceLibelleCourt()));
        nonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        nonConformite.setActionId(findActionById(dto.getActionId()));
        nonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        nonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        nonConformite.setEtatTraitement(Etat.SOUMISSION);
        nonConformite.setDateVisaEmetteur(LocalDateTime.now());
        nonConformite.setStatus(Status.DRAFT);
        nonConformite.setFichiers(fichierServiceImpl.convertBase64(dto.getFichiers()));

        // Sauvegarder la NonConformité avec ses PlanActions automatiquement persistées
        NonConformite savedNonConformite = nonConformiteRepository.save(nonConformite);
        // Retour DTO
        return nonConformiteMapper.toDto(savedNonConformite);
    }

    @Override
    public NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException {
        // Vérifier si la non-conformité existe
        NonConformite existingNonConformite = nonConformiteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Non-conformité non trouvée avec l'ID : " + id));
        // Mise à jour des champs modifiables
        existingNonConformite.setEfficaciteId(findEfficaciteById(dto.getEfficaciteId()));
        existingNonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        existingNonConformite.setActionId(findActionById(dto.getActionId()));
        existingNonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        existingNonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        existingNonConformite.setEtatTraitement(dto.getEtatTraitement());
        existingNonConformite.setUserImputId(dto.getUserImputId());
        existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
        // Mettre à jour les fichiers s'ils sont fournis
        if (dto.getFichiers() != null) {
            existingNonConformite.setFichiers(fichierServiceImpl.convertBase64(dto.getFichiers()));
        }
        if (dto.getPlanActions() != null && !dto.getPlanActions().isEmpty()) {
            // Crée les objets PlanAction et associe-les à la NonConformité
            List<PlanAction> planActions = dto.getPlanActions().stream()
                    .map(planActionDto -> {
                        PlanAction planAction = planActionMapper.toEntity(planActionDto);
                        planAction.setDateEcheance(planActionDto.getDateEcheance());
                        planAction.setStatus(planActionDto.getStatus());
                        planAction.setNumeroOdre(planActionDto.getNumeroOdre());
                        planAction.setNonConformeId(dto.getId()); // Associer la NonConformité persistée
                        return planAction;
                    }).collect(Collectors.toList());

            // Ajouter les PlanActions à la NonConformité
            existingNonConformite.setPlanActions(planActions);
        }
        // Sauvegarde de la mise à jour
        NonConformite updatedNonConformite = nonConformiteRepository.save(existingNonConformite);
        // Retour DTO
        return nonConformiteMapper.toDto(updatedNonConformite);
    }


    @Override
    public List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException {
        dtos.forEach(dto ->{
            NonConformite existingNonConformite = nonConformiteRepository.findById(dto.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Non-conformité non trouvée avec l'ID : " +dto.getId()));
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setJustificationPilote(dto.getJustificationPilote());
            existingNonConformite.setPertinancePilote(dto.getPertinancePilote());
            existingNonConformite.setJustificationRs(dto.getJustificationRs());
            existingNonConformite.setEfficaciteId(findEfficaciteById(dto.getEfficaciteId()));
            existingNonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
            existingNonConformite.setActionId(findActionById(dto.getActionId()));
            existingNonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
            existingNonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
            existingNonConformite.setEtatTraitement(dto.getEtatTraitement());
            existingNonConformite.setUserImputId(dto.getUserImputId());
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setStatus(dto.getStatus());
            existingNonConformite.setPertinanceRsSuivi(dto.getPertinanceRsSuivi());
            existingNonConformite.setNumeroFdac(dto.getNumeroFdac());
            Optional<ConfigGlobal> configGlobal=configGlobalRepository.findAll().stream().findFirst();
            Structure structure = structureRepository.getReferenceById(UUID.fromString(dto.getOrigineId()));
            if (dto.getEtatTraitement()==Etat.CLOTURE){
                existingNonConformite.setDateSuivi(LocalDateTime.now());
            }
            if (dto.getEtatTraitement()==Etat.TRAITEMENT){
                String subject = "Taitement d'une non conformité ";
                String link = "http://localhost:4200/page/traitement";
                sendMailService.sendMailToUserAfterDemandImputed(dto.getUserImputeEmail(), subject,link,"emailTemplate",dto.getUserImputFullName(),dto.getNumeroReference(),dto.getObservationRejet());

            }
            if (dto.getEtatTraitement()==Etat.IMPUTATION){
                String subject = "Non-conformité signalée – Action attendue de votre part ";
                String link = "http://localhost:4200/page/imputation";
                sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"structureToStructure",structure.getAutoriteSignataire(),dto.getNumeroReference(),dto.getStructureSoumissionLibelle());

            }
            if (dto.getEtatTraitement()==Etat.VALIDATION_RS){
                String subject = "Validation d'une non conformité ";
                String link = "http://localhost:4200/page/validation_rs";
                sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"validationRq",configGlobal.get().getNomCompletRq(),dto.getNumeroReference(),"");

            }
            existingNonConformite.setDelaisMiseOeuvre(dto.getDelaisMiseOeuvre());
            if (dto.getEtatTraitement()==Etat.VALIDATION){
                String subject = "Validation de la non-conformité N°"+dto.getNumeroReference();
                String link = "http://localhost:4200/page/validation";
                sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"validationNonConformite",structure.getAutoriteSignataire(),dto.getNumeroReference(),dto.getObservationRejet());

                dto.getParticipants().forEach(participant -> {
                existingNonConformite.getParticipants().getFullNames().add(participant);
            });
            }
            existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
            if (dto.getPlanActions() != null && !dto.getPlanActions().isEmpty()) {
                List<PlanAction> existingPlanActions = existingNonConformite.getPlanActions();
                if (existingPlanActions == null) {
                    existingPlanActions = new ArrayList<>();
                    existingNonConformite.setPlanActions(existingPlanActions);
                } else {
                    existingPlanActions.clear();
                }
                dto.getPlanActions().stream()
                        .map(planActionDto -> {

                            PlanAction planAction = planActionMapper.toEntity(planActionDto);
                            planAction.setNonConformeId(dto.getId());
                            planAction.setStatus(planActionDto.getStatus());
                            planAction.setNumeroOdre(planActionDto.getNumeroOdre());
                            planAction.setNumeroNc(dto.getNumeroReference());
                            planAction.setDateEcheance(planActionDto.getDateEcheance());
                            planAction.setDateEcheance(planActionDto.getDateEcheance());
                            planAction.setProcEmetteur(dto.getStructureSoumissionLibelle());
                            String subject = "Taitement d'une plan d'action ";
                            String link = "http://localhost:4200/page/imputation";
                            //sendMailService.sendMailToUserAfterDemandImputed(planAction.getResponsableEmail(), subject,link,"emailPlanAction");
                            return planAction;
                        })
                        .forEach(existingPlanActions::add);
            } else {
                if (existingNonConformite.getPlanActions() != null) {
                    existingNonConformite.getPlanActions().clear();
                }
            }
             nonConformiteRepository.save(existingNonConformite);
        } );
       return  dtos;
    }

    @Override
    public NonConformiteDto update(NonConformiteDto nonConformiteDto) {
        return nonConformiteRepository.findById(nonConformiteDto.getId()).map(nonConformiteExisted -> {
            nonConformiteMapper.updateEntityFromDto(nonConformiteDto, nonConformiteExisted);
            return nonConformiteMapper.toDto(nonConformiteRepository.save(nonConformiteExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune NonConformité trouvée."));
    }

    @Override
    public List<NonConformiteDto> allNonConformites() {
        return  nonConformiteMapper.toDtos(nonConformiteRepository.findAll()) ;
    }

    @Override
    public List<NonConformiteDto> findImupted(String userId,Etat etat) {
        return  nonConformiteMapper.toDtos(nonConformiteRepository.findByUserImputIdAndEtatTraitement(userId,etat)) ;
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findByEtatTraitement(etat));
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByEtatTraitementAndStructureSoumissionId(etat, uuid));
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByEtatTraitementAndOrigineId(etat, uuid));
    }

    @Override
    public NonConformiteDto getNonConformiteById(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            return nonConformiteMapper.toDto(nonConformiteRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette NonConformité n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            nonConformiteRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette NonConformité n'existe pas.");
        }
    }

    @Override
    public NonConformiteDto rejectNonConformite(RejectNonConformiteDto rejectNonConformiteDto) {
        NonConformite nonConformite = nonConformiteRepository.getReferenceById(rejectNonConformiteDto.getId());
        nonConformite.setEtatTraitement(rejectNonConformiteDto.getEtapeTraitement());
        nonConformite.setObservationRejet(rejectNonConformiteDto.getRejectReason());
        nonConformite.setStatus(Status.REJECTED);
        String subject = "Taitement d'une non conformité N°" + nonConformite.getNumeroReference();
        String link = "http://localhost:4200/page/imputation";
        Optional<ConfigGlobal> configGlobal=configGlobalRepository.findAll().stream().findFirst();
        if (rejectNonConformiteDto.getEtapeTraitement()==Etat.SOUMISSION){
            Structure structure = structureRepository.getReferenceById(UUID.fromString(nonConformite.getStructureSoumissionId()));
            sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"rejectNonConformite",structure.getAutoriteSignataire(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"rejectNonConformite",configGlobal.get().getNomCompletRq(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        }
        if (rejectNonConformiteDto.getEtapeTraitement()==Etat.TRAITEMENT){
            sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getUserImputeEmail(), subject,link,"rejectNonConformite",nonConformite.getUserImputFullName(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            //sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getUserImputeEmail(), subject,link,"rejectNonConformite",nonConformite.getUserImputFullName(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"rejectNonConformite",configGlobal.get().getNomCompletRq(), nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        }
        sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getCurrentUserEmail(), subject, link, "rejectNonConformite",nonConformite.getCurrentUserfullName(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"rejectNonConformite",configGlobal.get().getNomCompletRq(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        return nonConformiteMapper.toDto(nonConformiteRepository.save(nonConformite));
    }

    public void deleteMultiple(List<NonConformiteDto> nonConformiteDtos) {
        nonConformiteDtos.forEach(actualityDto -> {
            if (!nonConformiteRepository.existsById(actualityDto.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actualité invalide, impossible de supprimer.");
            }
            nonConformiteRepository.deleteById(actualityDto.getId());
            //fichierServiceImpl.removePjByEntity(actualityDto.getId(), this.pjDirectory);
        });

    }
    @Transactional(readOnly = true)
    public List<NcStats> getNcStats(String structureSoumissionId) {
        return nonConformiteRepository.countByStatusForStructure(structureSoumissionId);
    }
    public void changeStatus(UUID id, Status status) {
        log.debug("Request to change status of Actuality : {}", id);
        if (!nonConformiteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actualité invalide, impossible de changer le statut.");
        }
        NonConformite nc = nonConformiteRepository.getReferenceById(id);
        if (nc.getStatus() == Status.DRAFT) {
            nc.setPublicationDate(LocalDateTime.now());
            nc.setEtatTraitement(Etat.RECEPTION);
            String subject = "Taitement d'une non conformité N°" + nc.getNumeroReference();
            String link = "http://localhost:4200/page/reception";
            Structure structure = structureRepository.getReferenceById(UUID.fromString(nc.getStructureSoumissionId()));
            sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"validationNonConformite",structure.getAutoriteSignataire(),nc.getNumeroReference(),nc.getObservationRejet());
        }
        if (nc.getStatus() == Status.PUBLISHED) {
            nc.setArchivageDate(LocalDateTime.now());
        }
        nc.setStatus(status);
        nonConformiteRepository.save(nc);
    }


    public void changeManyStatus(List<NonConformiteDto> nonConformiteDtos, Status status) {
        nonConformiteDtos.forEach(act -> {
            if (!nonConformiteRepository.existsById(act.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actualité invalide, impossible de changer le statut.");
            }
            NonConformite nc = nonConformiteRepository.getReferenceById(act.getId());
            if (nc.getStatus() == Status.DRAFT) {
                nc.setPublicationDate(LocalDateTime.now());
                nc.setEtatTraitement(Etat.RECEPTION);
            }
            if (nc.getStatus() == Status.PUBLISHED) {

                nc.setArchivageDate(LocalDateTime.now());
            }
            nc.setStatus(status);
            nonConformiteRepository.save(nc);
        });

    }

    @Transactional(readOnly = true)
    public List<NonConformiteDto> findAll(final Status status, final  String structureSoumissionId) {
        log.debug("Request to get all Actualities");

        List<NonConformite> nonConformites;

        if (Objects.nonNull(status)) {
            nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,structureSoumissionId);
        } else {
            nonConformites = nonConformiteRepository.findAll();
        }

        return nonConformites.stream().map(actuality -> {
            NonConformiteDto nonConformiteDto = nonConformiteMapper.toDto(actuality);
         //   nonConformiteDto.setPieceJointes(pieceJointeService.getPjByEntity(actuality.getId(), this.pjDirectory));

            return nonConformiteDto;
        }).toList();
    }
    public String genererNumeroReference(String origineService) {
        final String prefix = "NFQT";
        final int annee = LocalDate.now().getYear();

        Integer dernierNumero = nonConformiteRepository.findDernierNumero(origineService, annee);
        int nouveauNumero = (dernierNumero != null) ? dernierNumero + 1 : 1;

        String numeroFormate = String.format("%05d", nouveauNumero);
        return String.format("%s-%s-%d-%s", prefix, origineService.toUpperCase(), annee, numeroFormate);
    }

}