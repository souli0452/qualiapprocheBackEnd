package com.qualiapproche.amelioration.entities.mappers;

import com.qualiapproche.common.base.Participants;
import org.mapstruct.Mapper;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ParticipantsMapper {
    default Participants map(Set<String> value) {
        if (value == null) {
            return null;
        }
        Participants participants = new Participants();
        participants.setFullNames(value);
        return participants;
    }

    default Set<String> map(Participants value) {
        if (value == null) {
            return null;
        }
        return value.getFullNames();
    }
}
