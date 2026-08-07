package com.qualiapproche.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Paramètres du serveur de courriel, tels que {@code spring.mail.*} les porte.
 *
 * <p>Spring Boot construit lui-même le {@code JavaMailSender} à partir de ces propriétés — y compris
 * celles de {@link #properties}. Trois services le remplaçaient par un bean bâti à la main qui ne
 * reprenait que l'hôte, le port et l'authentification : tout ce qui vit sous
 * {@code spring.mail.properties} — SSL, délais d'attente — était perdu. Impossible, dès lors, de
 * joindre un serveur en SSL implicite (port 465) : la connexion partait en clair sur un port
 * chiffré et attendait jusqu'au délai du système. Ces beans ont été retirés.</p>
 *
 * <p>Cette classe subsiste pour {@code MailUtils}, qui ouvre sa propre session JavaMail.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    private String protocol;

    /**
     * Propriétés JavaMail brutes, reprises telles quelles ({@code mail.smtp.ssl.enable},
     * {@code mail.smtp.starttls.enable}, {@code mail.smtp.timeout}…).
     *
     * <p>Remplace les deux champs {@code auth} et {@code starttlsEnable} qui liaient
     * {@code spring.mail.auth} et {@code spring.mail.starttls-enable} — deux propriétés qui
     * n'existent dans aucun fichier de configuration du projet, les vraies vivant sous
     * {@code spring.mail.properties.mail.smtp.*}. Elles étaient donc toujours nulles, et le code
     * retombait sur des valeurs écrites en dur : STARTTLS activé quoi qu'il arrive, SSL jamais.</p>
     */
    private Map<String, String> properties = new LinkedHashMap<>();
}
