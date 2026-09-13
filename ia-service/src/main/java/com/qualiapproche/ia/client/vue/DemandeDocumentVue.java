package com.qualiapproche.ia.client.vue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Ce que l'assistant retient d'une demande portant sur un document. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemandeDocumentVue {

    private String id;
    private String documentNumber;
    private String documentTitre;
    private String type;
    private String etat;
    private String objectif;
    private String demandeurNom;
}
