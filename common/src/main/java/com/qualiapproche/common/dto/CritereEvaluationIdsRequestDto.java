package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;







import java.util.List;
import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class CritereEvaluationIdsRequestDto {

    private List<UUID> critereEvaluationIds;

}
