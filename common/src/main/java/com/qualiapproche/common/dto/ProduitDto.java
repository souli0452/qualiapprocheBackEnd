package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 
import com.fasterxml.jackson.annotation.JsonIdentityInfo;

import com.fasterxml.jackson.annotation.ObjectIdGenerators;



import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor

@SuperBuilder

public class ProduitDto extends AuditEntityDto{
    private String libelleProduit;
    private String descriptionProduit;
    private List<AuditeDto> audites;

}
