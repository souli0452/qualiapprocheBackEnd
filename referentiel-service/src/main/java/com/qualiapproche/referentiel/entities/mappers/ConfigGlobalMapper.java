package com.qualiapproche.referentiel.entities.mappers;

import com.qualiapproche.common.dto.ConfigGlobalDto;
import com.qualiapproche.referentiel.entities.ConfigGlobal;
import org.springframework.stereotype.Component;

@Component
public class ConfigGlobalMapper {

    public ConfigGlobalDto toDto(ConfigGlobal entity) {
        if (entity == null) {
            return null;
        }
        return ConfigGlobalDto.builder()
                .id(entity.getId())
                .nomCompletRq(entity.getNomCompletRq())
                .emailRq(entity.getEmailRq())
                .rappelEcheance(entity.getRappelEcheance())
                .build();
    }

    public ConfigGlobal toEntity(ConfigGlobalDto dto) {
        if (dto == null) {
            return null;
        }
        return ConfigGlobal.builder()
                .id(dto.getId())
                .nomCompletRq(dto.getNomCompletRq())
                .emailRq(dto.getEmailRq())
                .rappelEcheance(dto.getRappelEcheance())
                .build();
    }
}
