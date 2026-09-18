package com.qualiapproche.ia.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.ia.config.IaAssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Le seul point par lequel le service parle au modèle.
 *
 * <p>Rassemblé ici parce que deux usages s'en servent — la suggestion rédactionnelle et la
 * conversation — et que ce qui s'y joue ne doit être écrit qu'une fois : <b>un appel, un seul</b>,
 * et la traduction des pannes du fournisseur en réponses intelligibles. Dupliqué, ce code se
 * serait remis à facturer deux générations d'un côté pendant qu'on le corrigeait de l'autre.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientDuModele {

    /** Message rendu quand le fournisseur répond mal, quelle qu'en soit la cause exacte. */
    public static final String MESSAGE_INDISPONIBLE =
            "Le service d'assistance IA est momentanément indisponible.";

    /** Borne du parcours de la chaîne des causes, cf. {@link #estUnDepassementDeDelai}. */
    private static final int PROFONDEUR_MAX_DES_CAUSES = 10;

    private final ChatClient chatClient;
    private final IaAssistantProperties proprietes;

    /**
     * Une consigne système, un message utilisateur, une réponse.
     *
     * @param consigne le prompt système
     * @param message ce que le demandeur fournit
     */
    public ChatResponse repondre(String consigne, String message) {
        return appeler(chatClient.prompt().system(consigne).user(message));
    }

    /**
     * Une consigne système et un fil de messages — la conversation.
     *
     * <p>Le fil est rendu au modèle à chaque tour : c'est la seule mémoire qu'il ait. L'appelant
     * décide de sa longueur ; ici on se contente de le transmettre.</p>
     */
    public ChatResponse repondre(String consigne, List<Message> fil) {
        return appeler(chatClient.prompt().system(consigne).messages(fil));
    }

    /**
     * Le texte produit, ou {@code null} si le modèle n'a rien rendu : une réponse sans génération
     * reste possible (filtrage du fournisseur, arrêt immédiat) et ne doit pas lever ici.
     */
    public String texteDe(ChatResponse reponse) {
        if (reponse == null || reponse.getResult() == null || reponse.getResult().getOutput() == null) {
            return null;
        }
        return reponse.getResult().getOutput().getText();
    }

    /** Le modèle ayant répondu, tel qu'il se nomme lui-même. */
    public String modeleDe(ChatResponse reponse) {
        return reponse != null && reponse.getMetadata() != null ? reponse.getMetadata().getModel() : null;
    }

    /** Les jetons consommés par l'appel, si le fournisseur les rapporte. */
    public Long jetonsDe(ChatResponse reponse) {
        if (reponse == null || reponse.getMetadata() == null) {
            return null;
        }
        Usage usage = reponse.getMetadata().getUsage();
        return usage != null && usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : null;
    }

    /**
     * Déclenche la génération, une seule fois, et traduit les échecs.
     *
     * <p>{@code call()} est <b>paresseux</b> : il ne fait que construire le porteur de la requête.
     * {@code content()} et {@code chatResponse()} déclenchent chacun une génération — les
     * interroger tous deux facturait deux appels pour une seule réponse, et faisait décrire à la
     * trace une génération autre que celle rendue. On ne demande donc que {@code chatResponse()},
     * dont le texte s'extrait ensuite par {@link #texteDe}.</p>
     */
    private ChatResponse appeler(ChatClient.ChatClientRequestSpec requete) {
        try {
            return requete.call().chatResponse();
        } catch (RuntimeException e) {
            throw traduire(e);
        }
    }

    /**
     * Traduit l'échec d'un appel au modèle en réponse intelligible pour l'appelant.
     *
     * <p>Le dépassement de délai se cherche <b>dans toute la chaîne des causes</b>, et non sur le
     * type de l'exception de tête : un essai l'a montré, une lecture qui expire ne remonte pas en
     * {@code ResourceAccessException} portant directement un {@link SocketTimeoutException}, mais
     * en {@code RestClientException} « Error while extracting response », le délai n'apparaissant
     * que deux causes plus bas. Le service rendait donc 503 « indisponible » là où le modèle avait
     * simplement été trop lent — deux situations que l'exploitant ne doit pas confondre.</p>
     *
     * <p>Le détail exact est journalisé, jamais rendu : il peut contenir des fragments de la
     * requête envoyée au fournisseur.</p>
     */
    private BusinessException traduire(RuntimeException e) {
        if (estUnDepassementDeDelai(e)) {
            log.warn("Le fournisseur de modèle a dépassé le délai de {} ms.", proprietes.getTimeoutMs());
            return new BusinessException(
                    "Réponse du modèle trop lente, réessayez.", HttpStatus.GATEWAY_TIMEOUT);
        }
        if (e instanceof ResourceAccessException) {
            log.warn("Fournisseur de modèle injoignable : {}", e.getMessage());
            return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (e instanceof RestClientResponseException reponse) {
            log.warn("Le fournisseur de modèle a répondu {} : {}",
                    reponse.getStatusCode(), reponse.getMessage());
            return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        log.error("Erreur inattendue lors de l'appel au modèle.", e);
        return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Un dépassement de délai figure-t-il dans la chaîne des causes ? Le parcours est borné : une
     * chaîne circulaire — deux exceptions se désignant l'une l'autre — y tournerait sans fin.
     */
    private boolean estUnDepassementDeDelai(Throwable erreur) {
        Throwable cause = erreur;
        for (int profondeur = 0; cause != null && profondeur < PROFONDEUR_MAX_DES_CAUSES; profondeur++) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return false;
    }
}
