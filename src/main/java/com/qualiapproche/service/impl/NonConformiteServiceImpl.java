package com.qualiapproche.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.qualiapproche.entities.*;
import com.qualiapproche.entities.mappers.*;
import com.qualiapproche.enumeration.Etat;
import com.qualiapproche.enumeration.Status;
import com.qualiapproche.enumeration.TypeDemande;
import com.qualiapproche.repository.*;
import com.qualiapproche.service.FichierService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import com.qualiapproche.service.NonConformiteService;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import com.qualiapproche.dto.NonConformiteDto;
import lombok.RequiredArgsConstructor;

import static com.qualiapproche.utils.UtilsClass.generateNumeroReferences;


@Service
@RequiredArgsConstructor
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
        nonConformite.setNumeroReference("NRF-" + generateNumeroReferences(6));
        nonConformite.setNiveauNonConformiteId(findNiveauNonConformiteById(dto.getNiveauNonConformiteId()));
        nonConformite.setActionId(findActionById(dto.getActionId()));
        nonConformite.setTypeNonConformiteId(findTypeNonConformiteById(dto.getTypeNonConformiteId()));
        nonConformite.setTypeProcessusId(findTypeProcessusById(dto.getTypeProcessusId()));
        nonConformite.setEtatTraitement(Etat.RECEPTION);
        nonConformite.setDateVisaEmetteur(LocalDateTime.now());
        nonConformite.setStatus(Status.PENDING);
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
                        planAction.setNonConformite(existingNonConformite); // Associer la NonConformité persistée
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
            if (dto.getEtatTraitement()==Etat.CLOTURE){
                existingNonConformite.setDateSuivi(LocalDateTime.now());
            }
            existingNonConformite.setDelaisMiseOeuvre(dto.getDelaisMiseOeuvre());
            dto.getParticipants().forEach(participant -> {
                existingNonConformite.getParticipants().getFullNames().add(participant);
            });
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
                            planAction.setNonConformite(existingNonConformite);
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
}