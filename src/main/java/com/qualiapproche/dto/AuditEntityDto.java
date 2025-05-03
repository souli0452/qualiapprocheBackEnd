package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AuditEntityDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy 'à' HH:mm:ss")
    private LocalDateTime updateAt;
    private String createdById;
    private String updateById;
    private String currentUserfullName;
    private String currentUserEmail;
}
