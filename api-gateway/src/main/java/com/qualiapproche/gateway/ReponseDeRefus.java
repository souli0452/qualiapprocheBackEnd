package com.qualiapproche.gateway;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Le refus que la passerelle oppose elle-même, avant tout service.
 *
 * <p>Même forme que celui de {@link LicenceFilter} : un {@code message} rédigé pour être affiché
 * tel quel, et un {@code motif} que le frontal peut tester sans analyser la phrase. Un refus sans
 * corps laisse l'utilisateur devant un écran figé, et le support devant un journal muet.</p>
 */
final class ReponseDeRefus {

    private ReponseDeRefus() {
    }

    static Mono<Void> ecrire(ServerWebExchange exchange, HttpStatus statut, String motif, String message) {
        ServerHttpResponse reponse = exchange.getResponse();
        reponse.setStatusCode(statut);
        reponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Les deux textes sont des constantes du code : rien de ce que le client envoie n'y entre,
        // l'échappement des guillemets suffit.
        String corps = "{\"message\":\"" + message.replace("\"", "'") + "\","
                + "\"motif\":\"" + motif + "\"}";
        DataBuffer tampon = reponse.bufferFactory().wrap(corps.getBytes(StandardCharsets.UTF_8));
        return reponse.writeWith(Mono.just(tampon));
    }
}
