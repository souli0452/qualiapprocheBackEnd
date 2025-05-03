package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.EfficaciteDto;
import com.qualiapproche.entities.Efficacite;

@Mapper(componentModel = "spring")
public interface EfficaciteMapper {
    EfficaciteDto toDto(Efficacite efficacite);

    Efficacite toEntity(EfficaciteDto efficaciteDto);

    List<EfficaciteDto> toDtos(List<Efficacite> efficacites);

    List<Efficacite> toEntities(List<EfficaciteDto> efficaciteDtos);
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "updateById", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdById", ignore = true)
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
