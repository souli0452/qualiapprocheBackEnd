package com.qualiapproche.amelioration.client;

import com.qualiapproche.amelioration.config.FeignConfig;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.dto.TypeProcessusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.*;

@FeignClient(name = "referentiel-service", configuration = FeignConfig.class)
public interface ReferentielClient {

    @GetMapping(ROOT_STRUCTURE_API + STRUCTURE_BY_ID)
    StructureDto getStructureById(@PathVariable("id") UUID id);

    /**
     * Réglages de l'organisation renseignés, indexés par clé.
     *
     * <p>Remplace l'ancienne configuration globale : les mêmes informations y sont désormais des
     * réglages clé/valeur, que l'organisation étend sans qu'on livre du code. Rendu directement en
     * carte, le décodeur Feign de ce service ôtant l'enveloppe {@code ApiResponse}.</p>
     */
    @GetMapping(PARAMETRE_ROOT_URL + "/publics")
    Map<String, String> parametresPublics();

    @GetMapping(TYPE_PROCESSUS_ROOT_URL + GET_TYPE_PROCESSUS_BY_ID)
    TypeProcessusDto getTypeProcessusById(@PathVariable("id") UUID id);
}
