package com.qualiapproche.ia.client.vue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Ce que l'assistant retient d'un document qualité. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentVue {

    private String id;
    private String documentNumber;
    private String titre;
    private String documentType;
    private String serviceLibelle;
    private boolean enRetardRevision;
}
