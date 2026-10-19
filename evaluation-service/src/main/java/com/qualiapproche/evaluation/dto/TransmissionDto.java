package com.qualiapproche.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Le compte rendu d'une transmission d'écarts au module Non-conformités. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransmissionDto {

    private int transmis;

    /**
     * Les écarts encore à transmettre après ce passage, faute de temps : un passage en traite un
     * lot, l'écran relance les suivants. Zéro quand tout est parti ou a échoué.
     */
    private int restants;

    /** Les écarts qui n'ont pas pu partir, chacun avec sa raison : ils restent à transmettre. */
    @Builder.Default
    private List<String> echecs = new ArrayList<>();
}
