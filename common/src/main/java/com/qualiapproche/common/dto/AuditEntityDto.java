package com.qualiapproche.common.dto;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


 
import com.fasterxml.jackson.annotation.JsonFormat;



import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class AuditEntityDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID id;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updateAt;
    private String createdById;
    private String updateById;
    private String currentUserfullName;
    private String currentUserEmail;
    private String currentUserStructure;


}
