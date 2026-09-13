package com.qualiapproche.ia.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bornes de génération de l'assistant, préfixe {@code ia.assistant}.
 *
 * <p>Valeurs d'origine dans {@code application.yml} ({@code IA_MAX_TOKENS}, {@code IA_TEMPERATURE},
 * {@code IA_TIMEOUT_MS}, {@code IA_BUDGET_JETONS_JOUR}) ; les défauts posés ici ne servent que si
 * le yml n'en porte pas.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ia.assistant")
public class IaAssistantProperties {

    /** Nombre maximal de jetons générés par suggestion. */
    private int maxTokens = 1200;

    /** Température de génération : basse, pour un rendu normatif plutôt qu'inventif. */
    private double temperature = 0.3;

    /**
     * Délai maximal d'une lecture sur le fournisseur de modèle, en millisecondes. Sans borne, une
     * connexion qui ne répond plus retient le fil de la requête jusqu'au délai du système, et
     * l'appelant attend indéfiniment. La connexion, elle, doit s'établir bien plus vite
     * ({@code min(timeoutMs, 10 s)} dans {@link IaChatConfig}).
     */
    private long timeoutMs = 45_000;

    /**
     * Budget quotidien de jetons (somme des {@code jetonsUtilises} des suggestions du jour), par
     * direction — ou par utilisateur quand le jeton ne porte pas de direction. Zéro ou une valeur
     * négative lève toute limite.
     */
    private long budgetJetonsJour = 200_000;

    /**
     * Nombre maximal de messages dans un fil de conversation, les deux rôles confondus.
     *
     * <p>Un fil n'a pas de fin naturelle : sans borne, une conversation ouverte un matin se
     * poursuit jusqu'à épuiser le budget de la structure. Atteinte, la borne n'efface rien — elle
     * invite à ouvrir un nouveau fil, ce qui repart d'un contexte propre.</p>
     */
    private int maxMessagesConversation = 40;

    /**
     * Nombre de messages passés renvoyés au modèle à chaque tour.
     *
     * <p>C'est toute la mémoire dont il dispose. Renvoyer le fil entier ferait croître le coût de
     * chaque tour avec la longueur de la conversation — le dixième échange paierait les neuf
     * précédents. Une fenêtre glissante borne ce coût ; ce qui en sort est oublié du modèle, pas
     * de la base.</p>
     */
    private int fenetreConversation = 12;
}
