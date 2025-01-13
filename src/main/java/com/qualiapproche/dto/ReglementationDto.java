package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.Exigence;
import com.qualiapproche.entities.SuiviAuditInspection;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class ReglementationDto extends AuditEntityDto{

    private String nomReglementation;
    private String descriptionReglementation;
    private String organismeReglementation;
    @ManyToMany
    private List<Exigence> exigences;
    @ManyToMany
    private List<SuiviAuditInspection> suiviAuditInspections;
}
