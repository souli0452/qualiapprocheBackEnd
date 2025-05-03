package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.PrestataireDto;
import com.qualiapproche.entities.Prestataire;

@Mapper(componentModel = "spring")
public interface PrestataireMapper {
    PrestataireDto toDto(Prestataire prestataire);

    Prestataire toEntity(PrestataireDto prestataireDto);

    List<PrestataireDto> toDtos(List<Prestataire> prestataireList);

    List<Prestataire> toEntities(List<PrestataireDto> prestataireDtoDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(PrestataireDto enqueteDto, @MappingTarget Prestataire prestataire);
    default Prestataire map(UUID id) {
        if (id == null) {
            return null;
        }
        Prestataire prestataire = new Prestataire();
        prestataire.setId(id);
        return prestataire;
    }
}
