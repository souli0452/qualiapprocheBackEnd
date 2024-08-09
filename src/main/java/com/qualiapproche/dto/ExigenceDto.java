package com.qualiapproche.dto;

import com.qualiapproche.entities.ActionCorrectivePreventive;
import com.qualiapproche.entities.Audite;
import lombok.*;


import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ExigenceDto extends AuditEntityDto{

    private String libelleExigence;
    private String descriptionExigence;
    private String dateEcheanceExigence;
    private String statutConformite;
    private List<Audite> audites;
    private List<ActionCorrectivePreventive> actionCorrectivePreventives;
}
