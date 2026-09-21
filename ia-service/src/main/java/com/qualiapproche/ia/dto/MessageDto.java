package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.RoleMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un message du fil, tel que l'écran le réaffiche. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    private RoleMessage role;
    private String contenu;
    private int rang;
    private LocalDateTime createdAt;
}
