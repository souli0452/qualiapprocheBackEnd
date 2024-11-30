package com.qualiapproche.dto;

import com.qualiapproche.entities.ActionCorrectivePreventive;

import jakarta.persistence.ManyToMany;
import lombok.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Risque;

import jakarta.persistence.MappedSuperclass;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class RisqueDto extends  AuditEntityDto {
    private String libelle;
    private String description;
    private String niveau;
    private Risque.StatutRisque statut;
    private String plantAttenuation;
    private String commentaireRisque;
    private String evidenceRisque;
    // private LocalDateTime dateIdentificationRisque;
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;


}
