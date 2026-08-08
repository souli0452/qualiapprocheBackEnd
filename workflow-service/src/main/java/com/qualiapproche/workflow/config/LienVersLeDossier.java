package com.qualiapproche.workflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adresse du dossier dans le frontal, pour le bouton « Consulter » des courriels d'étape.
 *
 * <p>Un seul motif servait tous les types de ressource — or les routes du frontal diffèrent :
 * un document s'ouvre par {@code ?documentId=}, une demande par {@code ?demandeId=}, une
 * non-conformité depuis son suivi. Le motif unique ne pouvait être juste que pour un type à la
 * fois, il restait donc vide, et les boutons des courriels ne menaient nulle part.</p>
 *
 * <p>La table associe à chaque type de ressource le chemin de son écran, {@code {resourceId}} y
 * étant substitué. Les chemins sont relatifs : la racine vient de {@code base-url}
 * ({@code FRONTEND_URL}), la seule chose que le déploiement ait à renseigner. Racine absente,
 * aucun lien ne part — un courriel sans bouton vaut mieux qu'un bouton mort.</p>
 *
 * <p>Un type absent de la table retombe sur l'ancien {@code motif-lien}, conservé pour les
 * installations qui l'avaient renseigné.</p>
 */
@Component
@ConfigurationProperties(prefix = "workflow.notifications")
@Getter
@Setter
public class LienVersLeDossier {

    /** Racine du frontal ({@code https://qualisira.horeb.tech}). Vide, aucun lien n'est produit. */
    private String baseUrl = "";

    /** Chemin de l'écran de chaque type de ressource, relatif à la racine. */
    private Map<String, String> liens = new LinkedHashMap<>();

    /** Ancien motif unique, gardé en repli pour un type absent de la table. */
    private String motifLien = "";

    /**
     * Adresse complète du dossier, ou chaîne vide si rien ne permet d'en produire une sûre.
     *
     * @param resourceType famille du dossier ({@code NON_CONFORMITE}, {@code DOCUMENT}…)
     * @param resourceId   identifiant du dossier
     */
    public String pour(String resourceType, String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return "";
        }
        String type = resourceType == null ? "" : resourceType.trim().toUpperCase(Locale.ROOT);
        String motif = liens.getOrDefault(type, motifLien);
        if (motif == null || motif.isBlank()) {
            return "";
        }

        String chemin = motif
                .replace("{resourceType}", type.toLowerCase(Locale.ROOT))
                .replace("{resourceId}", resourceId);

        if (chemin.startsWith("http://") || chemin.startsWith("https://")) {
            return chemin;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            // Un chemin relatif sans racine donnerait un lien mort dans un client de messagerie.
            return "";
        }
        String racine = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return racine + (chemin.startsWith("/") ? chemin : "/" + chemin);
    }
}
