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

    /** Les écarts qui n'ont pas pu partir, chacun avec sa raison : ils restent à transmettre. */
    @Builder.Default
    private List<String> echecs = new ArrayList<>();
}
