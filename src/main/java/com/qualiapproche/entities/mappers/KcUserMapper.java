package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.auth.KcUserDto;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface KcUserMapper extends EntityMapper<KcUserDto, UserRepresentation> {
    @Mapping(target = "structure", expression = "java(getStructure(userRepresentation))")
    KcUserDto toDto(UserRepresentation userRepresentation);

    UserRepresentation toEntity(KcUserDto kcUserDto);

    default String getStructure(UserRepresentation userRepresentation) {
        if (userRepresentation.getAttributes() == null) return null;
        List<String> structureValues = userRepresentation.getAttributes().get("structure");
        return (structureValues != null && !structureValues.isEmpty()) ? structureValues.get(0) : null;
    }

    @Override
    default UserRepresentation map(String id) {
        if (id == null) return null;
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(id);
        return userRepresentation;
    }
}
