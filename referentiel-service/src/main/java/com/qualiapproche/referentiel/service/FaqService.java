package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.FaqDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** La foire aux questions : ce que l'organisation écrit, ce que ses utilisateurs consultent. */
public interface FaqService {

    FaqDto create(FaqDto dto);

    FaqDto update(FaqDto dto);

    FaqDto getById(UUID id);

    /**
     * Pour l'administration : d'un côté ou de l'autre de la publication, paginé et filtré.
     *
     * <p>L'écran présente deux onglets et demande donc toujours l'un des deux états.</p>
     */
    Page<FaqDto> getAll(boolean publiee, String recherche, Pageable pageable);

    /** Combien d'entrées de chaque côté, pour les onglets. */
    long compter(boolean publiee);

    /**
     * Pour l'administration : tout, publié ou non, en entier.
     *
     * <p>C'est ce que l'écran demande par {@code /all}, comme les autres référentiels : le
     * service de pagination du front y passe page et taille, que ce point d'entrée ignore —
     * une liste tronquée à dix valeurs tairait les suivantes sans que rien ne l'indique.</p>
     */
    List<FaqDto> getAll();

    /**
     * Pour l'aide et pour l'assistant IA : les entrées publiées, dans l'ordre d'affichage.
     *
     * <p>Sans pagination, à dessein — les deux lecteurs en ont besoin en entier : l'aide pour
     * l'afficher, l'assistant pour la joindre à sa consigne. C'est aussi pourquoi la réponse est
     * bornée à mille cinq cents caractères à la saisie.</p>
     */
    List<FaqDto> getPubliees();

    void delete(UUID id);
}
