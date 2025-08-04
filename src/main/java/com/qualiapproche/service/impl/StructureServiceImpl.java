
package com.qualiapproche.service.impl;

import com.qualiapproche.dto.StructureDto;
import com.qualiapproche.entities.Structure;
import com.qualiapproche.entities.mappers.StructureMapper;
import com.qualiapproche.enumeration.TypeStructure;
import com.qualiapproche.repository.StructureRepository;
import com.qualiapproche.service.StructureService;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@AllArgsConstructor
public class StructureServiceImpl implements StructureService {
    private final StructureRepository structureRepository;
    private final StructureMapper mapper;


    @Override
    public StructureDto saveStructure(StructureDto structureDto)
    {
         Structure structure = mapper.toEntity(structureDto);
         structure.setTitreHonorifiqueSignataire(structureDto.getTitreHonorifiqueSignataire());


        if ((isNull(structure.getId()) && structureRepository.existsByLibelleLong(structure.getLibelleLong())
                || nonNull(structure.getId()) && structureRepository.existsByLibelleLongAndIdNot(structure.getLibelleLong(), structureDto.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une structure avec le même nom exisite déjà.");
        }

        structure = structureRepository.save(structure);


        return mapper.toDto(structure);
    }

    @Override
    public StructureDto updateStructure(StructureDto structureDto) {
        if (isNull(structureDto.getId()) || !structureRepository.existsById(structureDto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Aucune structure avec cet ID : %s trouvée.", structureDto.getId()));
        }

        return saveStructure(structureDto);
    }


    @Override
    public StructureDto getStructureById(UUID directionId)  {
        Structure structure = structureRepository.findById(directionId).orElseThrow(() ->
                new NotFoundException("Aucune structure trouvée avec l'id: {}" + directionId));

        return mapper.toDto(structure);
    }

    @Override
    public String getStructureNameById(UUID directionId) throws NotFoundException {
        Structure structure = structureRepository.findById(directionId).orElseThrow(() ->
                new NotFoundException("Aucune structure trouvée avec l'id: {}" + directionId));

        return structure.getLibelleCourt();
    }





    @Override
    public List<StructureDto> getAllStructures(TypeStructure typeStructure, UUID directionId) {
        List<Structure> structures;

        if (isNull(typeStructure) && isNull(directionId)) {
            structures = structureRepository.findAll();
        } else if (isNull(typeStructure)) {
            structures = structureRepository.findAllByDirectionId(directionId);
        } else if (isNull(directionId)) {
            structures = structureRepository.findAllByTypeStructure(typeStructure);
        } else {
            structures = structureRepository.findAllByDirectionIdAndTypeStructure(directionId, typeStructure);
        }

        return structures.stream().map(mapper::toDto).toList();
    }

    @Override
    public List<StructureDto> getAllStructuresAll() {
        return structureRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public void deleteStructure(UUID id) {
        if (!structureRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Aucune structure avec cet ID : %s trouvée.", id));
        }
        structureRepository.deleteById(id);
    }

    @Override
    public StructureDto findStructureByLibelle(String libelle) {
        List<Structure> structures = structureRepository.findByLibelleCourt(libelle);
        if (structures.size() > 1) {
            return structureRepository.findByLibelleLong(libelle).map(mapper::toDto)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune structure trouvée avec le nom : " + libelle));
        } else if (structures.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune structure trouvée avec le nom : " + libelle);
        }

        return mapper.toDto(structures.get(0));
    }

}
