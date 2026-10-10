package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.referentiel.service.impl.ParametrageParDirection;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Les points d'entrée d'un paramétrage propre à chaque direction.
 *
 * <p>Chaque sous-classe porte son chemin et son {@code @RequirePermissions} ; les contrôles
 * {@code @perm.canXxx(this)} lisent l'annotation sur la classe réelle, donc sur elle.</p>
 *
 * <p>Deux lectures de la liste : la racine sert l'écran, page par page et filtrée par
 * {@code search} ; {@code /all} sert les listes de choix, entière — enveloppée à la main, car
 * {@code GlobalResponseHandler} pagine d'office toute réponse de type {@code List}, à dix
 * éléments faute de page et de taille.</p>
 */
public abstract class ParametrageParDirectionController<D> {

    protected abstract ParametrageParDirection<?, D> service();

    /** L'écran, page par page : les entrées actives, ou toutes avec {@code inactifs}. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<D>> page(
            @RequestParam(value = "inactifs", defaultValue = "false") boolean inactifs,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service().page(inactifs, search, pageable));
    }

    /** Les entrées actives, pour un choix ; avec {@code inactifs}, aussi les retirées, pour l'affichage. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<D>>> all(
            @RequestParam(value = "inactifs", defaultValue = "false") boolean inactifs) {
        return ResponseEntity.ok(ApiResponse.success(service().lister(inactifs)));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<D> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service().lire(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<D> create(@RequestBody D saisie) {
        return ResponseEntity.ok(service().creer(saisie));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<D> update(@PathVariable("id") UUID id, @RequestBody D saisie) {
        return ResponseEntity.ok(service().modifier(id, saisie));
    }

    /** Retire l'entrée des choix, sans l'effacer : les dossiers qui la citent la lisent toujours. */
    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service().retirer(id);
        return ResponseEntity.noContent().build();
    }
}
