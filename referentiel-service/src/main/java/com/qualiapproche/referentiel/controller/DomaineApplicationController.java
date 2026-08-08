package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.DomaineApplicationDto;
import com.qualiapproche.referentiel.service.DomaineApplicationService;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOMAINE_APPLICATION_ROOT_URL;
import com.qualiapproche.common.response.ApiResponse;

/**
 * Référentiel des domaines d'application.
 *
 * <p>La lecture est ouverte à qui peut lire un document : l'écran de saisie doit pouvoir proposer
 * les valeurs, et support-service les résoudre pour les afficher. Seule l'administration du
 * référentiel demande la permission d'écriture.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(DOMAINE_APPLICATION_ROOT_URL)
@RequirePermissions(
        create = {"domaine-application-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"domaine-application-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"domaine-application-read", "domaine-application-write", "document-read", "document-write", "CONFIG_READ"},
        delete = {"domaine-application-write", "CONFIG_GLOBAL_MANAGE"}
)
public class DomaineApplicationController {

    // Chemins alignés sur la convention des autres référentiels (/create, /update/{id},
    // /delete/{id}) : c'est celle qu'attend `BaseCrudService` côté front, et s'en écarter
    // aurait obligé à réécrire un service d'appel pour ces deux seuls référentiels.
    private final DomaineApplicationService service;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<DomaineApplicationDto> create(@RequestBody DomaineApplicationDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<DomaineApplicationDto> update(@PathVariable("id") UUID id, @RequestBody DomaineApplicationDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(service.update(dto));
    }

    /**
     * Liste complète, non paginée et rangée.
     *
     * <p>Retour explicite en {@code ApiResponse} : {@code GlobalResponseHandler} pagine d'office
     * toute réponse de type {@code List}, et un sélecteur tronqué à dix valeurs tairait les
     * suivantes sans que rien ne l'indique.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<DomaineApplicationDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    /**
     * Page du référentiel, filtrée sur {@code search} le cas échéant.
     *
     * <p>La recherche est servie ici plutôt que dans le navigateur : une liste déroulante
     * paginée ne connaît que les pages déjà chargées, et y filtrer laisserait introuvable
     * toute valeur située au-delà.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<DomaineApplicationDto>> page(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.getAll(search, pageable));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<DomaineApplicationDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
