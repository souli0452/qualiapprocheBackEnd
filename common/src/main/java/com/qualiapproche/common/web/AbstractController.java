package com.qualiapproche.common.web;

import com.qualiapproche.common.api.CriteriaDto;
import com.qualiapproche.common.service.GenericService;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Socle des contrôleurs de ressource : les points d'entrée que toutes offrent de la même façon.
 *
 * <p>Un contrôleur en dérive et les hérite ; il ne déclare que le service qui les remplit. Chaque
 * module réécrivait jusqu'ici les siens, avec un paramètre de requête par colonne — dix-sept pour
 * les non-conformités — qu'il fallait tenir à jour dans le contrôleur, le service et la
 * spécification à la fois. Les opérations communes s'ajouteront ici, et aucun contrôleur concret
 * n'aura à les reprendre.</p>
 *
 * <p>Le premier est {@code POST /search} : critères dans le corps, pagination dans l'URL. Le corps
 * et non l'URL, parce qu'une sélection multiple sur plusieurs colonnes dépasse vite ce qu'une
 * chaîne de requête sait porter, et que l'écran n'a pas à encoder des listes d'identifiants à la
 * main. {@code POST} pour cette raison seule — la recherche ne modifie rien.</p>
 *
 * <p>L'habilitation reste celle du contrôleur concret : {@code @perm.canRead(this)} désigne
 * l'instance réelle, donc les permissions que la ressource déclare.</p>
 *
 * @param <D> objet de transfert rendu à l'appelant
 */
public abstract class AbstractController<D> {

    /** Le service qui sait chercher cette ressource. */
    protected abstract GenericService<D> recherche();

    @Operation(summary = "Rechercher",
            description = "Critères dans le corps — texte libre et filtres nommés, tous facultatifs "
                    + "et cumulables — pagination et tri dans l'URL. Les résultats restent bornés à "
                    + "ce que l'appelant a le droit de voir.")
    @PreAuthorize("@perm.canRead(this)")
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<D>> search(@RequestBody(required = false) CriteriaDto criteres,
                                          @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(recherche().rechercher(criteres, pageable));
    }
}
