package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.EfficaciteDto;
import com.qualiapproche.entities.Efficacite;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface EfficaciteMapper {
    EfficaciteDto toDto(Efficacite efficacite);

    Efficacite toEntity(EfficaciteDto efficaciteDto);

    List<EfficaciteDto> toDtos(List<Efficacite> efficacites);

    List<Efficacite> toEntities(List<EfficaciteDto> efficaciteDtos);
    // @Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(EfficaciteDto efficaciteDto, @MappingTarget Efficacite efficacite);
    default Efficacite map(UUID id) {
        if (id == null) {
            return null;
        }
        Efficacite efficacite = new Efficacite();
        efficacite.setId(id);
        return efficacite;
    }
}
