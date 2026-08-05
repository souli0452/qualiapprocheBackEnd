package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;







import java.util.List;



@Data
@AllArgsConstructor
@NoArgsConstructor

@SuperBuilder

public class ProduitDto extends AuditEntityDto {
    private String libelleProduit;
    private String descriptionProduit;
    private List<AuditeDto> audites;

}
