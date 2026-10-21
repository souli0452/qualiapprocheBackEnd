package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le rapport d'audit (D14), structuré pour que l'écran le mette en page section par section.
 *
 * <p>Seuls les constats <b>publiés</b> y entrent : un brouillon ou un constat en compilation ne
 * parle pas encore au nom de l'équipe. Même règle pour le RQAP-BF : les résultats officiels, sur
 * les critères publiés de la dernière évaluation validée.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RapportAuditDto {

    private AuditDto audit;
    private List<String> methodesAudit;
    private List<CompilationDto.Groupe> syntheseParNature;
    private List<ConstatRapport> constats;
    /** Les constats publiés dont la nature génère une non-conformité, avec leur dossier M05. */
    private List<ConstatRapport> ecarts;
    private Double scoreRqapbf;
    private MentionRQAPBFDto mentionRqapbf;
    private Map<String, Double> resultatsRqapbfParDomaine;
    private List<SignatureDto> signatures;

    /** Les preuves jointes aux constats publiés : les annexes les comptent, sans en inventer. */
    private long nombrePreuves;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstatRapport {
        private UUID id;
        private String pointControle;
        private String critere;
        private String nature;
        private String natureCouleur;
        private String observation;
        private LocalDateTime datePublication;
        private UUID nonConformiteId;
        /** Le numéro de la fiche NC ouverte, s'il a été retenu à la transmission. */
        private String numeroNonConformite;
        /** Le rédacteur du constat, nommé d'après l'annuaire. */
        private String auditeur;
    }
}
