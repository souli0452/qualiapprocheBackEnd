package com.qualiapproche.dto;

import com.qualiapproche.entities.Reclamation;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ActionCorrectivePreventiveDto extends AuditEntityDto {

    private String libelleActionCorrectivePreventive;
    private String descriptionActionCorrectivePreventive;
    private String responsable;
    private String statu;
    private String typeActionCorrectivePreventive;
    private LocalDateTime dateDebutActionCorrectivePreventive;
    private LocalDateTime dateFinActionCorrectivePreventive;
    private Reclamation reclamation;
}
