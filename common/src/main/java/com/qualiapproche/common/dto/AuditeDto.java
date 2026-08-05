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
public class AuditeDto extends AuditEntityDto {

    private String libelleAudite;
    private String descriptionAudite;
    private String resultatAudite;
    private String statutAudite;
    private String objectifAudite;
    private String typeAudite;
    private List<RisqueDto> risques;
    private List<NonConformiteDto> nonConformites;
}
