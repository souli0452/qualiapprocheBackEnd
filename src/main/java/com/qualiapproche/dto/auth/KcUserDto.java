package com.qualiapproche.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/11/26 à 14:29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KcUserDto {
    private String id;
    private Long createdTimestamp;
    private String username;
    private boolean enabled;
    private boolean emailVerified;
    private String firstName;
    private String structure;
    private String lastName;
    private String email;
    private String password;
    private String fonction;
    private String phoneNumber;
    private List<String> roles;

}
