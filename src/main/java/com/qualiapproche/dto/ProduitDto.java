package com.qualiapproche.dto;

import com.qualiapproche.entities.Audite;
import lombok.*;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProduitDto extends AuditEntityDto{
    private String libelleProduit;
    private String descriptionProduit;
    private List<Audite> audites;

}
