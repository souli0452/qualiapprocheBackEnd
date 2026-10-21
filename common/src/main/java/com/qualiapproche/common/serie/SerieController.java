package com.qualiapproche.common.serie;

import com.qualiapproche.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Les séries de numérotation d'un service : les lire, et les caler.
 *
 * <p>Chaque sous-classe porte son chemin et son {@code @RequirePermissions}. La liste est
 * enveloppée à la main : {@code GlobalResponseHandler} pagine d'office toute {@code List}.</p>
 */
public abstract class SerieController<E extends AbstractSerieEntity> {

    protected abstract AttributionDeNumero<E> attribution();

    protected abstract E nouvelle();

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<E>>> lister() {
        return ResponseEntity.ok(ApiResponse.success(attribution().lister()));
    }

    /** Cale la série : le prochain numéro attribué sera {@code valeur + 1}. Jamais en arrière. */
    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/{cle}/calage")
    @Transactional
    public ResponseEntity<E> caler(@PathVariable("cle") String cle, @RequestParam("valeur") long valeur) {
        return ResponseEntity.ok(attribution().caler(cle, valeur, c -> nouvelle()));
    }
}
