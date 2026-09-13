package com.qualiapproche.ia.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Ce que l'assistant emporte quand il interroge les services métier : <b>l'identité et les droits
 * de la personne qui l'a sollicité</b>, jamais les siens.
 *
 * <p>C'est la pièce qui rend sûr tout accès aux données par conversation, et elle doit exister
 * avant la première question qui en dépend. Sans elle, deux chemins s'ouvraient, mauvais tous les
 * deux : l'appel partait sans jeton et se faisait refuser, ou — pire — il repartait sur le jeton
 * {@code client_credentials} du service, déclaré dans {@code application.yml}. L'assistant aurait
 * alors parlé <b>en son nom propre</b>, avec les droits du service et le périmètre de personne :
 * « voici les non-conformités » aurait rendu celles de toutes les structures, à qui n'en voit
 * normalement qu'une. Le modèle n'y serait pour rien ; la faute serait ici.</p>
 *
 * <p>Deux choses voyagent, et il faut les deux :</p>
 * <ul>
 *   <li>le <b>jeton</b> de l'appelant, qui dit qui parle ;</li>
 *   <li>{@code X-User-Permissions}, qui dit ce qu'il a le droit de faire — le jeton Keycloak ne
 *       porte que des rôles techniques, et les permissions applicatives sont établies par la
 *       passerelle, une fois par requête entrante.</li>
 * </ul>
 *
 * <p>Propager cet en-tête ne l'expose pas : la passerelle écrase systématiquement tout
 * {@code X-User-Permissions} venu du client. Ce qui circule ici est ce qu'elle a établi.</p>
 *
 * <p>Recopie assumée de ce que font déjà {@code amelioration}, {@code referentiel},
 * {@code support} et {@code workflow} — à une exception près : <b>aucun repli sur le jeton de
 * service</b>. Ces services en ont besoin pour des actes qui sont les leurs (déclarer un fait au
 * moteur, relire un historique pour composer une fiche). L'assistant, lui, n'agit jamais pour son
 * compte : il ne consulte que pour répondre à quelqu'un. Sans requête utilisateur à l'origine, il
 * n'y a rien à demander — et un repli silencieux sur les droits du service serait exactement le
 * trou que cette classe existe pour fermer.</p>
 */
@Slf4j
@Configuration
public class FeignConfig {

    private static final String PERMISSIONS_HEADER = "X-User-Permissions";

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentification instanceof JwtAuthenticationToken jeton)) {
                // Rien à propager : l'appel partira sans jeton et sera refusé par le service visé.
                // C'est le comportement voulu — mieux vaut un refus franc qu'une réponse obtenue
                // avec des droits qui ne sont ceux de personne.
                log.warn("Appel sortant sans utilisateur authentifié : {}", requestTemplate.url());
                return;
            }

            requestTemplate.header("Authorization", "Bearer " + jeton.getToken().getTokenValue());

            var attributs = RequestContextHolder.getRequestAttributes();
            if (attributs instanceof ServletRequestAttributes servlet) {
                String permissions = servlet.getRequest().getHeader(PERMISSIONS_HEADER);
                if (permissions != null && !permissions.isBlank()) {
                    requestTemplate.header(PERMISSIONS_HEADER, permissions);
                }
            }
        };
    }

    /**
     * Les services métier répondent dans l'enveloppe {@code ApiResponse} : le contenu utile est
     * sous {@code data}, jamais à la racine. Ce décodeur le déballe, comme chez les autres
     * consommateurs.
     */
    @Bean
    public Decoder feignDecoder(ObjectMapper objectMapper) {
        return new ApiResponseFeignDecoder(objectMapper);
    }
}
