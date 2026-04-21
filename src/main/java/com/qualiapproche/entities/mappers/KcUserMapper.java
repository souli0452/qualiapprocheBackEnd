package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.auth.KcUserDto;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mapper(componentModel = "spring")
public interface KcUserMapper extends EntityMapper<KcUserDto, UserRepresentation> {
    KcUserDto toDto(UserRepresentation userRepresentation);

    UserRepresentation toEntity(KcUserDto kcUserDto);

    @AfterMapping
    default void mapAttributesToDto(UserRepresentation entity, @MappingTarget KcUserDto dto) {
        Map<String, List<String>> attributes = entity.getAttributes();
        if (attributes != null) {
            if (attributes.containsKey("structure") && !attributes.get("structure").isEmpty()) {
                dto.setStructure(attributes.get("structure").get(0));
            }
            if (attributes.containsKey("fonction") && !attributes.get("fonction").isEmpty()) {
                dto.setFonction(attributes.get("fonction").get(0));
            }
            if (attributes.containsKey("phoneNumber") && !attributes.get("phoneNumber").isEmpty()) {
                dto.setPhoneNumber(attributes.get("phoneNumber").get(0));
            }
        }
    }

    @AfterMapping
    default void mapAttributesToEntity(KcUserDto dto, @MappingTarget UserRepresentation entity) {
        if (dto.getStructure() != null || dto.getFonction() != null || dto.getPhoneNumber() != null) {
            Map<String, List<String>> attributes = entity.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
                entity.setAttributes(attributes);
            }
            if (dto.getStructure() != null) {
                attributes.put("structure", Collections.singletonList(dto.getStructure()));
            }
            if (dto.getFonction() != null) {
                attributes.put("fonction", Collections.singletonList(dto.getFonction()));
            }
            if (dto.getPhoneNumber() != null) {
                attributes.put("phoneNumber", Collections.singletonList(dto.getPhoneNumber()));
            }
        }
    }


    @Override
    default UserRepresentation map(String id) {
        if (id == null) return null;
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(id);
        return userRepresentation;
    }
}
