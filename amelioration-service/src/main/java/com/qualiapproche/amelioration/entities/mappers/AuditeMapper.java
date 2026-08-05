package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.amelioration.entities.Audite;
import com.qualiapproche.common.base.Participants;
import com.qualiapproche.common.dto.AuditeDto;
import com.qualiapproche.common.mappers.EntityMapper;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
@Mapper(componentModel = "spring")
public interface AuditeMapper extends EntityMapper<AuditeDto, Audite> {

    // 1. Pour convertir l'entité vers le DTO (Extraction)
    default Set<String> map(Participants value) {
        if (value == null || value.getFullNames() == null) {
            return Collections.emptySet();
        }
        return value.getFullNames();
    }

    // 2. Pour convertir le DTO vers l'entité (Re-création)
    default Participants map(Set<String> names) {
        if (names == null) {
            return null;
        }
        Participants participants = new Participants();
        participants.setFullNames(new HashSet<>(names));
        return participants;
    }
}
