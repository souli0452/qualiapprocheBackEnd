package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigGlobalDto {
    private UUID id;
    private String nomCompletRq;
    private String emailRq;
    private int rappelEcheance;
}
