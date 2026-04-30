package com.qualiapproche.userservice.entities.mappers;

import com.qualiapproche.common.dto.auth.KcRoleDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Component;

@Component
public class KcRoleMapper {

    public KcRoleDto toDto(RoleRepresentation role) {
        if (role == null) return null;
        KcRoleDto dto = new KcRoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }

    public RoleRepresentation toEntity(KcRoleDto dto) {
        if (dto == null) return null;
        RoleRepresentation role = new RoleRepresentation();
        role.setId(dto.getId());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        return role;
    }
}
