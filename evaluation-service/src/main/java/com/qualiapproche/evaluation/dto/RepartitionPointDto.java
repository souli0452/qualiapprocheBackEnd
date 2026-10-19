package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Un point de contrôle d'un audit, et l'auditeur de l'équipe qui le renseigne. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionPointDto {
    private UUID pointId;
    private String checklist;
    private String chapitreISO;
    private String libelle;
    /** L'auditeur qui le renseigne ; nul : toute l'équipe peut le renseigner. */
    private UUID auditeurId;
    /** D'où vient l'affectation : « audit », « modèle », ou rien. Lecture seule. */
    private String origine;
}
