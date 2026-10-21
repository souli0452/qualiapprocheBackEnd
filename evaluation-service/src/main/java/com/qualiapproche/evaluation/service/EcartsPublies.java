package com.qualiapproche.evaluation.service;

import java.util.List;
import java.util.UUID;

/**
 * Des écarts viennent d'être publiés : constats dont la nature génère une non-conformité, ou
 * critères RQAP-BF au constat NC. Leur transmission attend que la publication soit committée.
 */
public record EcartsPublies(List<UUID> constatIds, List<UUID> notationIds) {

    public static EcartsPublies deConstats(List<UUID> ids) {
        return new EcartsPublies(ids, List.of());
    }

    public static EcartsPublies deNotations(List<UUID> ids) {
        return new EcartsPublies(List.of(), ids);
    }

    public boolean estVide() {
        return constatIds.isEmpty() && notationIds.isEmpty();
    }
}
