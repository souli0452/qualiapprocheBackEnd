package com.qualiapproche.amelioration.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.qualiapproche.common.dto.*;
import com.qualiapproche.amelioration.entities.*;
import com.qualiapproche.amelioration.entities.mappers.*;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.amelioration.repository.*;
import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.common.utils.StatutEnum;
import com.qualiapproche.common.service.SendMailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import com.qualiapproche.amelioration.service.NonConformiteService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NonConformiteServiceImpl implements NonConformiteService {
    private final NonConformiteRepository nonConformiteRepository;
    private final NonConformiteMapper nonConformiteMapper;
    private final PlanActionMapper planActionMapper;
    private final TypeNonConformiteRepository typeNonConformiteRepository;
    private final ReferentielClient referentielClient;
    private final EfficaciteRepository efficaciteRepository;
    private final ActionRepository actionRepository;
    private final NiveauNonConformiteRepository niveauNonConformiteRepository;
    private final PieceJointeService fichierService;
    private final SendMailService sendMailService;
    private final PlanActionRepository planActionRepository;

    @org.springframework.beans.factory.annotation.Value("${frontend.url}")
    private String frontendUrl;

    /**
     * Recherche les entités en base et renvoie une exception si l'ID est invalide.
     */
    private UUID findEfficaciteById(UUID id) {
        return id;
    }

    private UUID findNiveauNonConformiteById(UUID id) {
        return id;
    }

    private UUID findActionById(UUID id) {
        return id;
    }

    private UUID findTypeNonConformiteById(UUID id) {
        return id;
    }

    private UUID findTypeProcessusById(UUID id) {
        return id;
    }

    private NonConformiteDto populateAttachments(NonConformiteDto dto) {
        if (dto == null)
            return null;
        dto.setFichiers(fichierService.getPjByEntityId(dto.getId()));
        if (dto.getPlanActions() != null) {
            dto.getPlanActions().forEach(plan -> plan.setFichiers(fichierService.getPjByEntityId(plan.getId())));
        }
        return dto;
    }

    /**
     * Crée une nouvelle NonConformité après validation.
     * 
     * @param dto Les données de la NonConformité.
     * @return Le NonConformiteDto correspondant.
     */
    @Override
    public NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException {
        NonConformite nonConformite = nonConformiteMapper.toEntity(dto);
        nonConformite.setTypeDemande(TypeDemande.NON_CONFORMITE);
        nonConformite.setVersion("1.0");
        nonConformite.setOriginNonConformiteLibelle(dto.getOriginNonConformiteLibelle());
        nonConformite.setNumeroReference(genererNumeroReference(dto.getStructureSoumissionId(), dto.getStructureSoumissionLibelle()));
        nonConformite.setStructureSoumissionId(dto.getStructureSoumissionId());
        nonConformite.setStructureSoumissionLibelle(dto.getStructureSoumissionLibelle());
        nonConformite.setOrigineId(null);
        nonConformite.setOrigineService(null);
        nonConformite.setOrigineServiceLibelleCourt(null);
        nonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        nonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        nonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        if (nonConformite.getEtatTraitement() != null){
            nonConformite.setEtatTraitement(nonConformite.getEtatTraitement());
        }else {
            nonConformite.setEtatTraitement(Etat.SOUMISSION);
        }
        if (nonConformite.getStatus() != null){
            nonConformite.setStatus(nonConformite.getStatus());
        }else {
            nonConformite.setStatus(Status.DRAFT);
        }
            nonConformite.setDateVisaEmetteur(LocalDateTime.now());


        // Sauvegarder la NonConformité avec ses PlanActions automatiquement persistées
        NonConformite savedNonConformite = nonConformiteRepository.save(nonConformite);
        fichierService.savePj(dto.getFichiers(), savedNonConformite.getId());

        return populateAttachments(nonConformiteMapper.toDto(savedNonConformite));
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
        if (dto.getEtatTraitement() == Etat.VALIDATION_RS) {
            if (dto.getCircuit() == null || dto.getOrigineId() == null ) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le circuit, la structure destination (origineId) et le type d'action sont obligatoires pour la validation RS.");
            }
        }
        existingNonConformite.setCircuit(dto.getCircuit());
        existingNonConformite.setOrigineId(dto.getOrigineId());
        existingNonConformite.setOrigineService(dto.getOrigineService());
        existingNonConformite.setOrigineServiceLibelleCourt(dto.getOrigineServiceLibelleCourt());
        existingNonConformite.setActionLibelle(dto.getActionLibelle());

        existingNonConformite.setUserImputId(dto.getUserImputId());
        existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
        // Mettre à jour les fichiers s'ils sont fournis
        if (dto.getFichiers() != null) {
            fichierService.deleteAllByEntityId(id);
            nonConformiteRepository.flush();
            fichierService.savePj(dto.getFichiers(), id);
        }
        if (dto.getPlanActions() != null && !dto.getPlanActions().isEmpty()) {

            List<PlanAction> planActions = dto.getPlanActions().stream()
                    .map(planActionDto -> {
                        PlanAction planAction = planActionMapper.toEntity(planActionDto);
                        planAction.setDateEcheance(planActionDto.getDateEcheance());
                        planAction.setStatus(planActionDto.getStatus());
                        planAction.setActionCorrective(planActionDto.getActionCorrective());
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
        return populateAttachments(nonConformiteMapper.toDto(updatedNonConformite));
    }

    @Override
    public List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException {
        dtos.forEach(dto -> {
            NonConformite existingNonConformite = nonConformiteRepository.findById(dto.getId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Non-conformité non trouvée avec l'ID : " + dto.getId()));
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setJustificationPilote(dto.getJustificationPilote());
            existingNonConformite.setPertinancePilote(dto.getPertinancePilote());
            existingNonConformite.setJustificationRs(dto.getJustificationRs());
            existingNonConformite.setEfficaciteId(findEfficaciteById(dto.getEfficaciteId()));
            existingNonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
            existingNonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
            existingNonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
            existingNonConformite.setEtatTraitement(dto.getEtatTraitement());
            if (dto.getEtatTraitement() == Etat.IMPUTATION) {
                if (dto.getCircuit() == null || dto.getOrigineId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le circuit, la structure destination (origineId) et le type d'action sont obligatoires pour la validation RS.");
                }
                existingNonConformite.setCircuit(dto.getCircuit());
                existingNonConformite.setOrigineId(dto.getOrigineId());
                existingNonConformite.setOrigineService(dto.getOrigineService());
                existingNonConformite.setOrigineServiceLibelleCourt(dto.getOrigineServiceLibelleCourt());
            }


          //  existingNonConformite.setActionLibelle(dto.getActionLibelle());
            existingNonConformite.setUserImputId(dto.getUserImputId());
            existingNonConformite.setUserImputeEmail(dto.getUserImputeEmail());
            existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
            existingNonConformite.setPertinanceRs(dto.getPertinanceRs());
            existingNonConformite.setStatus(dto.getStatus());
            existingNonConformite.setActionPreventive(dto.getActionPreventive());
            existingNonConformite.setPertinanceRsSuivi(dto.getPertinanceRsSuivi());
            existingNonConformite.setNumeroFdac(dto.getNumeroFdac());
            ConfigGlobalDto configGlobal = referentielClient.getConfigGlobal();
            StructureDto structure = null;
            if (dto.getOrigineId() != null) {
                structure = referentielClient.getStructureById(UUID.fromString(dto.getOrigineId()));
            }
            StructureDto structureSoumission = null;
            if (dto.getStructureSoumissionId() != null) {
                structureSoumission = referentielClient
                        .getStructureById(UUID.fromString(dto.getStructureSoumissionId()));
            }
            if (dto.getFichiers() != null) {
                fichierService.deleteAllByEntityId(dto.getId());
                nonConformiteRepository.flush();
                fichierService.savePj(dto.getFichiers(), dto.getId());
            }
            if (dto.getEtatTraitement() == Etat.CLOTURE) {
                existingNonConformite.setDateSuivi(LocalDateTime.now());
            }
            if (dto.getEtatTraitement() == Etat.TRAITEMENT) {
                String subject = "Taitement d'une non conformité ";
                String link = frontendUrl + "/traitement";
                sendMailService.sendMailToUserAfterDemandImputed(dto.getUserImputeEmail(), subject, link,
                        "emailTemplate", dto.getUserImputFullName(), dto.getNumeroReference(),
                        dto.getObservationRejet());

            }
            if (dto.getEtatTraitement() == Etat.IMPUTATION) {

                    String subject = "Non-conformité signalée – Action attendue de votre part ";
                    String link = frontendUrl + "/imputation";
                    if (structure != null) {
                        sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject, link,
                                "structureToStructure", structure.getAutoriteSignataire(), dto.getNumeroReference(),
                                dto.getStructureSoumissionLibelle());

                }
            }
            /*
             * if (dto.getEtatTraitement()==Etat.VALIDATION_PLAN){
             * String subject = "Non-conformité signalée – Action attendue de votre part ";
             * String link = frontendUrl + "/validation-plan";
             * sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(),
             * subject,link,"validationPlanRequise",structure.getAutoriteSignataire(),dto.
             * getNumeroReference(),dto.getCurrentUserfullName());
             * 
             * }
             */
            if (dto.getEtatTraitement() == Etat.VALIDATION_RS) {
                String subject = "Validation d'une non conformité ";
                String link = frontendUrl + "/validation_rs";
                if (configGlobal != null) {
                    sendMailService.sendMailToUserAfterDemandImputed(configGlobal.getEmailRq(), subject, link,
                            "validationRq", configGlobal.getNomCompletRq(), dto.getNumeroReference(), "");
                }
            }
            if (dto.getEtatTraitement() == Etat.SUIVI_RQ) {
                String subject = "Suivi d'une non conformité ";
                String link = frontendUrl + "/suivi_rq";
                if (configGlobal != null) {
                    sendMailService.sendMailToUserAfterDemandImputed(configGlobal.getEmailRq(), subject, link,
                            "validationRq", configGlobal.getNomCompletRq(), dto.getNumeroReference(), "");
                }
            }
            if (dto.getEtatTraitement() == Etat.CLOTURE) {
                String subject = "Cloture  non conformité ";
                String link = frontendUrl + "/consultation";
                if (structure != null) {
                    sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject, link,
                            "traitementReussi", structure.getAutoriteSignataire(), dto.getNumeroReference(),
                            dto.getStructureSoumissionLibelle());
                }
                if (structureSoumission != null) {
                    sendMailService.sendMailToUserAfterDemandImputed(structureSoumission.getEmail(), subject, link,
                            "traitementReussi", structureSoumission.getAutoriteSignataire(), dto.getNumeroReference(),
                            dto.getStructureSoumissionLibelle());
                }
            }
            existingNonConformite.setDelaisMiseOeuvre(dto.getDelaisMiseOeuvre());
            if (dto.getEtatTraitement() == Etat.VALIDATION) {
                String subject = "Validation de la non-conformité N°" + dto.getNumeroReference();
                String link = frontendUrl + "/validation";
                if (structure != null) {
                    sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject, link,
                            "validationNonConformite", structure.getAutoriteSignataire(), dto.getNumeroReference(),
                            dto.getObservationRejet());
                }
                if (!dto.getParticipants().isEmpty()) {
                    dto.getParticipants().forEach(participant -> {
                        existingNonConformite.getParticipants().getFullNames().add(participant);
                    });
                }
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
                            planAction.setNonConformeId(existingNonConformite.getId());
                            planAction.setStatus(planActionDto.getStatus());
                            planAction.setNumeroOdre(planActionDto.getNumeroOdre());
                            planAction.setNumeroNc(dto.getNumeroReference());
                            planAction.setSolutionRetenues(planActionDto.getSolutionRetenues());
                            planAction.setCauseIdentifiees(planActionDto.getCauseIdentifiees());
                            planAction.setDateEcheance(planActionDto.getDateEcheance());
                            planAction.setDateEcheance(planActionDto.getDateEcheance());
                            planAction.setProcEmetteur(dto.getStructureSoumissionLibelle());

                            return planAction;
                        })
                        .forEach(existingPlanActions::add);
            } else {
                if (existingNonConformite.getPlanActions() != null) {
                    existingNonConformite.getPlanActions().clear();
                }
            }
            nonConformiteRepository.save(existingNonConformite);
        });
        return dtos.stream().map(this::populateAttachments).toList();
    }

    @Override
    public NonConformiteDto update(NonConformiteDto nonConformiteDto) {
        return nonConformiteRepository.findById(nonConformiteDto.getId()).map(nonConformiteExisted -> {
            nonConformiteMapper.updateEntityFromDto(nonConformiteDto, nonConformiteExisted);
            return populateAttachments(nonConformiteMapper.toDto((nonConformiteExisted)));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune NonConformité trouvée."));
    }

    @Override
    public List<NonConformiteDto> allNonConformites() {
        List<NonConformite> allNonConformites = nonConformiteRepository.findAll();
        List<NonConformite> filteredNonConformites = allNonConformites.stream()
                .filter(nc -> nc.getStatus() != Status.DRAFT)
                .collect(Collectors.toList());

        return nonConformiteMapper.toDtos(filteredNonConformites).stream().map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> findImupted(String userId, Etat etat) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findByUserImputIdAndEtatTraitement(userId, etat))
                .stream().map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findByEtatTraitement(etat)).stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid) {
        return nonConformiteMapper
                .toDtos(nonConformiteRepository.findAllByEtatTraitementAndStructureSoumissionId(etat, uuid)).stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByStructure(String uuid) {
        List<NonConformite> list = nonConformiteRepository.findAllByOrigineId(uuid);
        Set<NonConformite> uniqueList = new LinkedHashSet<>(list);
        return nonConformiteMapper.toDtos(new ArrayList<>(uniqueList)).stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByEtatTraitementAndOrigineId(etat, uuid))
                .stream().map(this::populateAttachments).toList();
    }

    @Override
    public NonConformiteDto getNonConformiteById(UUID id) {
        if (nonConformiteRepository.existsById(id)) {
            return populateAttachments(nonConformiteMapper.toDto(nonConformiteRepository.getReferenceById(id)));
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette NonConformité n'existe pas.");
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
    public NonConformiteDto rejectNonConformite(RejectNonConformiteDto rejectNonConformiteDto) throws IOException {
        NonConformite nonConformite = nonConformiteRepository.getReferenceById(rejectNonConformiteDto.getId());
        nonConformite.setEtatTraitement(rejectNonConformiteDto.getEtapeTraitement());
        nonConformite.setObservationRejet(rejectNonConformiteDto.getRejectReason());
        nonConformite.setStatus(Status.REJECTED);
        /*
         * List<Fichier> fichiers=
                new ArrayList<>();
         * if (rejectNonConformiteDto.getDocRejet() != null) {
         * Fichier fichier=fichierMapper.toEntity(rejectNonConformiteDto.getDocRejet());
         * fichiers.add(fichier);
         * nonConformite.setDocRejet(fichierService.convertBase64(fichiers).stream().
         * findFirst().get());
         * 
         * }
         */
        String subject = "Taitement d'une non conformité N°" + nonConformite.getNumeroReference();

        ConfigGlobalDto configGlobal = referentielClient.getConfigGlobal();
        if (rejectNonConformiteDto.getEtapeTraitement() == Etat.SOUMISSION) {
            String link = frontendUrl + "/reception";
            try {
                if (nonConformite.getStructureSoumissionId() != null) {
                    StructureDto structure = referentielClient
                            .getStructureById(UUID.fromString(nonConformite.getStructureSoumissionId()));
                    if (structure != null) {
                        sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject, link,
                
                                "rejectNonConformite", structure.getAutoriteSignataire(),
                                nonConformite.getNumeroReference(), nonConformite.getObservationRejet());
                    }
                }
            } catch (Exception e) {
                log.error("Erreur envoi mail rejet : {}", e.getMessage());
            }
        }
        if (rejectNonConformiteDto.getEtapeTraitement() == Etat.TRAITEMENT) {
            String link = frontendUrl;
            sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getUserImputeEmail(), subject, link,
                    "rejectNonConformite", nonConformite.getUserImputFullName(), nonConformite.getNumeroReference(),
                    nonConformite.getObservationRejet());
        }
        String link = frontendUrl;
        sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getCurrentUserEmail(), subject, link,
                "rejectNonConformite", nonConformite.getCurrentUserfullName(), nonConformite.getNumeroReference(),
                nonConformite.getObservationRejet());
        return populateAttachments(nonConformiteMapper.toDto((nonConformite)));}

    public void deleteMultiple(List<NonConformiteDto> nonConformiteDtos) {
        nonConformiteDtos.forEach(actualityDto -> {
            if (!nonConformiteRepository.existsById(actualityDto.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Actualité invalide, impossible de supprimer.");
            }
            nonConformiteRepository.deleteById(actualityDto.getId());
            // fichierServiceImpl.removePjByEntity(actualityDto.getId(), this.pjDirectory);
        });

    }

    @Transactional(readOnly = true)
    public List<NcStats> getNcStats(String structureSoumissionId) {
        return nonConformiteRepository.countByStatusForStructure(structureSoumissionId);
    }

    public void changeStatus(UUID id, Status status) {
        log.debug("Request to change status of Actuality : {}", id);
        if (!nonConformiteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Actualité invalide, impossible de changer le statut.");
        }
        NonConformite nc = nonConformiteRepository.getReferenceById(id);
        if (nc.getStatus() == Status.DRAFT) {
            nc.setPublicationDate(LocalDateTime.now());
            nc.setEtatTraitement(Etat.RECEPTION);
            String subject = "Taitement d'une non conformité N°" + nc.getNumeroReference();
            try {
                if (nc.getStructureSoumissionId() != null) {
                    StructureDto structure = referentielClient
                            .getStructureById(UUID.fromString(nc.getStructureSoumissionId()));
                    if (structure != null) {
                        sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject, "",
                                "validationNonConformite", structure.getAutoriteSignataire(), nc.getNumeroReference(),
                                nc.getObservationRejet());
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi du mail de changement de statut : {}", e.getMessage());
            }
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Actualité invalide, impossible de changer le statut.");
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
    public List<NonConformiteDto> findAll(final Status status, final String structureSoumissionId) {
        log.debug("Request to get all Actualities");

        List<NonConformite> nonConformites;
        List<NonConformite> nonConformitesOthers;

        if (Objects.nonNull(status)) {
            if (status == Status.PUBLISHED) {
                nonConformitesOthers = nonConformiteRepository
                        .findAllByStatusAndStructureSoumissionId(Status.IN_PROGRESS, structureSoumissionId);
                nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,
                        structureSoumissionId);
                nonConformites.addAll(nonConformitesOthers);
            } else {
                nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,
                        structureSoumissionId);
            }

        } else {
            nonConformites = nonConformiteRepository.findAll();
        }

        return nonConformites.stream().map(actuality -> {
            NonConformiteDto nonConformiteDto = nonConformiteMapper.toDto(actuality);
            return populateAttachments(nonConformiteDto);
        }).toList();
    }

    @Override
    public List<NonConformiteDto> findAllByStructure(String structureSoumissionId) {
        List<NonConformite> nonConformites = nonConformiteRepository
                .findAllByOrigineIdAndStatusIsNot(structureSoumissionId, Status.DRAFT);
        List<NonConformite> nonConformitesS = nonConformiteRepository
                .findAllByStructureSoumissionIdAndStatusIsNot(structureSoumissionId, Status.DRAFT);
        
        Set<NonConformite> uniqueNonConformites = new LinkedHashSet<>(nonConformites);
        uniqueNonConformites.addAll(nonConformitesS);
        
        return nonConformiteMapper.toDtos(new ArrayList<>(uniqueNonConformites)).stream().map(this::populateAttachments).toList();
    }

    public String genererNumeroReference(String structureSoumissionId, String structureSoumissionLibelle) {
        final String prefix = "NFQT";
        final int annee = LocalDate.now().getYear();

        Integer dernierNumero = nonConformiteRepository.findDernierNumero(structureSoumissionId, annee);
        int nouveauNumero = (dernierNumero != null) ? dernierNumero + 1 : 1;

        String sigle = structureSoumissionLibelle;
        if (structureSoumissionId != null) {
            try {
                StructureDto structure = referentielClient.getStructureById(UUID.fromString(structureSoumissionId));
                if (structure != null && structure.getLibelleCourt() != null) {
                    sigle = structure.getLibelleCourt();
                }
            } catch (Exception e) {
                log.error("Error fetching structure sigle for reference generation: {}", e.getMessage());
            }
        }

        String numeroFormate = String.format("%05d", nouveauNumero);
        return String.format("%s-%s-%d-%s", prefix, sigle.toUpperCase().replaceAll("\\s+", "_"), annee, numeroFormate);
    }

    @Override
    public Map<String, Long> getNonConformiteStatsByStructure(int annee) {
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999999999);

        // 2. Appel du repository avec les dates précises
        return nonConformiteRepository
                .getNonConformiteStatsByStructureAndPeriod(debutAnnee, finAnnee)
                .stream()
                .collect(Collectors.toMap(
                        NonConformiteByStructDto::getOrigineServiceLibelleCourt,
                        NonConformiteByStructDto::getCount));
    }

    @Override
    public Map<String, Map<String, Long>> getStatsParAnnee(int annee) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Initialiser tous les mois à 0
        List<String> mois = Arrays.asList(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");
        mois.forEach(m -> stats.put(m, 0L));

        // Remplir avec les données de la base
        nonConformiteRepository.countByMonth(annee).forEach(row -> {
            int moisIndex = ((Number) row[0]).intValue() - 1;
            long count = ((Number) row[1]).longValue();
            stats.put(mois.get(moisIndex), count);
        });

        return Map.of(String.valueOf(annee), stats);
    }

    public Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee) {
        // 1. Plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Debug: Vérifier les données existantes
        log.debug("Vérification des statuts en base:");
        nonConformiteRepository.countStatusForDebug(debutAnnee, finAnnee)
                .forEach(row -> log.debug("Statut: {}, Count: {}", row[0], row[1]));

        // 3. Statuts à inclure (convertir les enums en String)
        List<String> statutsFiltres = Arrays.stream(Status.values())
                .filter(s -> Set.of(
                        Status.APPROVED,
                        Status.REJECTED,
                        Status.IN_PROGRESS).contains(s))
                .map(Enum::name)
                .collect(Collectors.toList());

        // 4. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            statutsFiltres.forEach(statut -> stats.put(statut, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 5. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndStatus(
                debutAnnee,
                finAnnee,
                statutsFiltres);

        log.debug("Résultats de la requête:");
        resultats.forEach(row -> log.debug("Mois: {}, Statut: {}, Count: {}", row[0], row[1], row[2]));

        // 6. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String statut = ((String) row[1]).toUpperCase();
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(statut, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);
    }

    @Override
    public NonConformiteDto getByNumeroRef(String numeroRef) {
        return populateAttachments(
                nonConformiteMapper.toDto(nonConformiteRepository.getNonConformiteByNumeroReference(numeroRef)));
    }

    private static final List<String> STATUTS = Arrays.asList(
            "APPROVED", "REJECTED", "IN_PROGRESS");

    private static final List<String> MOIS = Arrays.asList(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre");

    @Override
    public Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId) {
        // 1. Définir la plage temporelle exacte pour l'année
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupérer les données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndService(
                debutAnnee,
                finAnnee,
                origineServiceId);

        // 3. Initialiser la structure de réponse
        Map<String, Long> statsParMois = new LinkedHashMap<>();
        List<String> nomsMois = List.of(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        // Initialiser tous les mois à 0
        nomsMois.forEach(mois -> statsParMois.put(mois, 0L));

        // 4. Peupler les résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1; // Conversion 1-12 → 0-11
                long count = ((Number) row[1]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    statsParMois.put(nomsMois.get(moisIndex), count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne statistiques: {}", Arrays.toString(row), e);
            }
        });

        // 5. Structurer la réponse finale
        return Collections.singletonMap(String.valueOf(annee), statsParMois);
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee,
            String origineServiceId) {
        // 1. Plage temporelle
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Debug: Vérifier les données existantes
        log.debug("Vérification des statuts en base:");
        nonConformiteRepository.countStatusForDebug(debutAnnee, finAnnee)
                .forEach(row -> log.debug("Statut: {}, Count: {}", row[0], row[1]));

        // 3. Statuts à inclure (convertir les enums en String)
        List<String> statutsFiltres = Arrays.stream(Status.values())
                .filter(s -> Set.of(
                        Status.APPROVED,
                        Status.REJECTED,
                        Status.IN_PROGRESS).contains(s))
                .map(Enum::name)
                .collect(Collectors.toList());

        // 4. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            statutsFiltres.forEach(statut -> stats.put(statut, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 5. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndStatus(
                debutAnnee,
                finAnnee,
                statutsFiltres);

        log.debug("Résultats de la requête:");
        resultats.forEach(row -> log.debug("Mois: {}, Statut: {}, Count: {}", row[0], row[1], row[2]));

        // 6. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String statut = ((String) row[1]).toUpperCase();
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size()) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(statut, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);
    }

    @Override
    public ValidatePlanActionDto validatePlan(ValidatePlanActionDto validatePlanActionDto) {
        NonConformite nonConformite = nonConformiteRepository.findById(validatePlanActionDto.getNonConformiteId())
                .orElseThrow(() -> new EntityNotFoundException("Non-conformité introuvable"));

        for (UUID planId : validatePlanActionDto.getPlanIds()) {
            PlanAction planAction = planActionRepository.findById(planId)
                    .orElseThrow(() -> new EntityNotFoundException("Plan d'action introuvable : ID " + planId));

            planAction.setStatus(StatutEnum.NON_TRAITER);

            String subject = "Traitement d'un plan d'action";
            String link = frontendUrl + "/traitement-action/non-traiter";

            sendMailService.sendMailToUserAfterDemandImputed(
                    planAction.getResponsableEmail(),
                    subject,
                    link,
                    "emailPlanAction",
                    planAction.getResponsableNomComplet(),
                    nonConformite.getNumeroReference(),
                    "" // Observation vide pour l'instant
            );

            planActionRepository.save(planAction);
        }

        return validatePlanActionDto;
    }

    @Override
    public Map<String, Map<String, Map<String, Long>>> getStatsNiveauParAnnee(int annee, String origineServiceId) {
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupération de tous les niveaux existants
        List<String> tousNiveaux = niveauNonConformiteRepository.findAllLibelles();
        log.debug("Niveaux disponibles: {}", tousNiveaux);

        // 3. Initialisation de la structure de réponse
        Map<String, Map<String, Long>> statsMensuelles = new LinkedHashMap<>();
        List<String> nomsMois = List.of("janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre");

        nomsMois.forEach(mois -> {
            Map<String, Long> stats = new LinkedHashMap<>();
            tousNiveaux.forEach(niveau -> stats.put(niveau, 0L));
            statsMensuelles.put(mois, stats);
        });

        // 4. Récupération des données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndNiveau(
                debutAnnee,
                finAnnee,
                origineServiceId);

        // 5. Traitement des résultats
        resultats.forEach(row -> {
            try {
                int moisIndex = ((Number) row[0]).intValue() - 1;
                String niveau = (String) row[1];
                long count = ((Number) row[2]).longValue();

                if (moisIndex >= 0 && moisIndex < nomsMois.size() && tousNiveaux.contains(niveau)) {
                    String mois = nomsMois.get(moisIndex);
                    statsMensuelles.get(mois).put(niveau, count);
                }
            } catch (Exception e) {
                log.error("Erreur traitement ligne: {}", Arrays.toString(row), e);
            }
        });

        return Map.of(String.valueOf(annee), statsMensuelles);

    }

    @Override
    public List<NonConformiteDto> findAllByInitiator(String userId) {
        return nonConformiteRepository.findAllByCreatedById(userId).stream()
                .map(nonConformiteMapper::toDto)
                .map(this::populateAttachments)
                .collect(Collectors.toList());
    }

    @Override
    public List<NonConformiteDto> findByUser(String userId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByCreatedById(userId))
                .stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> findImputedByUser(String userId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByUserImputId(userId))
                .stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> findArchivedByUser(String userId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByCreatedByIdAndStatus(userId, Status.ARCHIVED))
                .stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public NcCountsDto getCountsByUser(String userId) {
        return NcCountsDto.builder()
                .brouillons(nonConformiteRepository.countByCreatedByIdAndStatus(userId, Status.DRAFT))
                .imputees(nonConformiteRepository.countByUserImputId(userId))
                .archives(nonConformiteRepository.countByCreatedByIdAndStatus(userId, Status.ARCHIVED))
                .build();
    }

    @Override
    public List<NonConformiteDto> findByStructure(String structureId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByStructureSoumissionIdOrOrigineId(structureId, structureId))
                .stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public List<NonConformiteDto> findByStructureAllUsers(String structureId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByCurrentUserStructure(structureId)).stream()
                .map(this::populateAttachments).toList();
    }

    @Override
    public NcDashboardDto getDashboardRQ() {
        List<NonConformite> all = nonConformiteRepository.findAll();
        return buildDashboardDto(all);
    }

    @Override
    public NcDashboardDto getDashboardPilot(String structureId) {
        List<NonConformite> all = nonConformiteRepository.findAllByStructureSoumissionIdOrOrigineId(structureId, structureId);
        return buildDashboardDto(all);
    }

    @Override
    public NcDashboardDto getDashboardUser(String userId) {
        List<NonConformite> all = nonConformiteRepository.findAllByUserInvolved(userId);
        return buildDashboardDto(all);
    }

    private NcDashboardDto buildDashboardDto(List<NonConformite> ncs) {
        Map<Status, Long> statsByStatus = ncs.stream()
                .filter(nc -> nc.getStatus() != null)
                .collect(Collectors.groupingBy(NonConformite::getStatus, Collectors.counting()));

        Map<Status, Map<String, Long>> statsByStatusAndGravity = ncs.stream()
                .filter(nc -> nc.getStatus() != null && nc.getNiveauNonConformiteLibelle() != null)
                .collect(Collectors.groupingBy(
                        NonConformite::getStatus,
                        Collectors.groupingBy(NonConformite::getNiveauNonConformiteLibelle, Collectors.counting())
                ));

        return NcDashboardDto.builder()
                .totalNC(ncs.size())
                .statsByStatus(statsByStatus)
                .statsByStatusAndGravity(statsByStatusAndGravity)
                .build();
    }

    @Override
    public NcEvolutionDto getNcEvolutionStats(int annee, Integer mois, String structureId) {
        if (structureId != null && (structureId.trim().isEmpty() || "null".equalsIgnoreCase(structureId.trim()))) {
            structureId = null;
        }

        List<String> tousNiveaux = niveauNonConformiteRepository.findAllLibelles();
        if (tousNiveaux == null) {
            tousNiveaux = new ArrayList<>();
        }

        long totalEvolution;
        double pct = 0.0;
        List<String> labels;
        List<NcEvolutionDto.DatasetDto> datasets = new ArrayList<>();
        Map<String, Long> gravityCounts = new LinkedHashMap<>();

        for (String libelle : tousNiveaux) {
            gravityCounts.put(libelle, 0L);
        }

        if (mois == null) {
            totalEvolution = nonConformiteRepository.countTotalByYear(annee, structureId);
            long countPrev = nonConformiteRepository.countTotalByYear(annee - 1, structureId);

            if (countPrev > 0) {
                pct = ((double) (totalEvolution - countPrev) / countPrev) * 100.0;
            } else if (totalEvolution > 0) {
                pct = 100.0;
            }

            labels = List.of("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Jul", "Août", "Sep", "Oct", "Nov", "Déc");

            for (String libelle : tousNiveaux) {
                datasets.add(NcEvolutionDto.DatasetDto.builder()
                        .label(libelle)
                        .data(new ArrayList<>(Collections.nCopies(12, 0L)))
                        .build());
            }

            List<Object[]> results = nonConformiteRepository.getEvolutionStatsByYear(annee, structureId);
            for (Object[] row : results) {
                if (row[0] == null || row[1] == null || row[2] == null) continue;
                int moisVal = ((Number) row[0]).intValue();
                String gravite = (String) row[1];
                long count = ((Number) row[2]).longValue();

                int moisIdx = moisVal - 1;
                if (moisIdx >= 0 && moisIdx < 12) {
                    for (NcEvolutionDto.DatasetDto dataset : datasets) {
                        if (dataset.getLabel().equals(gravite)) {
                            dataset.getData().set(moisIdx, count);
                            break;
                        }
                    }
                    if (gravityCounts.containsKey(gravite)) {
                        gravityCounts.put(gravite, gravityCounts.get(gravite) + count);
                    }
                }
            }
        } else {
            totalEvolution = nonConformiteRepository.countTotalByMonth(annee, mois, structureId);
            long countPrev;
            if (mois > 1) {
                countPrev = nonConformiteRepository.countTotalByMonth(annee, mois - 1, structureId);
            } else {
                countPrev = nonConformiteRepository.countTotalByMonth(annee - 1, 12, structureId);
            }

            if (countPrev > 0) {
                pct = ((double) (totalEvolution - countPrev) / countPrev) * 100.0;
            } else if (totalEvolution > 0) {
                pct = 100.0;
            }

            labels = List.of("Semaine 1", "Semaine 2", "Semaine 3", "Semaine 4");

            for (String libelle : tousNiveaux) {
                datasets.add(NcEvolutionDto.DatasetDto.builder()
                        .label(libelle)
                        .data(new ArrayList<>(Collections.nCopies(4, 0L)))
                        .build());
            }

            List<Object[]> results = nonConformiteRepository.getEvolutionStatsByMonth(annee, mois, structureId);
            for (Object[] row : results) {
                if (row[0] == null || row[1] == null || row[2] == null) continue;
                int semaineVal = ((Number) row[0]).intValue();
                String gravite = (String) row[1];
                long count = ((Number) row[2]).longValue();

                int semaineIdx = Math.min(4, semaineVal) - 1;
                if (semaineIdx >= 0 && semaineIdx < 4) {
                    for (NcEvolutionDto.DatasetDto dataset : datasets) {
                        if (dataset.getLabel().equals(gravite)) {
                            long existing = dataset.getData().get(semaineIdx);
                            dataset.getData().set(semaineIdx, existing + count);
                            break;
                        }
                    }
                    if (gravityCounts.containsKey(gravite)) {
                        gravityCounts.put(gravite, gravityCounts.get(gravite) + count);
                    }
                }
            }
        }

        String pourcentageEvolution;
        if (pct > 0.0) {
            pourcentageEvolution = String.format(Locale.US, "+%.1f", pct);
        } else if (pct < 0.0) {
            pourcentageEvolution = String.format(Locale.US, "%.1f", pct);
        } else {
            pourcentageEvolution = "0.0";
        }

        List<NcEvolutionDto.GravityCountDto> gravitesBreakdown = new ArrayList<>();
        for (Map.Entry<String, Long> entry : gravityCounts.entrySet()) {
            gravitesBreakdown.add(NcEvolutionDto.GravityCountDto.builder()
                    .nom(entry.getKey())
                    .count(entry.getValue())
                    .build());
        }

        NcEvolutionDto.ChartDataDto chartData = NcEvolutionDto.ChartDataDto.builder()
                .labels(labels)
                .datasets(datasets)
                .build();

        return NcEvolutionDto.builder()
                .totalEvolution(totalEvolution)
                .pourcentageEvolution(pourcentageEvolution)
                .gravites(gravitesBreakdown)
                .chartData(chartData)
                .build();
    }

    @Override
    public List<NonConformiteDto> getNonConformitesByNiveau(UUID niveauId) {
        return nonConformiteMapper.toDtos(nonConformiteRepository.findAllByNiveauNonConformiteId(niveauId)).stream()
                .map(this::populateAttachments)
                .collect(Collectors.toList());
    }
}
