package com.qualiapproche.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

        planAction.setNonConformite(nonConformite);  // Associer la NonConformité à PlanAction
        return planActionMapper.toDto(planActionRepository.save(planAction));
    }



    @Override
    public List<PlanActionDto> allPlanActions() {
        return  planActionMapper.toDtos(planActionRepository.findAll()) ;
    }

    @Override
    public PlanActionDto getPlanActionDtoById(UUID id) {
        if (planActionRepository.existsById(id)) {
            return planActionMapper.toDto(planActionRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce plan d'action n'existe pas.");
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

