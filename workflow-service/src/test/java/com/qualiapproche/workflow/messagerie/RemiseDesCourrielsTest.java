package com.qualiapproche.workflow.messagerie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualiapproche.common.messagerie.AdressesMessagerie;
import com.qualiapproche.common.messagerie.ConvertisseurDesMessages;
import com.qualiapproche.common.messagerie.CourrielAEnvoyer;
import com.qualiapproche.common.messagerie.LecteurDeMessages;
import com.qualiapproche.common.messagerie.Dedoublonnage;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le seul point d'envoi SMTP de l'application : ce que tous les services déposent dans leur boîte
 * d'envoi finit ici.
 *
 * <p>Deux exigences, qui étaient chacune un défaut de l'ancien envoi direct. Un échec doit
 * <b>remonter</b> : avalé en une ligne de journal, il faisait tenir pour remis un courriel jamais
 * parti — un mot de passe expiré se traduisait par un silence complet. Et une livraison en double
 * ne doit <b>pas</b> repartir : le courtier livre « au moins une fois », et un acquittement perdu ne
 * doit pas valoir deux courriels au même responsable.</p>
 *
 * <p>Ces cas reprennent ceux qui portaient jadis sur {@code SmtpEmailService} — expéditeur, copie,
 * refus du serveur — au point où l'envoi a désormais lieu.</p>
 */
class RemiseDesCourrielsTest {

    private static final String EXPEDITEUR = "qualite@exemple.fr";

    private JavaMailSender serveur;
    private ObjectProvider<JavaMailSender> messagerie;
    private Dedoublonnage<MessageTraite> dedoublonnage;
    private RemiseDesCourriels remise;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        serveur = mock(JavaMailSender.class);
        when(serveur.createMimeMessage()).thenAnswer(i -> new MimeMessage((jakarta.mail.Session) null));
        messagerie = mock(ObjectProvider.class);
        when(messagerie.getIfAvailable()).thenReturn(serveur);
        dedoublonnage = mock(Dedoublonnage.class);
        when(dedoublonnage.premiereLivraison(any())).thenReturn(true);
        remise = new RemiseDesCourriels(messagerie, dedoublonnage,
                new LecteurDeMessages(new ConvertisseurDesMessages(new ObjectMapper())), EXPEDITEUR);
    }

    private CourrielAEnvoyer courriel(String copie) {
        return CourrielAEnvoyer.builder()
                .destinataire("claire@exemple.fr")
                .copie(copie)
                .sujet("Un dossier vous attend")
                .corps("<p>Bonjour <b>Claire</b></p>")
                .origine("etape-NOTIF_ETAPE")
                .build();
    }

    private MimeMessage messageEnvoye() {
        ArgumentCaptor<MimeMessage> aMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(serveur).send(aMessage.capture());
        return aMessage.getValue();
    }

    /**
     * Corps HTML réellement expédié.
     *
     * <p>Le message peut être multipart : lire {@code getContent()} directement rendrait la
     * représentation de l'enveloppe, sur laquelle toute assertion passerait à vide.</p>
     */
    private String extraireHtml(Object contenu) throws Exception {
        if (contenu instanceof String texte) {
            return texte;
        }
        if (contenu instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String trouve = extraireHtml(multipart.getBodyPart(i).getContent());
                if (trouve != null) {
                    return trouve;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ première livraison

    @Test
    @DisplayName("Première livraison : le courriel part avec expéditeur, destinataire, copie, objet et corps HTML")
    void premiereLivraison_courrielEnvoye() throws Exception {
        remise.remettre(courriel("rq@exemple.fr"), UUID.randomUUID().toString());

        MimeMessage message = messageEnvoye();
        // Sans expéditeur, le relais authentifié refuse le message.
        assertThat(message.getFrom()[0].toString()).isEqualTo(EXPEDITEUR);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("claire@exemple.fr");
        assertThat(message.getRecipients(Message.RecipientType.CC))
                .as("sans en-tête Cc, le responsable qualité ne recevrait rien")
                .isNotNull()
                .anyMatch(adresse -> adresse.toString().contains("rq@exemple.fr"));
        assertThat(message.getSubject()).isEqualTo("Un dossier vous attend");
        // Corps posé en HTML, et non en texte brut : le gabarit rendu s'afficherait balises comprises.
        assertThat(message.getDataHandler().getContentType()).startsWith("text/html");
        assertThat(extraireHtml(message.getContent())).isEqualTo("<p>Bonjour <b>Claire</b></p>");
    }

    @Test
    @DisplayName("Aucune copie demandée : pas d'en-tête Cc vide, que certains relais rejettent")
    void copieVide_pasDEnTeteCc() throws Exception {
        remise.remettre(courriel("   "), UUID.randomUUID().toString());

        assertThat(messageEnvoye().getRecipients(Message.RecipientType.CC)).isNull();
    }

    @Test
    @DisplayName("Aucun expéditeur configuré : le message part sans en-tête From forcé")
    void expediteurVide_fromNonPose() throws Exception {
        new RemiseDesCourriels(messagerie, dedoublonnage, null, "")
                .remettre(courriel(null), UUID.randomUUID().toString());

        assertThat(messageEnvoye().getHeader("From")).isNull();
    }

    // ------------------------------------------------------------------ livraison en double

    @Test
    @DisplayName("Livraison en double : le courriel déjà remis ne repart pas")
    void livraisonEnDouble_aucunEnvoi() {
        String identifiant = UUID.randomUUID().toString();
        when(dedoublonnage.premiereLivraison(identifiant)).thenReturn(false);

        remise.remettre(courriel(null), identifiant);

        // Un acquittement perdu ne vaut pas deux courriels au même responsable.
        verify(serveur, never()).send(any(MimeMessage.class));
        verify(messagerie, never()).getIfAvailable();
    }

    // ------------------------------------------------------------------ échecs

    @Test
    @DisplayName("Aucune messagerie configurée : l'exception rend le courriel au courtier")
    void aucuneMessagerie_exception() {
        when(messagerie.getIfAvailable()).thenReturn(null);

        // Le courriel sera relivré, et partira dès qu'une messagerie sera configurée ; l'ignorer
        // l'aurait perdu en silence.
        assertThatThrownBy(() -> remise.remettre(courriel(null), UUID.randomUUID().toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("claire@exemple.fr");
    }

    @Test
    @DisplayName("Un refus du serveur SMTP remonte, pour que le courtier relivre le courriel")
    void refusDuServeur_remonte() {
        doThrow(new MailAuthenticationException("535 auth failed"))
                .when(serveur).send(any(MimeMessage.class));

        // L'envoi attrapait jadis l'échec et rendait la main normalement : le courriel était tenu
        // pour remis alors qu'aucun n'était parti, et jamais rejoué.
        assertThatThrownBy(() -> remise.remettre(courriel(null), UUID.randomUUID().toString()))
                .isInstanceOf(MailException.class)
                .hasMessageContaining("535 auth failed");
    }

    @Test
    @DisplayName("Une adresse inexploitable ne guérira pas : le courriel est abandonné, pas relivré trois jours durant")
    void adresseInexploitable_abandonnee() {
        CourrielAEnvoyer illisible = courriel(null);
        illisible.setDestinataire("pas une adresse");

        // Aucune exception : le message est acquitté au lieu de revenir du courtier jusqu'au rebut.
        remise.remettre(illisible, UUID.randomUUID().toString());
        verify(serveur, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Un message illisible est écarté sans exception : rien ne part, rien n'est relivré")
    void messageIllisible() throws Exception {
        jakarta.jms.TextMessage illisible = mock(jakarta.jms.TextMessage.class);
        when(illisible.getStringProperty(AdressesMessagerie.PROPRIETE_TYPE)).thenReturn(AdressesMessagerie.TYPE_COURRIEL);
        when(illisible.getText()).thenReturn("{pas du json");

        remise.surMessage(illisible);

        verify(serveur, never()).send(any(MimeMessage.class));
    }
}
