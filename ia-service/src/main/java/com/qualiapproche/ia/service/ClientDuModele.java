package com.qualiapproche.ia.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.ia.config.IaAssistantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
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
     * Le texte produit, débarrassé du balisage Markdown, ou {@code null} si le modèle n'a rien
     * rendu : une réponse sans génération reste possible (filtrage du fournisseur, arrêt immédiat)
     * et ne doit pas lever ici.
     */
    public String texteDe(ChatResponse reponse) {
        if (reponse == null || reponse.getResult() == null || reponse.getResult().getOutput() == null) {
            return null;
        }
        return sansBalisage(reponse.getResult().getOutput().getText());
    }

    /**
     * Retire le balisage Markdown que le modèle produit par habitude.
     *
     * <p>Rien ne le met en forme : le fil l'affiche en texte brut, et une suggestion part dans un
     * champ de formulaire. Les dièses et les astérisques s'y montrent tels quels — une réponse de
     * quatre lignes en portait davantage que de mots utiles. La consigne le proscrit déjà, mais un
     * modèle de sept milliards de paramètres retombe dans ses plis : la garantie est ici.</p>
     *
     * <p>Le texte est conservé, seules les marques tombent : un titre devient sa propre ligne, un
     * terme en gras redevient le terme. Les puces sont normalisées en tirets plutôt que
     * supprimées — une énumération reste une énumération, et le tiret se lit partout.</p>
     */
    private String sansBalisage(String texte) {
        if (texte == null || texte.isBlank()) {
            return texte;
        }
        return texte
                // Titres : « ### Constat » devient « Constat », la ligne subsiste.
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s+", "")
                // Puces d'astérisque ou de plus, ramenées au tiret : « * point » devient « - point ».
                .replaceAll("(?m)^(\\s*)[*+]\\s+", "$1- ")
                // Gras et italique, doubles marques d'abord : sans quoi « **mot** » laisse « *mot* ».
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("__(.+?)__", "$1")
                .replaceAll("(?<![\\p{L}\\d*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\p{L}\\d*])", "$1")
                // Traits de séparation seuls sur leur ligne : « --- », « *** », « ___ ».
                .replaceAll("(?m)^\\s*([-*_])\\1{2,}\\s*$", "")
                // Accents graves, ceux des blocs comme ceux des termes.
                .replaceAll("(?m)^\\s*```.*$", "")
                .replace("`", "")
                // Les lignes vidées par ce qui précède ne doivent pas laisser de trous.
                .replaceAll("\\n{3,}", "\\n\\n")
                .strip();
    }

    /** Le modèle ayant répondu, tel qu'il se nomme lui-même. */
    public String modeleDe(ChatResponse reponse) {
        return reponse != null && reponse.getMetadata() != null ? reponse.getMetadata().getModel() : null;
    }

    /**
     * Les jetons de l'appel, rapportés par le fournisseur ou estimés à défaut.
     *
     * <p>Ollama ne renseigne pas toujours l'usage. Rendus nuls, ces appels comptaient zéro dans
     * le budget quotidien, qui ne s'appliquait donc jamais sur une installation auto-hébergée —
     * le garde-fou manquait là où il protège le mieux, puisqu'un modèle local ne se facture pas
     * mais se paie en CPU. L'estimation, grossière, vaut mieux que l'aveuglement : elle borne
     * une boucle, ce qu'un zéro ne fait pas.</p>
     *
     * @param envoye ce qui est parti au modèle, consigne comprise
     * @param rendu  ce qu'il a produit
     */
    public long jetonsOuEstimation(ChatResponse reponse, String envoye, String rendu) {
        Long rapportes = jetonsDe(reponse);
        if (rapportes != null && rapportes > 0) {
            return rapportes;
        }
        long caracteres = (envoye == null ? 0 : envoye.length()) + (rendu == null ? 0 : rendu.length());
        int parJeton = Math.max(1, proprietes.getCaracteresParJeton());
        return Math.max(1, caracteres / parJeton);
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
     * <p>Le statut du fournisseur se cherche de la même façon, et pour la même raison. Spring AI
     * n'émet pas les exceptions du client HTTP : il les enveloppe dans {@link TransientAiException}
     * (429 et 5xx, après réessai) et {@link NonTransientAiException} (les 4xx). Éprouvés sur le
     * seul type de tête, les deux tests ci-dessous ne reconnaissaient donc <b>aucune</b> erreur du
     * fournisseur : un 401 comme un 429 tombaient dans la branche « inattendue », qui déverse une
     * pile de cent lignes là où une seule était prévue — et l'exploitant cherchait une panne du
     * service quand le fournisseur, lui, disait clairement pourquoi il refusait.</p>
     *
     * <p>Ces deux enveloppes ne portent pas toujours de cause : le statut et le corps de la
     * réponse ne vivent alors que dans leur message, d'où la branche qui le journalise tel quel.</p>
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
        ResourceAccessException injoignable = dansLesCauses(e, ResourceAccessException.class);
        if (injoignable != null) {
            log.warn("Fournisseur de modèle injoignable : {}", injoignable.getMessage());
            return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        RestClientResponseException reponse = dansLesCauses(e, RestClientResponseException.class);
        if (reponse != null) {
            log.warn("Le fournisseur de modèle a répondu {} : {}",
                    reponse.getStatusCode(), reponse.getMessage());
            return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (e instanceof NonTransientAiException || e instanceof TransientAiException) {
            log.warn("Le fournisseur de modèle a refusé l'appel : {}", e.getMessage());
            return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
        }
        log.error("Erreur inattendue lors de l'appel au modèle.", e);
        return new BusinessException(MESSAGE_INDISPONIBLE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * Un dépassement de délai figure-t-il dans la chaîne des causes ?
     */
    private boolean estUnDepassementDeDelai(Throwable erreur) {
        return dansLesCauses(erreur, SocketTimeoutException.class) != null;
    }

    /**
     * La première exception du type demandé dans la chaîne des causes, tête comprise.
     *
     * <p>Le parcours est borné : une chaîne circulaire — deux exceptions se désignant l'une
     * l'autre — y tournerait sans fin.</p>
     */
    private <T extends Throwable> T dansLesCauses(Throwable erreur, Class<T> type) {
        Throwable cause = erreur;
        for (int profondeur = 0; cause != null && profondeur < PROFONDEUR_MAX_DES_CAUSES; profondeur++) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
            cause = cause.getCause() == cause ? null : cause.getCause();
        }
        return null;
    }
}
