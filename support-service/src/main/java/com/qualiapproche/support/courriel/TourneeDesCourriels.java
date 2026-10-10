package com.qualiapproche.support.courriel;

import com.qualiapproche.common.courriel.FileDeCourriels;
import org.springframework.stereotype.Component;

/** La tournée des courriels du service. Voir {@link com.qualiapproche.common.courriel.TourneeDesCourriels}. */
@Component
public class TourneeDesCourriels extends com.qualiapproche.common.courriel.TourneeDesCourriels {

    public TourneeDesCourriels(FileDeCourriels<CourrielSortant> file) {
        super(file);
    }
}
