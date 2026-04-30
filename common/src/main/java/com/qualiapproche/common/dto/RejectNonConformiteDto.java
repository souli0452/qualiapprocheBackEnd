package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 

import com.qualiapproche.common.enumeration.Etat;



import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class RejectNonConformiteDto {
    private UUID id;
    private String rejectReason;
    private Etat etapeTraitement;
    //FichierDto docRejet;
}
