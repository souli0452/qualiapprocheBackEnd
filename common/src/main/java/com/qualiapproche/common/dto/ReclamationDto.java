package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 




import java.time.LocalDateTime;



@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class ReclamationDto extends AuditEntityDto{
    private String numeroReference;
    private String nomDemendeur;
}