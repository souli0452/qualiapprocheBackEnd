package com.qualiapproche.amelioration.entities.mappers;


import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.common.dto.PieceJointeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PieceJointeMapper {

    PieceJointeDTO toDto(PieceJointe pieceJointe);
    @Mapping(target = "id", ignore = true)
    PieceJointe toEntity(PieceJointeDTO pieceJointeDTO);

    default PieceJointe map(UUID id) {
        if (id == null) {
            return null;
        }
        PieceJointe pieceJointe = new PieceJointe();
        pieceJointe.setId(id);
        return pieceJointe;
    }
}
