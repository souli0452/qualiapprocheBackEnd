package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.PrioriteDocumentDto;
import com.qualiapproche.referentiel.service.PrioriteDocumentService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.PRIORITE_DOCUMENT_ROOT_URL;

/**
 * Référentiel des priorités de document.
 *
 * <p>La lecture est ouverte à qui peut lire un document : l'écran de saisie doit pouvoir proposer
 * les valeurs, et support-service les résoudre pour les afficher. Seule l'administration du
 * référentiel demande la permission d'écriture.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(PRIORITE_DOCUMENT_ROOT_URL)
@RequirePermissions(
        create = {"priorite-document-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"priorite-document-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"priorite-document-read", "priorite-document-write", "document-read", "document-write", "CONFIG_READ"},
        delete = {"priorite-document-write", "CONFIG_GLOBAL_MANAGE"}
)
public class PrioriteDocumentController {

    // Chemins alignés sur la convention des autres référentiels (/create, /update/{id},
    // /delete/{id}) : c'est celle qu'attend `BaseCrudService` côté front, et s'en écarter
    // aurait obligé à réécrire un service d'appel pour ces deux seuls référentiels.
    private final PrioriteDocumentService service;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<PrioriteDocumentDto> create(@RequestBody PrioriteDocumentDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<PrioriteDocumentDto> update(@PathVariable("id") UUID id, @RequestBody PrioriteDocumentDto dto) {
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
    public ResponseEntity<com.qualiapproche.common.response.ApiResponse<List<PrioriteDocumentDto>>> all() {
        return ResponseEntity.ok(com.qualiapproche.common.response.ApiResponse.success(service.getAll()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<PrioriteDocumentDto>> page(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<PrioriteDocumentDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
