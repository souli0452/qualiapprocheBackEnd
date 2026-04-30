package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 




import java.util.List;
import java.util.UUID;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class CritereEvaluationIdsRequestDto {

    private List<UUID> critereEvaluationIds;

}
