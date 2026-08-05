package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;




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
