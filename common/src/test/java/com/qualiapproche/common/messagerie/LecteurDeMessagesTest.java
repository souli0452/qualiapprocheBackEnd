package com.qualiapproche.common.messagerie;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce qu'un consommateur accepte du courtier : nos types logiques, rien d'autre — et un message
 * illisible est écarté au lieu d'être relivré trois jours durant.
 */
class LecteurDeMessagesTest {

    private final LecteurDeMessages lecteur = new LecteurDeMessages(new ConvertisseurDesMessages(new ObjectMapper()));

    private static TextMessage message(String type, String json) throws JMSException {
        TextMessage message = mock(TextMessage.class);
        when(message.getStringProperty(AdressesMessagerie.PROPRIETE_TYPE)).thenReturn(type);
        when(message.getStringProperty(AdressesMessagerie.PROPRIETE_IDENTIFIANT)).thenReturn("m-1");
        when(message.getText()).thenReturn(json);
        return message;
    }

    @Test
    @DisplayName("Un rappel du circuit est lu, auteur compris")
    void rappelLu() throws Exception {
        TextMessage message = message(AdressesMessagerie.TYPE_RAPPEL_CIRCUIT,
                "{\"typeRessource\":\"NON_CONFORMITE\",\"auteurId\":\"u-1\",\"auteurNom\":\"Awa\"}");

        assertThat(lecteur.lire(message, RappelDuCircuit.class)).hasValueSatisfying(r -> {
            assertThat(r.getTypeRessource()).isEqualTo("NON_CONFORMITE");
            assertThat(r.getAuteurId()).isEqualTo("u-1");
            assertThat(r.getAuteurNom()).isEqualTo("Awa");
        });
        assertThat(lecteur.identifiant(message)).isEqualTo("m-1");
    }

    @Test
    @DisplayName("Un courriel confidentiel se lit comme un courriel")
    void courrielConfidentielLu() throws Exception {
        TextMessage message = message(AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL, "{\"destinataire\":\"a@b.fr\"}");

        assertThat(lecteur.lire(message, CourrielAEnvoyer.class))
                .hasValueSatisfying(c -> assertThat(c.getDestinataire()).isEqualTo("a@b.fr"));
    }

    @Test
    @DisplayName("Un nom de classe en guise de type est refusé : le convertisseur de Spring l'aurait instancié")
    void nomDeClasseRefuse() throws Exception {
        assertThat(lecteur.lire(message(RappelDuCircuit.class.getName(), "{}"), RappelDuCircuit.class)).isEmpty();
        assertThat(lecteur.lire(message("com.zaxxer.hikari.HikariConfig", "{}"), RappelDuCircuit.class)).isEmpty();
    }

    @Test
    @DisplayName("Un message sans type, au JSON invalide, ou d'un autre type que celui de la file est écarté")
    void illisiblesEcartes() throws Exception {
        assertThat(lecteur.lire(message(null, "{}"), RappelDuCircuit.class)).isEmpty();
        assertThat(lecteur.lire(message(AdressesMessagerie.TYPE_RAPPEL_CIRCUIT, "{pas du json"),
                RappelDuCircuit.class)).isEmpty();
        assertThat(lecteur.lire(message(AdressesMessagerie.TYPE_COURRIEL, "{}"), RappelDuCircuit.class)).isEmpty();
    }
}
