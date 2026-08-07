package com.qualiapproche.common.utils;

import com.qualiapproche.common.config.MailConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Envoi d'un courriel dont le corps vient d'un gabarit Thymeleaf de fichier.
 *
 * <p>Remplace {@code MailUtils.sendEmailWithTheamleafEngine}, qui ouvrait sa <b>propre</b> session
 * JavaMail à côté de celle du contexte. Il y avait donc deux chemins d'envoi par service, avec deux
 * jeux de réglages : celui-ci reprenait l'hôte et le port mais décidait le chiffrement dans son
 * code, ignorait les délais d'attente, et ne bénéficiait d'aucune correction apportée à
 * l'autre.</p>
 *
 * <p>Un seul {@link JavaMailSender} désormais, celui que Spring Boot construit à partir de
 * {@code spring.mail.*} — SSL, STARTTLS et délais d'attente compris.</p>
 *
 * <p>L'expéditeur est la boîte authentifiée : un relais refuse un message dont l'expéditeur lui est
 * étranger, et l'ancien envoi le posait au petit bonheur.</p>
 *
 * <p>Le bean est déclaré par {@code CourrielConfig}, dans {@code common.config} — le seul paquet de
 * ce module que les services balaient. Annoté {@code @Component} ici, il n'était enregistré nulle
 * part et les services qui l'injectent ne démarraient plus.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class CourrielParGabarit {

    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    /**
     * Compose puis envoie le message.
     *
     * @param message      destinataire, copie et objet
     * @param variables    valeurs exposées au gabarit
     * @param piecesJointes fichiers à joindre, éventuellement vide
     * @param nomGabarit   nom du gabarit Thymeleaf, tel que le résolveur de fichiers l'attend
     * @param moteur       moteur de rendu du service appelant : chacun a le sien, avec son
     *                     résolveur et son préfixe de dossier
     * @throws org.springframework.mail.MailException si le serveur refuse ou n'est pas joignable.
     *         Volontairement propagée : un appelant qui l'avale fait croire à un envoi qui n'a pas
     *         eu lieu, ce qui a longtemps rendu les échecs de courriel indétectables.
     */
    public void envoyer(EmailMessage message, Map<String, Object> variables,
                        List<File> piecesJointes, String nomGabarit, TemplateEngine moteur) {
        if (message == null || message.getToAddress() == null || message.getToAddress().isBlank()) {
            // Rien à envoyer, et rien d'anormal : certains appels n'ont pas de destinataire connu.
            log.warn("Courriel « {} » non envoyé : aucun destinataire.",
                    message != null ? message.getSubject() : "sans objet");
            return;
        }

        Context contexte = new Context();
        if (variables != null) {
            contexte.setVariables(variables);
        }
        String corps = moteur.process(nomGabarit, contexte);

        MimeMessage mime = mailSender.createMimeMessage();
        try {
            boolean avecPiecesJointes = piecesJointes != null && !piecesJointes.isEmpty();
            MimeMessageHelper aide = new MimeMessageHelper(
                    mime, avecPiecesJointes, StandardCharsets.UTF_8.name());

            aide.setFrom(mailConfig.getUsername());
            aide.setTo(message.getToAddress());
            if (message.getCcAddress() != null && !message.getCcAddress().isBlank()) {
                aide.setCc(message.getCcAddress());
            }
            aide.setSubject(message.getSubject());
            aide.setText(corps, true);

            if (avecPiecesJointes) {
                for (File fichier : piecesJointes) {
                    aide.addAttachment(fichier.getName(), new FileSystemResource(fichier));
                }
            }
        } catch (jakarta.mail.MessagingException e) {
            // Message impossible à composer : adresse malformée, pièce jointe illisible. La cause ne
            // changera pas d'elle-même, mais elle doit être dite plutôt que journalisée en passant.
            throw new MailPreparationException(
                    "Courriel « " + message.getSubject() + " » impossible à composer : " + e.getMessage(), e);
        }

        mailSender.send(mime);
        log.info("Courriel « {} » remis au serveur SMTP pour {}", message.getSubject(), message.getToAddress());
    }
}
