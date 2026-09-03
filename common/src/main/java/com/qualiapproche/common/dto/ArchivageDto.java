package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "La mise à l'archive d'une pièce du référentiel, réduite à sa date. Ébauche : "
        + "aucun point d'entrée ne l'expose encore, et la pièce archivée n'y est pas désignée ; "
        + "seul l'audit hérité dit qui a archivé.")
public class ArchivageDto extends AuditEntityDto {
    @Schema(description = "Date et heure de la mise à l'archive, distincte de la date de création "
            + "héritée, qui est celle de l'enregistrement lui-même.")
    private LocalDateTime dateArchivage;
}
