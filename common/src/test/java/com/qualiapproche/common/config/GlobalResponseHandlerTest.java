package com.qualiapproche.common.config;

import com.qualiapproche.common.response.ApiResponse;
import com.qualiapproche.common.response.PaginatedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enveloppe appliquée aux réponses, et le piège qu'elle tend.
 *
 * <p>Toute réponse de type {@code List} est paginée d'office, à dix éléments par défaut : le corps
 * cesse d'être un tableau et devient un objet de pagination, dont les éléments suivants sont
 * absents sans que rien ne le signale. Un contrôleur qui sert une liste destinée à un sélecteur
 * doit donc l'envelopper lui-même dans un {@link ApiResponse}, seule forme que cet intercepteur
 * laisse passer intacte.</p>
 */
class GlobalResponseHandlerTest {

    private final GlobalResponseHandler handler = new GlobalResponseHandler();

    private ServerHttpRequest requete() {
        return new ServletServerHttpRequest(new MockHttpServletRequest("GET", "/api/v1/referentiel"));
    }

    private Object ecrire(Object corps) {
        return handler.beforeBodyWrite(corps, null, null, null, requete(), null);
    }

    @Test
    @DisplayName("Une liste nue est paginée : le corps n'est plus un tableau et se tronque à dix")
    void listeNue_paginee() {
        List<String> aQuinzeValeurs = IntStream.range(0, 15).mapToObj(i -> "valeur" + i).toList();

        Object aCorps = ecrire(aQuinzeValeurs);

        assertThat(aCorps).isInstanceOf(ApiResponse.class);
        Object aDonnees = ((ApiResponse<?>) aCorps).getData();
        assertThat(aDonnees)
                .withFailMessage("Le corps devrait être un objet de pagination, pas un tableau : "
                        + "c'est ce qui fait échouer un sélecteur qui attend une liste.")
                .isInstanceOf(PaginatedResponse.class);
        assertThat(((PaginatedResponse<?>) aDonnees).getContent()).hasSize(10);
    }

    @Test
    @DisplayName("Une liste déjà enveloppée traverse intacte, entière et sous forme de tableau")
    void listeEnveloppee_intacte() {
        List<String> aQuinzeValeurs = IntStream.range(0, 15).mapToObj(i -> "valeur" + i).toList();

        Object aCorps = ecrire(ApiResponse.success(aQuinzeValeurs));

        assertThat(aCorps).isInstanceOf(ApiResponse.class);
        // C'est la forme qu'attendent les sélecteurs du front : data est le tableau lui-même.
        assertThat(((ApiResponse<?>) aCorps).getData())
                .isInstanceOf(List.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(15);
    }
}
