package com.qualiapproche.common.messagerie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un courriel à remettre, déjà rendu : le service qui l'émet connaît son gabarit et ses
 * variables, le consommateur n'a plus qu'à l'envoyer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourrielAEnvoyer {
    private String destinataire;
    private String copie;
    private String sujet;
    /** Corps HTML. */
    private String corps;
    /** D'où vient le courriel (« publication-constats », « relance-plan »…) : pour le diagnostic. */
    private String origine;
}
