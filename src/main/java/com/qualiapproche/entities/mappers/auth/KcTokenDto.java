package com.qualiapproche.entities.mappers.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/25 à 15:15
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class KcTokenDto {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private Integer refreshExpiresIn;
    private String tokenType;
    private String sessionState;
    private String scope;
}
