package com.qualiapproche.dto;

import com.qualiapproche.entities.Exigence;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FormationDtao extends AuditEntityDto{

    private String libelleFormation;
    private String descriptionFormation;
    private String objectifFormation;
    private String prerequisFormation;
    private String compétenceAcquise;
    private List<Exigence> exigences;
}
