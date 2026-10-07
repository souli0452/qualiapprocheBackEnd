package com.qualiapproche.ia.client;

import com.qualiapproche.common.dto.FaqDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static com.qualiapproche.common.utils.ApiUrls.FAQ_PUBLIEES;
import static com.qualiapproche.common.utils.ApiUrls.FAQ_ROOT_URL;

/**
 * Ce que l'assistant lit du référentiel : la foire aux questions de l'organisation.
 *
 * <p>La FAQ ne lui appartient pas — elle vit dans referentiel-service, s'affiche dans l'aide et
 * vaut pour une installation qui ne souscrirait jamais au module ASSISTANT_IA. L'assistant n'en
 * est qu'un lecteur, et il la lit comme tout le reste : avec le jeton de la personne qui
 * l'interroge, jamais avec un jeton de service. Il ne voit donc que la FAQ de la direction de
 * son interlocuteur, et jamais un brouillon.</p>
 */
@FeignClient(name = "referentiel-service")
public interface ReferentielClient {

    @GetMapping(FAQ_ROOT_URL + FAQ_PUBLIEES)
    List<FaqDto> faqPubliee();
}
