package com.qualiapproche.common.dto.auth;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 


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
