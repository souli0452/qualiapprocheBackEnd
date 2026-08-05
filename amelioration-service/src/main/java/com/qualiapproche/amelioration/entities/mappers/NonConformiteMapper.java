package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { ParticipantsMapper.class, PlanActionMapper.class })
public interface NonConformiteMapper extends EntityMapper<NonConformiteDto, NonConformite> {

    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    NonConformite toEntity(NonConformiteDto dto);

    /**
     * Recopie sur le dossier ce que la fiche a modifié.
     *
     * <p>Les actions correctives en sont exclues : la collection est en {@code orphanRemoval}, si
     * bien qu'une fiche qui les renvoie amputées — ou dépourvues de champs qu'elle n'affiche pas —
     * les supprime ou les vide. Elles ont leur propre service, qui sait ce qu'un engagement
     * interdit.</p>
     */
    @Override
    @Mapping(target = "fichiers", ignore = true)
    @Mapping(target = "docRejet", ignore = true)
    @Mapping(target = "planActions", ignore = true)
    void updateEntityFromDto(NonConformiteDto dto, @MappingTarget NonConformite entity);
}
