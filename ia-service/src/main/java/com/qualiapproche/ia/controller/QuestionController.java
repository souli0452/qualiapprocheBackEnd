package com.qualiapproche.ia.controller;

import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.ia.dto.QuestionDemandeDto;
import com.qualiapproche.ia.dto.QuestionDto;
import com.qualiapproche.ia.dto.ReponseQuestionDto;
import com.qualiapproche.ia.enumeration.QuestionPredefinie;
import com.qualiapproche.ia.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.qualiapproche.common.utils.ApiUrls.IA_QUESTIONS;
import static com.qualiapproche.common.utils.ApiUrls.IA_QUESTION_PAR_CODE;
import static com.qualiapproche.common.utils.ApiUrls.IA_ROOT_URL;

/**
 * Les questions prédéfinies — celles qui puisent dans l'API métier.
 *
 * <p>Le catalogue est servi plutôt que codé dans l'écran : une question ajoutée ici apparaît dans
 * le fil sans livrer un frontal.</p>
 *
 * <p>Poser une question consomme une génération et interroge un service métier : elle exige donc
 * {@code assistant-ia-write}, comme toute sollicitation de l'assistant, et passe par le module de
 * licence ASSISTANT_IA. Ce que la réponse contient, en revanche, est décidé par le service métier
 * interrogé, qui reçoit le jeton et les permissions de l'appelant.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(IA_ROOT_URL)
@RequirePermissions(
        read = {"assistant-ia-read", "assistant-ia-write"},
        create = {"assistant-ia-write"}
)
public class QuestionController {

    private final QuestionService questionService;

    /**
     * Ce que l'écran peut proposer.
     *
     * <p>Rendu dans une {@code ApiResponse} explicite, et non en {@code List} nue : le
     * {@code GlobalResponseHandler} pagine toute liste qu'on lui laisse: le catalogue serait
     * servi par tranches de dix, et la onzième question disparaîtrait sans que rien ne le dise.</p>
     */
    @PreAuthorize("@perm.canRead(this)")
    @GetMapping(IA_QUESTIONS)
    public ResponseEntity<ApiResponse<List<QuestionDto>>> catalogue() {
        return ResponseEntity.ok(ApiResponse.success(questionService.catalogue()));
    }

    /** Pose l'une d'elles, et rattache l'échange au fil en cours s'il y en a un. */
    @PreAuthorize("@perm.canCreate(this)")
    @PostMapping(IA_QUESTION_PAR_CODE)
    public ResponseEntity<ReponseQuestionDto> repondre(@PathVariable("code") QuestionPredefinie code,
                                                      @RequestBody(required = false) QuestionDemandeDto demande) {
        return ResponseEntity.ok(questionService.repondre(
                code, demande != null ? demande.getConversationId() : null));
    }
}
