package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.PieceJointeDto;
import com.qualiapproche.entities.PieceJointe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;


@Mapper(componentModel = "spring")
public interface PieceJointeMapper extends EntityMapper<PieceJointeDto, PieceJointe> {

    @Mapping(target = "fichier", ignore = true)
    PieceJointeDto toDto(PieceJointe pieceJointe);
    PieceJointe toEntity(PieceJointeDto pieceJointeDto);

    default PieceJointe map(UUID id) {
        if (id == null) {
            return null;
        }
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setId(id);
        return pieceJointe;
    }
}
