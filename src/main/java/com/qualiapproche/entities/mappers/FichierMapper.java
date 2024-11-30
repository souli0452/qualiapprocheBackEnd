package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.FichierDto;
import com.qualiapproche.entities.Fichier;

@Mapper(componentModel = "spring")
public interface FichierMapper {
    FichierDto toDto(Fichier fichier);

    Fichier toEntity(FichierDto fichierDto);

    List<FichierDto> toDtos(List<Fichier> fichierList);

    List<Fichier> toEntities(List<FichierDto> fichierDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(FichierDto enqueteDto, @MappingTarget Fichier fichier);
    default Fichier map(UUID id) {
        if (id == null) {
            return null;
        }
        Fichier fichier = new Fichier();
        // fichier.setId(id);
        return fichier;
    }
}
