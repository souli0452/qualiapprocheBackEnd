package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PieceJointeDTO extends AuditEntityDto {

    private String nom;
    private String ext;
    private String type;
    private String url;
    private UUID entityId;
    private byte[] fichier;
    private boolean zipFile;
}
