package com.qualiapproche.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Les pastilles de l'accueil pour le module documentaire : trois files, et leur somme.
 *
 * <p>Pendant chiffré de la cloche documentaire, qui elle rédige des phrases. Même construction que
 * {@link NcNotificationsResumeDto} : les files ne se recoupent pas, et le total en est la somme.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Les pastilles de l'accueil pour la gestion documentaire. Recalculées à "
        + "chaque appel et jamais conservées, comme la cloche dont elles sont le pendant chiffré.")
public class DocumentNotificationsResumeDto {

    @Schema(description = "Somme des trois files.", example = "4")
    private long totalAlertes;

    @Schema(description = "Documents sur lesquels le circuit ouvre une décision à l'appelant — "
            + "visa, vérification, approbation. C'est le moteur qui les désigne.",
            example = "2")
    private long documentsATraiter;

    @Schema(description = "Demandes de révision ou de suppression sur lesquelles le circuit ouvre "
            + "une décision à l'appelant.",
            example = "1")
    private long demandesAInstruire;

    @Schema(description = "Documents à portée de l'appelant dont la revue périodique est échue. Le "
            + "chiffre suit le drapeau posé par la surveillance, non un calcul fait à la lecture.",
            example = "1")
    private long enRetardRevision;
}
