package com.qualiapproche.evaluation.controller.referentiel;

import com.qualiapproche.common.base.AuditEntity;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.evaluation.service.referentiel.ReferentielSimpleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * Les cinq points d'entrée de tout référentiel du module.
 *
 * <p>Chaque sous-classe porte son chemin et son {@code @RequirePermissions} ; les contrôles
 * {@code @perm.canXxx(this)} lisent l'annotation sur la classe réelle, donc sur elle.</p>
 *
 * <p>La liste est enveloppée à la main : GlobalResponseHandler pagine d'office toute réponse de
 * type {@code List}, à dix éléments faute de page et de taille.</p>
 */
public abstract class ReferentielSimpleController<E extends AuditEntity> {

    protected abstract ReferentielSimpleService<E> service();

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<E>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service().lister()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<E> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service().lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<E> create(@RequestBody E saisie) {
        return ResponseEntity.ok(service().creer(saisie));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<E> update(@PathVariable("id") UUID id, @RequestBody E saisie) {
        return ResponseEntity.ok(service().modifier(id, saisie));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service().supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
