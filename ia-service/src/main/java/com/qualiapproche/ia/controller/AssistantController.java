package com.qualiapproche.ia.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.ia.dto.DemandeAssistanceDto;
import com.qualiapproche.ia.dto.SuggestionHistoriqueDto;
import com.qualiapproche.ia.dto.SuggestionIaDto;
import com.qualiapproche.ia.dto.VerdictDemandeDto;
import com.qualiapproche.ia.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.IA_ASSISTANCE;
import static com.qualiapproche.common.utils.ApiUrls.IA_ROOT_URL;
import static com.qualiapproche.common.utils.ApiUrls.IA_SUGGESTIONS;
import static com.qualiapproche.common.utils.ApiUrls.IA_SUGGESTION_VERDICT;

/**
 * Assistance rédactionnelle de l'assistant IA.
 *
 * <p>La lecture et l'écriture sont distinctes : consulter l'historique des suggestions ne
 * demande que {@code assistant-ia-read}, tandis que solliciter une génération ou prononcer un
 * verdict exige {@code assistant-ia-write} — seule l'écriture est soumise au module de licence
 * ASSISTANT_IA (LicenceFilter de la passerelle).</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IA_ROOT_URL)
@RequirePermissions(
        read = {"assistant-ia-read", "assistant-ia-write"},
        create = {"assistant-ia-write"},
        update = {"assistant-ia-write"}
)
public class AssistantController {

    private final AssistantService assistantService;

    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(IA_ASSISTANCE)
    public ResponseEntity<SuggestionIaDto> assister(@Valid @RequestBody DemandeAssistanceDto demande) {
        return ResponseEntity.ok(assistantService.assister(demande));
    }

    @PreAuthorize("@perm.canUpdate(this)")
    @PostMapping(IA_SUGGESTION_VERDICT)
    public ResponseEntity<SuggestionHistoriqueDto> verdict(
            @PathVariable("id") UUID id, @Valid @RequestBody VerdictDemandeDto demande) {
        return ResponseEntity.ok(assistantService.enregistrerVerdict(id, demande.getVerdict()));
    }

    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(IA_SUGGESTIONS)
    public ResponseEntity<Page<SuggestionHistoriqueDto>> suggestions(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(assistantService.historique(pageable));
    }
}
