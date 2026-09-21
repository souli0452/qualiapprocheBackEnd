package com.qualiapproche.ia.client.vue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Ce que l'assistant retient d'un plan d'action.
 *
 * <p>Vue locale et volontairement étroite : le DTO du module vit chez lui, et ia-service n'a pas à
 * en dépendre pour trois champs. {@code ignoreUnknown} fait le reste — un champ ajouté là-bas ne
 * casse rien ici.</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanActionVue {

    private String id;
    private String numeroOdre;
    private String causeIdentifiees;
    private String dateEcheance;
    private String responsable;
    private String workflowStatus;
}
