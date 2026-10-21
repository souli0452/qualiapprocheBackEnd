package com.qualiapproche.common.config;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ce que l'appelant reçoit quand une requête échoue.
 *
 * <p>Deux exigences, que les outils d'audit (ZAP) vérifient : une erreur de l'appelant se dit avec
 * son statut — 400, 405, 413, 415 — et non en 500 « erreur interne » ; et aucune réponse ne livre
 * la mécanique interne — noms de classes, messages du JDK, chemin demandé renvoyé tel quel.</p>
 */
class GlobalExceptionHandlerTest {

    record Corps(@NotBlank String nom) {
    }

    @RestController
    static class ControleurDeTest {

        @PostMapping("/corps")
        String corps(@Valid @RequestBody Corps corps) {
            return corps.nom();
        }

        @GetMapping("/parametre")
        String parametre(@RequestParam String valeur) {
            return valeur;
        }

        @GetMapping("/enum")
        String enumeration(@RequestParam String valeur) {
            return Thread.State.valueOf(valeur).name();
        }

        @GetMapping("/uuid")
        String uuid(@RequestParam String valeur) {
            return UUID.fromString(valeur).toString();
        }

        @GetMapping("/metier")
        String metier() {
            throw new IllegalArgumentException("Type de document introuvable avec le code : X");
        }

        @GetMapping("/hibernate")
        String hibernate() {
            throw new EntityNotFoundException("Unable to find com.qualiapproche.amelioration.entities.NonConformite with id 42");
        }

        @GetMapping("/introuvable")
        String introuvable() {
            throw new EntityNotFoundException("Non-conformité non trouvée avec l'ID : 42");
        }

        @GetMapping("/televersement")
        String televersement() {
            throw new MaxUploadSizeExceededException(10);
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        mvc = MockMvcBuilders.standaloneSetup(new ControleurDeTest())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Corps JSON illisible : 400, sans le message de Jackson")
    void corpsIllisible() throws Exception {
        mvc.perform(post("/corps").contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("com."))))
                .andExpect(jsonPath("$.message", not(containsString("Jackson"))));
    }

    @Test
    @DisplayName("Corps absent : 400")
    void corpsAbsent() throws Exception {
        mvc.perform(post("/corps").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Corps invalide : 400 avec le champ en faute")
    void corpsInvalide() throws Exception {
        mvc.perform(post("/corps").contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nom")));
    }

    @Test
    @DisplayName("Paramètre obligatoire absent : 400 qui le nomme")
    void parametreAbsent() throws Exception {
        mvc.perform(get("/parametre"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("valeur")));
    }

    @Test
    @DisplayName("Type de contenu refusé : 415")
    void typeDeContenu() throws Exception {
        mvc.perform(post("/corps").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Verbe non pris en charge : 405 avec l'en-tête Allow")
    void verbe() throws Exception {
        mvc.perform(put("/parametre").param("valeur", "x"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    @DisplayName("Fichier trop volumineux : 413")
    void televersement() throws Exception {
        mvc.perform(get("/televersement")).andExpect(status().isPayloadTooLarge());
    }

    @Test
    @DisplayName("Message du JDK (énumération, UUID) : 400 neutre, sans nom de classe")
    void messagesDuJdk() throws Exception {
        mvc.perform(get("/enum").param("valeur", "INCONNU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("java."))))
                .andExpect(jsonPath("$.message", not(containsString("No enum constant"))));
        mvc.perform(get("/uuid").param("valeur", "pas-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("UUID string"))));
    }

    @Test
    @DisplayName("Message métier en français : transmis tel quel")
    void messageMetier() throws Exception {
        mvc.perform(get("/metier"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Type de document introuvable avec le code : X"));
    }

    @Test
    @DisplayName("Introuvable : le message d'Hibernate est masqué, celui du service est gardé")
    void introuvable() throws Exception {
        mvc.perform(get("/hibernate"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Élément introuvable."));
        mvc.perform(get("/introuvable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Non-conformité non trouvée avec l'ID : 42"));
    }

    @Test
    @DisplayName("Ressource absente : 404 qui ne répète pas le chemin demandé")
    void ressourceAbsente() {
        var reponse = new GlobalExceptionHandler().handleNoResourceFound(
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "<script>alert(1)</script>"));

        assertThat(reponse.getStatusCode().value()).isEqualTo(404);
        assertThat(reponse.getBody().getMessage()).doesNotContain("script");
    }

    @Test
    @DisplayName("Aucun contrôleur à cette adresse : 404 sans écho du chemin, et non 500")
    void aucunControleur() throws Exception {
        mvc.perform(get("/nulle-part/<script>"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ressource introuvable."));
    }

    @Test
    @DisplayName("Reconnaissance des messages techniques")
    void messagesTechniques() {
        assertThat(GlobalExceptionHandler.messageTechnique(null)).isTrue();
        assertThat(GlobalExceptionHandler.messageTechnique("No enum constant com.x.Statut.Y")).isTrue();
        assertThat(GlobalExceptionHandler.messageTechnique("For input string: \"abc\"")).isTrue();
        assertThat(GlobalExceptionHandler.messageTechnique("échec dans java.util.Optional.get")).isTrue();
        assertThat(GlobalExceptionHandler.messageTechnique("Demande introuvable : 12")).isFalse();
        assertThat(GlobalExceptionHandler.messageTechnique("Voir https://qualisira.com/aide")).isFalse();
    }
}
