package com.qualiapproche.referentiel.controller;

import com.qualiapproche.common.dto.FaqDto;
import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.referentiel.service.FaqService;
import com.qualiapproche.referentiel.service.FichierFaqService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Les chemins de lecture de la FAQ, et la forme de ce qu'ils rendent.
 *
 * <p>Deux défauts de mise en ligne tiennent à ce fichier, et aucun ne se voyait à la compilation.
 * Un {@code @GetMapping("/{id}")} nu happait {@code /all} et {@code /publiees}, que Spring lui
 * présentait comme des identifiants — « Invalid UUID string: all », en 500, sur la simple
 * consultation de la liste. Et une réponse de type {@code List} rendue nue se fait paginer
 * d'office par GlobalResponseHandler, à dix éléments : l'écran aurait montré dix questions sur
 * trente sans que rien ne l'indique.</p>
 *
 * <p>Ces cas lisent les annotations plutôt que d'appeler le service : c'est la déclaration qui
 * était fautive, pas le comportement.</p>
 */
class CheminsDeLaFaqTest {

    private final FaqController controleur =
            new FaqController(mock(FaqService.class), mock(FichierFaqService.class));

    private Set<String> cheminsDeLecture() {
        return Arrays.stream(FaqController.class.getDeclaredMethods())
                .map(methode -> methode.getAnnotation(GetMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(mapping -> mapping.value().length == 0
                        ? java.util.stream.Stream.of("")
                        : Arrays.stream(mapping.value()))
                .collect(Collectors.toSet());
    }

    private Method methode(String nom) {
        return Arrays.stream(FaqController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(nom))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("aucun chemin de lecture n'est un identifiant nu, qui happerait /all et /publiees")
    void aucunIdentifiantNu() {
        assertThat(cheminsDeLecture())
                .as("un /{id} à la racine intercepte tous les sous-chemins littéraux")
                .doesNotContain("/{id}");
    }

    @Test
    @DisplayName("la convention du dépôt est suivie : /all, /get/{id}, et la page à la racine")
    void laConventionEstSuivie() {
        assertThat(cheminsDeLecture()).contains("/all", "/get/{id}", "/publiees", "");
    }

    @Test
    @DisplayName("les listes sont enveloppées : sans cela l'intercepteur les pagine à dix")
    void lesListesSontEnveloppees() {
        // Le type de retour porte la garantie : ApiResponse est la seule forme que
        // GlobalResponseHandler laisse passer intacte.
        for (String nom : List.of("all", "publiees")) {
            assertThat(methode(nom).getGenericReturnType().getTypeName())
                    .as("%s doit rendre une liste enveloppée dans ApiResponse", nom)
                    .contains(ApiResponse.class.getName())
                    .contains(FaqDto.class.getName());
        }
    }

    @Test
    @DisplayName("la racine du contrôleur est bien celle du contrat partagé")
    void laRacineEstCelleDuContrat() {
        assertThat(FaqController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly(com.qualiapproche.common.utils.ApiUrls.FAQ_ROOT_URL);
        assertThat(controleur).isNotNull();
    }
}
