package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.entities.ActionCorrectivePreventive;
import com.qualiapproche.entities.Audite;
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
public class ExigenceDto extends AuditEntityDto{

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statutConformite;
    private List<Audite> audites;
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;
}
