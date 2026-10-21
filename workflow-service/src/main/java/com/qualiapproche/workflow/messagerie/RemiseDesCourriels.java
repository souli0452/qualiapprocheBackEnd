package com.qualiapproche.workflow.messagerie;

import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.CourrielAEnvoyer;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import jakarta.jms.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Le seul point d'envoi SMTP de l'application : remet les courriels que tous les services
 * déposent dans leur boîte d'envoi.
 *
 * <p>Un échec passager — serveur injoignable, authentification refusée — lève l'exception : le
 * message est rendu au courtier, qui le relivre avec une attente croissante (jusqu'à trois jours
 * environ), puis le range au rebut (DLQ), où il reste lisible. Un courriel impossible à composer
 * (adresse malformée) ne guérira pas : il est journalisé et acquitté, au lieu d'être relivré trois
 * jours durant pour rien.</p>
 *
 * <p>Le courtier livre « au moins une fois » : un courriel déjà remis, puis relivré parce que son
 * acquittement s'est perdu, est écarté par le dédoublonnage plutôt qu'envoyé deux fois. La trace
 * est écrite dans la transaction du traitement : si l'envoi échoue, elle est annulée avec lui.</p>
 */
@Slf4j
@Component
public class RemiseDesCourriels {

    private final ObjectProvider<JavaMailSender> messagerie;
    private final Dedoublonnage<MessageTraite> dedoublonnage;
    private final LecteurDeMessages lecteur;
    private final String expediteur;

    public RemiseDesCourriels(ObjectProvider<JavaMailSender> messagerie, Dedoublonnage<MessageTraite> dedoublonnage,
                              LecteurDeMessages lecteur, @Value("${spring.mail.username:}") String expediteur) {
        this.messagerie = messagerie;
        this.dedoublonnage = dedoublonnage;
        this.lecteur = lecteur;
        this.expediteur = expediteur == null ? "" : expediteur;
    }

    /** Reçoit un courriel du courtier ; un message illisible est écarté sans nouvelle livraison. */
    @Transactional
    @JmsListener(destination = AdressesMessagerie.COURRIELS)
    public void surMessage(Message message) {
        lecteur.lire(message, CourrielAEnvoyer.class).ifPresent(c -> remettre(c, lecteur.identifiant(message)));
    }

    public void remettre(CourrielAEnvoyer courriel, String identifiant) {
        if (!dedoublonnage.premiereLivraison(identifiant)) {
            return;
        }
        JavaMailSender serveur = messagerie.getIfAvailable();
        if (serveur == null) {
            // Rendu au courtier : il sera relivré, et partira dès qu'une messagerie sera configurée.
            throw new IllegalStateException("Aucune messagerie configurée (spring.mail.host) : courriel « "
                    + courriel.getSujet() + " » pour " + courriel.getDestinataire() + " non remis.");
        }
        MimeMessage message = serveur.createMimeMessage();
        try {
            MimeMessageHelper aide = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            // L'expéditeur est la boîte authentifiée : un relais refuse un message dont l'expéditeur
            // est absent, ou étranger à la boîte qui s'authentifie.
            if (!expediteur.isBlank()) {
                aide.setFrom(expediteur);
            }
            aide.setTo(courriel.getDestinataire());
            if (courriel.getCopie() != null && !courriel.getCopie().isBlank()) {
                aide.setCc(courriel.getCopie().trim());
            }
            aide.setSubject(courriel.getSujet());
            aide.setText(courriel.getCorps(), true);
        } catch (MessagingException e) {
            log.error("Courriel « {} » ({}) pour « {} » impossible à composer, abandonné : {}", courriel.getSujet(),
                    courriel.getOrigine(), courriel.getDestinataire(), e.getMessage());
            return;
        }
        serveur.send(message);
        log.info("Courriel « {} » ({}) remis au serveur SMTP pour {}.", courriel.getSujet(), courriel.getOrigine(),
                courriel.getDestinataire());
    }
}
