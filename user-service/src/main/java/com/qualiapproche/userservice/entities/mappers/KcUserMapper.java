package com.qualiapproche.userservice.entities.mappers;

import com.qualiapproche.common.dto.auth.KcUserDto;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class KcUserMapper {

    public KcUserDto toDto(UserRepresentation user) {
        if (user == null) return null;
        KcUserDto dto = new KcUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(user.isEnabled() != null && user.isEnabled());
        dto.setEmailVerified(user.isEmailVerified() != null && user.isEmailVerified());
        dto.setCreatedTimestamp(user.getCreatedTimestamp());
        return dto;
    }

    public UserRepresentation toEntity(KcUserDto dto) {
        if (dto == null) return null;
        UserRepresentation user = new UserRepresentation();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEnabled(dto.isEnabled());
        user.setEmailVerified(dto.isEmailVerified());
        
        Map<String, java.util.List<String>> attributes = new HashMap<>();
        if (dto.getPhoneNumber() != null) {
            attributes.put("phoneNumber", Collections.singletonList(dto.getPhoneNumber()));
        }
        if (dto.getStructure() != null) {
            attributes.put("structure", Collections.singletonList(dto.getStructure()));
        }
        if (dto.getFonction() != null) {
            attributes.put("fonction", Collections.singletonList(dto.getFonction()));
        }
        user.setAttributes(attributes);
        
        return user;
    }
}
