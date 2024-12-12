package com.qualiapproche.entities.mappers;
import com.qualiapproche.dto.auth.KcRoleDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;

/**
 * @author :
 * <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/18 à 11:05
 */

@Mapper(componentModel = "spring")
public interface KcRoleMapper extends EntityMapper<KcRoleDto, RoleRepresentation> {

    /**
     * Maps a RoleRepresentation to a KcRoleDto.
     *
     * @param roleRepresentation the RoleRepresentation object.
     * @return the mapped KcRoleDto object.
     */
    KcRoleDto toDto(RoleRepresentation roleRepresentation);

    /**
     * Maps a KcRoleDto to a RoleRepresentation.
     *
     * @param kcRoleDto the KcRoleDto object.
     * @return the mapped RoleRepresentation object.
     */
    RoleRepresentation toEntity(KcRoleDto kcRoleDto);

    @Override
    default RoleRepresentation map(String id) {
        if (id == null) {
            return null;
        }
        RoleRepresentation roleRepresentation = new RoleRepresentation();
        roleRepresentation.setId(id);
        return roleRepresentation;
    }
}
