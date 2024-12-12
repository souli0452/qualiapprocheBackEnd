package com.qualiapproche.entities.mappers;

import com.qualiapproche.dto.auth.KcUserDto;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/18 à 11:05
 */

@Mapper(componentModel = "spring")
public interface KcUserMapper extends EntityMapper<KcUserDto, UserRepresentation> {

    KcUserDto toDto(UserRepresentation userRepresentation);

    UserRepresentation toEntity(KcUserDto kcUserDto);

    @Override
    default UserRepresentation map(String id) {
        if (id == null) {
            return null;
        }
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId(id);
        return userRepresentation;
    }
}
