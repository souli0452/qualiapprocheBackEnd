package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.DepartementDto;
import com.qualiapproche.entities.Departement;

@Mapper(componentModel = "spring")
public interface DepartementMapper {
    DepartementDto toDto(Departement departement);

    Departement toEntity(DepartementDto departementDto);

    List<DepartementDto> toDtos(List<Departement> departements);

    List<Departement> toEntities(List<DepartementDto> departementDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(DepartementDto departementDto, @MappingTarget Departement departement);
    default Departement map(UUID id) {
        if (id == null) {
            return null;
        }
        Departement departement = new Departement();
         departement.setId(id);
        return departement;
    }
}
