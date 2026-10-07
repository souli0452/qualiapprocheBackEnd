package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.FichierFaqDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.referentiel.entities.Faq;
import com.qualiapproche.referentiel.entities.FichierFaq;
import com.qualiapproche.referentiel.repository.FaqRepository;
import com.qualiapproche.referentiel.repository.FichierFaqRepository;
import com.qualiapproche.referentiel.service.FichierFaqService;
import com.qualiapproche.storage.ReglesDePieceJointe;
import com.qualiapproche.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Les pièces jointes de la FAQ.
 *
 * <p>Le service de fichiers est <b>facultatif</b>, et c'est délibéré : son auto-configuration ne
 * s'active que si l'installation renseigne l'adresse du serveur de fichiers. Sans elle, ce
 * service démarre quand même, la FAQ fonctionne, et seul le dépôt d'une pièce est refusé — avec
 * un message qui dit ce qui manque. L'exiger aurait rendu obligatoires des variables dont
 * referentiel-service n'avait jusqu'ici nul besoin, et un service qui ne démarre plus est un
 * prix disproportionné pour une fonction d'appoint.</p>
 *
 * <p>Tout est borné à la direction de l'appelant par l'entrée de FAQ à laquelle la pièce
 * appartient : on ne télécharge pas le document d'une autre organisation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FichierFaqServiceImpl implements FichierFaqService {

    /** Sous-dossier du serveur de fichiers, à côté de ceux des autres modules. */
    private static final String DOSSIER = "faq";

    private final FichierFaqRepository fichiers;
    private final FaqRepository entrees;
    private final ObjectProvider<StorageService> stockage;

    @Override
    @Transactional
    public List<FichierFaqDto> deposer(UUID faqId, List<MultipartFile> aDeposer) {
        Faq entree = sienneOuRien(faqId);
        StorageService service = exigerLeStockage();

        List<FichierFaqDto> posees = new ArrayList<>();
        for (MultipartFile fichier : aDeposer) {
            if (fichier == null || fichier.isEmpty()) {
                continue;
            }
            // Taille et extension : les mêmes règles que pour les pièces des dossiers, pour que
            // ce qui est refusé ailleurs le soit ici aussi.
            ReglesDePieceJointe.verifier(fichier.getOriginalFilename(), fichier.getSize());
            try {
                String reference = service.uploadFile(fichier, DOSSIER, entree.getId().toString());
                FichierFaq pose = fichiers.save(FichierFaq.builder()
                        .faqId(entree.getId())
                        .nom(fichier.getOriginalFilename())
                        .ext(extensionDe(fichier.getOriginalFilename()))
                        .type(fichier.getContentType())
                        .url(reference)
                        .build());
                posees.add(versDto(pose));
            } catch (Exception e) {
                log.error("Dépôt de la pièce jointe de FAQ {} impossible.", entree.getId(), e);
                throw new BusinessException(
                        "Le dépôt de la pièce jointe a échoué : " + fichier.getOriginalFilename(),
                        HttpStatus.BAD_GATEWAY);
            }
        }
        return posees;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FichierFaqDto> desEntrees(List<UUID> faqIds) {
        if (faqIds == null || faqIds.isEmpty()) {
            return List.of();
        }
        return fichiers.findAllByFaqIdIn(faqIds).stream().map(this::versDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream contenu(UUID fichierId) {
        FichierFaq fichier = siennOuRien(fichierId);
        try {
            return exigerLeStockage().downloadFile(fichier.getUrl());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lecture de la pièce jointe de FAQ {} impossible.", fichierId, e);
            throw new BusinessException(
                    "La pièce jointe est introuvable ou inaccessible.", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FichierFaqDto decrire(UUID fichierId) {
        return versDto(siennOuRien(fichierId));
    }

    @Override
    @Transactional
    public void supprimer(UUID fichierId) {
        FichierFaq fichier = siennOuRien(fichierId);
        // La ligne part d'abord : une référence qui pointe dans le vide se voit à l'écran, tandis
        // qu'un objet orphelin sur le serveur de fichiers ne gêne personne et se balaie plus tard.
        fichiers.delete(fichier);
        try {
            StorageService service = stockage.getIfAvailable();
            if (service != null) {
                service.deleteFile(fichier.getUrl());
            }
        } catch (Exception e) {
            log.warn("Pièce jointe de FAQ {} retirée de la base, mais pas du serveur de"
                    + " fichiers : {}", fichierId, e.getMessage());
        }
    }

    /**
     * Le service de fichiers, ou un refus qui nomme ce qui manque.
     *
     * <p>Un 503 et non un 500 : rien n'est cassé, l'installation n'a simplement pas configuré de
     * serveur de fichiers. Le message doit permettre à l'administrateur de comprendre sans lire
     * les journaux.</p>
     */
    private StorageService exigerLeStockage() {
        StorageService service = stockage.getIfAvailable();
        if (service == null) {
            throw new BusinessException(
                    "Les pièces jointes de la FAQ demandent un serveur de fichiers, que cette"
                            + " installation n'a pas configuré. La FAQ reste utilisable sans.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        return service;
    }

    private Faq sienneOuRien(UUID faqId) {
        UUID directionId = SecurityUtils.getCurrentDirectionId();
        return entrees.findById(faqId)
                .filter(entree -> directionId != null && directionId.equals(entree.getDirectionId()))
                .orElseThrow(() -> new BusinessException(
                        "Aucune entrée de FAQ ne porte cet identifiant : " + faqId,
                        HttpStatus.NOT_FOUND));
    }

    private FichierFaq siennOuRien(UUID fichierId) {
        FichierFaq fichier = fichiers.findById(fichierId)
                .orElseThrow(() -> new BusinessException(
                        "Aucune pièce jointe ne porte cet identifiant : " + fichierId,
                        HttpStatus.NOT_FOUND));
        // L'appartenance se juge sur l'entrée, pas sur la pièce : c'est l'entrée qui porte la
        // direction dans le modèle, et la pièce n'en est qu'une dépendance.
        sienneOuRien(fichier.getFaqId());
        return fichier;
    }

    private String extensionDe(String nom) {
        if (nom == null) {
            return null;
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? null : nom.substring(point + 1).toLowerCase();
    }

    private FichierFaqDto versDto(FichierFaq fichier) {
        return FichierFaqDto.builder()
                .id(fichier.getId())
                .faqId(fichier.getFaqId())
                .nom(fichier.getNom())
                .ext(fichier.getExt())
                .type(fichier.getType())
                .build();
    }
}
