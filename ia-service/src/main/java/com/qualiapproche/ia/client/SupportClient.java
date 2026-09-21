package com.qualiapproche.ia.client;

import com.qualiapproche.ia.client.vue.DemandeDocumentVue;
import com.qualiapproche.ia.client.vue.DocumentVue;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_URL;

/**
 * Ce que l'assistant sait demander au module documentaire.
 *
 * <p>Comme partout, l'appel part avec le jeton et les permissions de l'appelant : le module rend
 * ce que la personne aurait vu sur ses propres écrans, et rien de plus.</p>
 */
@FeignClient(name = "support-service")
public interface SupportClient {

    /** Les documents dont l'appelant doit décider — visa, validation, diffusion. */
    @GetMapping(DOCUMENT_URL + "/a-traiter")
    List<DocumentVue> documentsATraiter();

    /** Les demandes de modification ou de suppression que l'appelant doit instruire. */
    @GetMapping("/api/v1/demandes-document/a-traiter")
    List<DemandeDocumentVue> demandesATraiter();
}
