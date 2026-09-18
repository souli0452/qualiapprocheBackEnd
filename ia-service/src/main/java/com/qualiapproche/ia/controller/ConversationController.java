package com.qualiapproche.ia.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.ia.dto.ConversationDto;
import com.qualiapproche.ia.dto.MessageDemandeDto;
import com.qualiapproche.ia.dto.ReponseConversationDto;
import com.qualiapproche.ia.service.ConversationService;
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

import static com.qualiapproche.common.utils.ApiUrls.IA_CONVERSATION;
import static com.qualiapproche.common.utils.ApiUrls.IA_CONVERSATIONS;
import static com.qualiapproche.common.utils.ApiUrls.IA_CONVERSATION_PAR_ID;
import static com.qualiapproche.common.utils.ApiUrls.IA_ROOT_URL;

/**
 * Le fil de discussion avec l'assistant qualité.
 *
 * <p>Distinct de l'assistance rédactionnelle : le bouton d'un écran écrit <b>dans un champ</b>,
 * ce fil répond <b>à une question</b>. Les deux ne se remplacent pas.</p>
 *
 * <p>Cet assistant ne consulte aucune donnée de l'organisation : il répond sur la méthode et le
 * vocabulaire qualité, et dit qu'il ne voit pas les dossiers dès qu'on l'interroge sur l'un d'eux.
 * C'est ce qui permet de l'ouvrir à tous sans question de périmètre — il n'a rien à divulguer.</p>
 *
 * <p>Écrire exige {@code assistant-ia-write}, et passe donc par le module de licence ASSISTANT_IA
 * (LicenceFilter de la passerelle) ; relire ses propres fils ne demande que la lecture.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IA_ROOT_URL)
@RequirePermissions(
        read = {"assistant-ia-read", "assistant-ia-write"},
        create = {"assistant-ia-write"}
)
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Pose une question. Sans {@code conversationId}, un fil s'ouvre — l'appelant n'a pas à
     * demander l'ouverture séparément, ce qui lui éviterait mal un aller-retour.
     */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(IA_CONVERSATION)
    public ResponseEntity<ReponseConversationDto> repondre(@Valid @RequestBody MessageDemandeDto demande) {
        return ResponseEntity.ok(conversationService.repondre(demande));
    }

    /** Ses propres fils, du plus vivant au plus ancien, sans leurs messages. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(IA_CONVERSATIONS)
    public ResponseEntity<Page<ConversationDto>> mesConversations(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(conversationService.mesConversations(pageable));
    }

    /** Un fil et ses messages, pour le rouvrir tel qu'il a été laissé. */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(IA_CONVERSATION_PAR_ID)
    public ResponseEntity<ConversationDto> conversation(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(conversationService.conversation(id));
    }
}
