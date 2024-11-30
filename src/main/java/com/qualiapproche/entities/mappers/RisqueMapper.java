package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.RisqueDto;
import com.qualiapproche.entities.Risque;

@Mapper(componentModel = "spring")
public interface RisqueMapper {
    RisqueDto toDto(Risque risque);

    Risque toEntity(RisqueDto risqueDto);

    List<RisqueDto> toDtos(List<Risque> risques);

    List<Risque> toEntities(List<RisqueDto> risqueDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(RisqueDto risqueDto, @MappingTarget Risque risque);
    default Risque map(UUID id) {
        if (id == null) {
            return null;
        }
        Risque risque = new Risque();
        // Risque.setId(id);
        return risque;
    }
}
