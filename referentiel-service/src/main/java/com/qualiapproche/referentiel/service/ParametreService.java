package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.ParametreDto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Réglages de l'organisation, désignés par une clé immuable. */
public interface ParametreService {

    ParametreDto create(ParametreDto dto);

    /**
     * Met à jour la valeur et les intitulés — jamais la clé.
     *
     * @throws BusinessException en 409 si une clé différente est
     *         soumise : c'est par elle que le code désigne le réglage.
     */
    ParametreDto update(UUID id, ParametreDto dto);

    ParametreDto getById(UUID id);

    ParametreDto getByCle(String cle);

    List<ParametreDto> getAll(String recherche);

    void delete(UUID id);

    /**
     * Réglages lisibles sans habilitation, indexés par clé.
     *
     * <p>Forme attendue par ce qui les consomme : un pied de page de courriel demande « le
     * téléphone » sans savoir combien de réglages existent ni dans quel ordre ils sont rangés.</p>
     */
    Map<String, String> valeursPubliques();
}
