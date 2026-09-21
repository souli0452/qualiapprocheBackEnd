package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.common.dto.FichierFaqDto;
import com.qualiapproche.referentiel.service.FaqService;
import com.qualiapproche.referentiel.service.FichierFaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.FAQ_FICHIER;
import static com.qualiapproche.common.utils.ApiUrls.FAQ_FICHIERS;
import static com.qualiapproche.common.utils.ApiUrls.FAQ_PUBLIEES;
import static com.qualiapproche.common.utils.ApiUrls.FAQ_ROOT_URL;

/**
 * La foire aux questions de l'application.
 *
 * <p>Deux publics, deux droits. <b>Écrire</b> demande {@code faq-write} : ce qu'on y met devient
 * la parole de l'organisation, affichée dans l'aide et récitée par l'assistant IA. <b>Lire les
 * entrées publiées</b> ne demande rien de plus que d'être connecté — une aide qu'il faut une
 * permission pour consulter n'aide personne, et c'est aussi ce que l'assistant appelle avec le
 * jeton de la personne qui l'interroge.</p>
 *
 * <p>La liste d'administration, elle, montre les brouillons et les entrées dépubliées : elle
 * reste derrière {@code faq-read}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(FAQ_ROOT_URL)
@RequirePermissions(
        create = {"faq-write", "CONFIG_GLOBAL_MANAGE"},
        update = {"faq-write", "CONFIG_GLOBAL_MANAGE"},
        read = {"faq-read", "faq-write", "CONFIG_READ"},
        delete = {"faq-write", "CONFIG_GLOBAL_MANAGE"}
)
public class FaqController {

    private final FaqService service;
    private final FichierFaqService fichiers;

    /**
     * Les entrées publiées, pour l'aide et pour l'assistant IA.
     *
     * <p>Seul point d'entrée sans permission propre. L'assistant l'appelle avec le jeton de son
     * interlocuteur — il ne voit donc jamais la FAQ d'une autre organisation que la sienne, et
     * jamais les brouillons.</p>
     */
    @GetMapping(FAQ_PUBLIEES)
    public ResponseEntity<ApiResponse<List<FaqDto>>> publiees() {
        return ResponseEntity.ok(ApiResponse.success(service.getPubliees()));
    }

    /**
     * Le référentiel en entier, pour l'écran d'administration.
     *
     * <p>Enveloppé à la main : GlobalResponseHandler pagine d'office toute réponse de type
     * {@code List}, à dix éléments faute de page et de taille — l'écran aurait affiché dix
     * questions sur trente sans que rien ne l'indique. Un {@code ApiResponse} explicite est la
     * seule forme que l'intercepteur laisse passer intacte.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FaqDto>>> all() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping
    public ResponseEntity<Page<FaqDto>> page(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(service.getAll(search, pageable));
    }

    /**
     * Une entrée par son identifiant.
     *
     * <p>Sous {@code /get/} et non à la racine : un {@code /{id}} nu happait {@code /all} et
     * {@code /publiees}, que Spring lui présentait comme des identifiants — « Invalid UUID
     * string: all », en 500, sur la simple consultation de la liste.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping("/get/{id}")
    public ResponseEntity<FaqDto> getById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping("/create")
    public ResponseEntity<FaqDto> create(@Valid @RequestBody FaqDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PutMapping("/update/{id}")
    public ResponseEntity<FaqDto> update(@PathVariable("id") UUID id, @Valid @RequestBody FaqDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(service.update(dto));
    }

    /**
     * Joint une ou plusieurs pièces à une entrée. Facultatif : une réponse se suffit souvent.
     *
     * <p>Rend 503 si l'installation n'a pas de serveur de fichiers — la FAQ reste alors
     * utilisable, seules les pièces sont indisponibles.</p>
     */
    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping(value = FAQ_FICHIERS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FichierFaqDto>> joindre(
            @PathVariable("id") UUID id,
            @RequestPart("fichiers") List<MultipartFile> aDeposer) {
        return ResponseEntity.ok(fichiers.deposer(id, aDeposer));
    }

    /**
     * Le contenu d'une pièce jointe.
     *
     * <p>Ouvert à qui peut lire la FAQ publiée, comme la réponse qu'elle appuie : un formulaire
     * que l'aide mentionne mais qu'on ne peut pas ouvrir n'aide personne. L'appartenance de
     * l'entrée à l'organisation de l'appelant est vérifiée par le service.</p>
     */
    @GetMapping(FAQ_FICHIER)
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable("fichierId") UUID fichierId) {
        FichierFaqDto description = fichiers.decrire(fichierId);
        String nom = description.getNom() == null ? "piece-jointe" : description.getNom();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nom + "\"")
                .contentType(description.getType() == null ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(description.getType()))
                .body(new InputStreamResource(fichiers.contenu(fichierId)));
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping(FAQ_FICHIER)
    public ResponseEntity<Void> retirerLaPiece(@PathVariable("fichierId") UUID fichierId) {
        fichiers.supprimer(fichierId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@perm.canDelete(this)")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
