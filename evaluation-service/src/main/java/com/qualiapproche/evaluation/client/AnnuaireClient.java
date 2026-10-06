package com.qualiapproche.evaluation.client;

import com.qualiapproche.evaluation.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Les personnes, telles que user-service les connaît depuis Keycloak.
 *
 * <p>Lue en {@code Map}, comme le fait amelioration-service : seuls le nom et le courriel servent,
 * et le contrat complet de user-service n'a pas à entrer ici.</p>
 */
@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface AnnuaireClient {

    @GetMapping("/api/v1/user-by-id")
    Map<String, Object> utilisateur(@RequestParam("userId") String userId);

    /**
     * Les porteurs d'un rôle, bornés aux membres de la structure : les pilotes du processus audité.
     *
     * <p>Réponse enveloppée dans {@code ApiResponse} : la liste est sous {@code data}, chaque
     * élément porte {@code userId}, {@code email} et {@code nomComplet}.</p>
     */
    @GetMapping("/api/v1/roles/{role}/users")
    Map<String, Object> porteursDuRole(@PathVariable("role") String role,
                                       @RequestParam("structureId") String structureId);
}
