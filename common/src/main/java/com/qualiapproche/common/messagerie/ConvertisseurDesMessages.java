package com.qualiapproche.common.messagerie;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

/**
 * Le convertisseur JSON de la messagerie, restreint à nos types logiques.
 *
 * <p>Celui de Spring, quand la propriété {@code _type} ne figure pas parmi ses correspondances, la
 * prend pour un nom de classe et instancie cette classe ({@code ClassUtils.forName}). Quiconque
 * pouvait écrire sur le courtier choisissait ainsi la classe que nos services construisaient —
 * porte connue d'exécution de code à distance. Ici, un type inconnu est refusé, sans repli.</p>
 */
public class ConvertisseurDesMessages extends MappingJackson2MessageConverter {

    /** Les seuls types qu'un message peut annoncer, et ce qu'ils deviennent. */
    static final Map<String, Class<?>> TYPES = Map.of(
            AdressesMessagerie.TYPE_COURRIEL, CourrielAEnvoyer.class,
            AdressesMessagerie.TYPE_COURRIEL_CONFIDENTIEL, CourrielAEnvoyer.class,
            AdressesMessagerie.TYPE_RAPPEL_CIRCUIT, RappelDuCircuit.class);

    private final ObjectMapper json;

    public ConvertisseurDesMessages(ObjectMapper json) {
        this.json = json;
        setObjectMapper(json);
        setTargetType(MessageType.TEXT);
        setTypeIdPropertyName(AdressesMessagerie.PROPRIETE_TYPE);
        setTypeIdMappings(TYPES);
    }

    @Override
    protected JavaType getJavaTypeForMessage(Message message) throws JMSException {
        String type = message.getStringProperty(AdressesMessagerie.PROPRIETE_TYPE);
        Class<?> classe = type == null ? null : TYPES.get(type);
        if (classe == null) {
            throw new MessageConversionException("Type de message inconnu : " + type);
        }
        return json.constructType(classe);
    }
}
