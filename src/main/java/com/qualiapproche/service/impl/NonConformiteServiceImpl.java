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
import com.qualiapproche.dto.NonConformiteByStructDto;
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
@Transactional
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
    private  final  FichierMapper fichierMapper;
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
            existingNonConformite.setUserImputeEmail(dto.getUserImputeEmail());
            existingNonConformite.setUserImputFullName(dto.getUserImputFullName());
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
                String link = "https://sgq-quali.horeb.tech/page/traitement";
                sendMailService.sendMailToUserAfterDemandImputed(dto.getUserImputeEmail(), subject,link,"emailTemplate",dto.getUserImputFullName(),dto.getNumeroReference(),dto.getObservationRejet());

            }
            if (dto.getEtatTraitement()==Etat.IMPUTATION){
                String subject = "Non-conformité signalée – Action attendue de votre part ";
                String link = "https://sgq-quali.horeb.tech/page/imputation";
                sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"structureToStructure",structure.getAutoriteSignataire(),dto.getNumeroReference(),dto.getStructureSoumissionLibelle());

            }
            if (dto.getEtatTraitement()==Etat.VALIDATION_RS){
                String subject = "Validation d'une non conformité ";
                String link = "https://sgq-quali.horeb.tech/page/validation_rs";
                sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"validationRq",configGlobal.get().getNomCompletRq(),dto.getNumeroReference(),"");

            }
            if (dto.getEtatTraitement()==Etat.SUIVI_RQ){
                String subject = "Suivi d'une non conformité ";
                String link = "https://sgq-quali.horeb.tech/page/suivi_rq";
                sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"validationRq",configGlobal.get().getNomCompletRq(),dto.getNumeroReference(),"");

            }
            existingNonConformite.setDelaisMiseOeuvre(dto.getDelaisMiseOeuvre());
            if (dto.getEtatTraitement()==Etat.VALIDATION){
                String subject = "Validation de la non-conformité N°"+dto.getNumeroReference();
                String link = "https://sgq-quali.horeb.tech/page/validation";
                sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"validationNonConformite",structure.getAutoriteSignataire(),dto.getNumeroReference(),dto.getObservationRejet());
                if (!dto.getParticipants().isEmpty()){
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
                            String subject = "Taitement d'une plan d'action ";
                            String link = "https://sgq-quali.horeb.tech/traitement-action/non-traiter";
                            if (dto.getEtatTraitement()==Etat.TRAITEMENT){
                                sendMailService.sendMailToUserAfterDemandImputed(planActionDto.getResponsableEmail(), subject,link,"emailPlanAction",planActionDto.getResponsableNomComplet(),dto.getNumeroReference(),"");
                            }

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
        List<NonConformite> allNonConformites = nonConformiteRepository.findAll();
        List<NonConformite> filteredNonConformites = allNonConformites.stream()
                .filter(nc -> nc.getStatus() !=Status.DRAFT)
                .collect(Collectors.toList());

        return nonConformiteMapper.toDtos(filteredNonConformites);
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
    public NonConformiteDto rejectNonConformite(RejectNonConformiteDto rejectNonConformiteDto) throws IOException {
        NonConformite nonConformite = nonConformiteRepository.getReferenceById(rejectNonConformiteDto.getId());
        nonConformite.setEtatTraitement(rejectNonConformiteDto.getEtapeTraitement());
        nonConformite.setObservationRejet(rejectNonConformiteDto.getRejectReason());
        nonConformite.setStatus(Status.REJECTED);
        List<Fichier> fichiers=new ArrayList<>();
        if (rejectNonConformiteDto.getDocRejet() != null) {
            Fichier fichier=fichierMapper.toEntity(rejectNonConformiteDto.getDocRejet());
               fichiers.add(fichier);
            nonConformite.setDocRejet(fichierServiceImpl.convertBase64(fichiers).stream().findFirst().get());

        }
        String subject = "Taitement d'une non conformité N°" + nonConformite.getNumeroReference();

        Optional<ConfigGlobal> configGlobal=configGlobalRepository.findAll().stream().findFirst();
        if (rejectNonConformiteDto.getEtapeTraitement()==Etat.SOUMISSION){
            String link = "https://sgq-quali.horeb.tech/page/reception";
            Structure structure = structureRepository.getReferenceById(UUID.fromString(nonConformite.getStructureSoumissionId()));
            sendMailService.sendMailToUserAfterDemandImputed(structure.getEmail(), subject,link,"rejectNonConformite",structure.getAutoriteSignataire(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"rejectNonConformite",configGlobal.get().getNomCompletRq(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        }
        if (rejectNonConformiteDto.getEtapeTraitement()==Etat.TRAITEMENT){
            String link = "https://sgq-quali.horeb.tech";
            sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getUserImputeEmail(), subject,link,"rejectNonConformite",nonConformite.getUserImputFullName(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            //sendMailService.sendMailToUserAfterDemandImputed(nonConformite.getUserImputeEmail(), subject,link,"rejectNonConformite",nonConformite.getUserImputFullName(),nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
            sendMailService.sendMailToUserAfterDemandImputed(configGlobal.get().getEmailRq(), subject,link,"rejectNonConformite",configGlobal.get().getNomCompletRq(), nonConformite.getNumeroReference(),nonConformite.getObservationRejet());
        }
        String link = "https://sgq-quali.horeb.tech";
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
            String link = "https://sgq-quali.horeb.tech/page/reception";
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
        List<NonConformite> nonConformitesOthers;

        if (Objects.nonNull(status)) {
            if (status == Status.PUBLISHED) {
                nonConformitesOthers = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(Status.IN_PROGRESS,structureSoumissionId);
                nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,structureSoumissionId);
                nonConformites.addAll(nonConformitesOthers);
            }else {
                nonConformites = nonConformiteRepository.findAllByStatusAndStructureSoumissionId(status,structureSoumissionId);
            }

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
                        NonConformiteByStructDto::getCount
                ));
    }
    @Override
    public Map<String, Map<String, Long>> getStatsParAnnee(int annee) {
        Map<String, Long> stats = new LinkedHashMap<>();

        // Initialiser tous les mois à 0
        List<String> mois = Arrays.asList(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre"
        );
        mois.forEach(m -> stats.put(m, 0L));

        // Remplir avec les données de la base
        nonConformiteRepository.countByMonth(annee).forEach(row -> {
            int moisIndex = ((Number)row[0]).intValue() - 1;
            long count = ((Number)row[1]).longValue();
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
                        Status.IN_PROGRESS
                ).contains(s))
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
                statutsFiltres
        );

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
        return nonConformiteMapper.toDto(nonConformiteRepository.getNonConformiteByNumeroReference(numeroRef));
    }

    private static final List<String> STATUTS = Arrays.asList(
            "APPROVED", "REJECTED", "IN_PROGRESS"
    );

    private static final List<String> MOIS = Arrays.asList(
            "janvier", "février", "mars", "avril", "mai", "juin",
            "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    );
    @Override
    public Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId) {
        // 1. Définir la plage temporelle exacte pour l'année
        LocalDateTime debutAnnee = LocalDateTime.of(annee, 1, 1, 0, 0, 0);
        LocalDateTime finAnnee = LocalDateTime.of(annee, 12, 31, 23, 59, 59, 999_999_999);

        // 2. Récupérer les données
        List<Object[]> resultats = nonConformiteRepository.countByMonthAndService(
                debutAnnee,
                finAnnee,
                origineServiceId
        );

        // 3. Initialiser la structure de réponse
        Map<String, Long> statsParMois = new LinkedHashMap<>();
        List<String> nomsMois = List.of(
                "janvier", "février", "mars", "avril", "mai", "juin",
                "juillet", "août", "septembre", "octobre", "novembre", "décembre"
        );

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
    public Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee, String origineServiceId) {
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
                        Status.IN_PROGRESS
                ).contains(s))
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
                statutsFiltres
        );

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
}