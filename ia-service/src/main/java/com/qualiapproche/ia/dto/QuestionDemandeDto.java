package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Poser une question prédéfinie, dans un fil existant ou dans un fil qui s'ouvre. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDemandeDto {

    /** Fil à poursuivre. Absent, un nouveau fil s'ouvre. */
    private UUID conversationId;
}
