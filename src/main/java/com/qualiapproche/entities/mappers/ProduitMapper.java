package com.qualiapproche.entities.mappers;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.qualiapproche.dto.ProduitDto;
import com.qualiapproche.entities.Produit;

@Mapper(componentModel = "spring")
public interface ProduitMapper {
    ProduitDto toDto(Produit produit);

    Produit toEntity(ProduitDto produitDto);

    List<ProduitDto> toDtos(List<Produit> produitList);

    List<Produit> toEntities(List<ProduitDto> produitDtos);
    //@Mapping(target = "id", ignore = true) // Ignorer l'id pour éviter de le modifier
    void updateEntityFromDto(ProduitDto enqueteDto, @MappingTarget Produit produit);
    default Produit map(UUID id) {
        if (id == null) {
            return null;
        }
        Produit produit = new Produit();
        produit.setId(id);
        return produit;
    }
}
