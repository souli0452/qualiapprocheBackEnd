package com.qualiapproche.common.dto.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/25 à 15:01
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class KcLoginRequestDto {
    private String username;
    private String password;
    private String refreshToken;
}
