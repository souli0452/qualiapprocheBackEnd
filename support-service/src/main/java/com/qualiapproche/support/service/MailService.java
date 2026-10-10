package com.qualiapproche.support.service;

import com.qualiapproche.common.courriel.FileDeCourriels;
import com.qualiapproche.support.courriel.CourrielSortant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Courriels du support-service : rendus par un gabarit Thymeleaf, puis déposés dans la file du
 * service ({@link FileDeCourriels}), que la tournée remet au serveur de messagerie.
 *
 * <p>L'envoi partait d'un {@code @Async} : en mémoire, perdu à un redémarrage, et un échec du
 * serveur n'était que journalisé. Déposé dans la transaction de l'appelant, le courriel n'existe
 * que si elle est validée, et il est rejoué jusqu'à sa remise.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final TemplateEngine templateEngine;
    private final FileDeCourriels<CourrielSortant> file;

    /**
     * Rend le gabarit et dépose le courriel.
     *
     * @param to           adresse du destinataire
     * @param subject      objet
     * @param templateName nom du gabarit (sans chemin, ex : « qmsDocumentShare »)
     * @param variables    variables du gabarit
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        file.deposer(to, null, subject, templateEngine.process(templateName, context), "document-" + templateName);
    }
}
