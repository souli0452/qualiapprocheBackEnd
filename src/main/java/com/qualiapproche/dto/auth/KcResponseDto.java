package com.qualiapproche.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/25 à 14:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KcResponseDto {
    private String status;
    private String message;
    private Object data;
}
