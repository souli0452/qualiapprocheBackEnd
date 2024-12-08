package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.Fournisseur;

@Mapper(componentModel = "spring")
public interface FounisseurMapper {
    FournisseurDto toDto(Fournisseur fournisseur);

    Fournisseur toEntity(FournisseurDto fournisseurDto);

    List<FournisseurDto> toDtos(List<Fournisseur> fournisseurList);

    List<Fournisseur> toEntities(List<FournisseurDto> fournisseurDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(FournisseurDto enqueteDto, @MappingTarget Fournisseur fournisseur);
    default Fournisseur map(UUID id) {
        if (id == null) {
            return null;
        }
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(id);
        return fournisseur;
    }
}
