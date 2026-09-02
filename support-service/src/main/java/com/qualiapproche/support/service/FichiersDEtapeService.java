package com.qualiapproche.support.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.storage.ReglesDePieceJointe;
import com.qualiapproche.storage.StorageService;
import com.qualiapproche.support.model.PieceJointeEtape;
import com.qualiapproche.support.repository.PieceJointeEtapeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pièces jointes réclamées par une étape de circuit, sur un document ou une demande.
 *
 * <p>Le moteur de workflow ne transporte que des chaînes : le client dépose d'abord la pièce, reçoit
 * sa référence, puis transmet cette référence comme valeur du champ de l'étape — « déposer d'abord,
 * référencer ensuite », la même convention que pour les non-conformités. Sans ce point d'entrée, une
 * étape documentaire réclamant un justificatif était indécidable depuis l'écran.</p>
 *
 * <p><b>Le nom d'origine est conservé en table.</b> La clé de l'objet ne reprend que l'extension —
 * deux fichiers homonymes s'y seraient écrasés — et le téléchargement aurait proposé un identifiant
 * technique. Dans un dossier d'audit, une pièce dont le nom est perdu perd la moitié de sa valeur.</p>
 *
 * <p><b>Le dossier de la pièce fait l'autorisation.</b> Il est lu en table, où il est inscrit au
 * dépôt. À défaut de ligne — pièce déposée avant cette table — le rangement dans le serveur de
 * fichiers tient lieu de preuve : la référence contient le chemin
 * {@code pieces-etape/<famille>/<dossier>/}. Sans l'un ou l'autre, le point d'entrée rendrait
 * n'importe quel objet du serveur de fichiers à qui sait lire un document.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FichiersDEtapeService {

    /** Racine commune : ces pièces ne sont ni des versions de document, ni des pièces de demande. */
    private static final String RACINE = "pieces-etape";

    private final StorageService storageService;
    private final PieceJointeEtapeRepository pieceRepository;
 
    /**
     * Dépose une pièce et rend sa référence.
     *
     * @param famille segment de rangement de la famille de dossiers ({@code "documents"}…)
     * @param dossier identifiant du dossier concerné
     * @param fichier pièce reçue en multipart
     * @return la référence de l'objet, à transmettre comme valeur du champ d'étape
     */
    @Transactional
    public String deposer(String famille, UUID dossier, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu : la pièce n'a pas été déposée.",
                    HttpStatus.BAD_REQUEST);
        }
        // Taille et type bornés avant tout rangement : la limite multipart du serveur (1 Go) n'est
        // pas une règle métier, et rien n'excluait un exécutable.
        try {
            ReglesDePieceJointe.verifier(fichier.getOriginalFilename(), fichier.getSize());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        String reference;
        try {
            reference = storageService.uploadFile(fichier, RACINE, famille, dossier.toString());
        } catch (Exception e) {
            // Le serveur de fichiers est indisponible ou refuse : la décision ne doit pas paraître
            // avoir été prise avec sa pièce.
            throw new BusinessException(
                    "Le dépôt de la pièce a échoué : " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }

        pieceRepository.save(PieceJointeEtape.builder()
                .reference(reference)
                .famille(famille)
                .dossierId(dossier)
                .nom(nomSain(fichier.getOriginalFilename()))
                .type(fichier.getContentType())
                .taille(fichier.getSize())
                .build());

        log.info("Pièce d'étape « {} » déposée pour {} {} : {}",
                fichier.getOriginalFilename(), famille, dossier, reference);
        return reference;
    }

    /**
     * Contenu d'une pièce déposée sur ce dossier.
     *
     * @throws BusinessException en 403 si la pièce ne relève pas du dossier demandé
     */
    public InputStream contenu(String famille, UUID dossier, String reference) {
        exigerQuElleAppartienneAuDossier(famille, dossier, reference);
        try {
            return storageService.downloadFile(reference);
        } catch (Exception e) {
            throw new BusinessException(
                    "Pièce introuvable ou illisible : " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Nom proposé au téléchargement : celui du dépôt.
     *
     * <p>À défaut de ligne — pièce déposée avant que cette table n'existe — le dernier segment de la
     * clé, qui reste un nom de fichier exploitable.</p>
     */
    public String nomPropose(String reference) {
        return pieceRepository.findByReference(reference)
                .map(PieceJointeEtape::getNom)
                .orElseGet(() -> dernierSegment(reference));
    }

    /** Type déclaré au dépôt, ou {@code null} : au téléchargement d'en tirer les conséquences. */
    public String typeDeContenu(String reference) {
        return pieceRepository.findByReference(reference)
                .map(PieceJointeEtape::getType)
                .orElse(null);
    }

    /** Pièces d'étape d'un dossier, de la plus ancienne à la plus récente. */
    public List<PieceJointeEtape> pieces(String famille, UUID dossier) {
        return pieceRepository.findByFamilleAndDossierIdOrderByCreatedAtAsc(famille, dossier);
    }

    /**
     * La pièce appartient-elle bien à ce dossier ?
     *
     * <p>La table fait autorité : c'est elle qui a enregistré le dossier au dépôt. Le rangement dans
     * le serveur de fichiers sert de repli pour les pièces antérieures — sans lui, une référence
     * déposée avant cette table cesserait brusquement d'être téléchargeable.</p>
     */
    private void exigerQuElleAppartienneAuDossier(String famille, UUID dossier, String reference) {
        if (reference == null || reference.isBlank()) {
            throw new BusinessException("Aucune pièce désignée.", HttpStatus.BAD_REQUEST);
        }

        Optional<PieceJointeEtape> piece = pieceRepository.findByReference(reference);
        if (piece.isPresent()) {
            boolean memeDossier = famille.equals(piece.get().getFamille())
                    && dossier.equals(piece.get().getDossierId());
            if (!memeDossier) {
                throw refus();
            }
            return;
        }

        if (!reference.startsWith(RACINE + "/" + famille + "/" + dossier + "/")) {
            throw refus();
        }
    }

    private BusinessException refus() {
        return new BusinessException("Cette pièce n'appartient pas à ce dossier.", HttpStatus.FORBIDDEN);
    }

    /**
     * Nom de fichier ramené à son dernier segment.
     *
     * <p>Certains navigateurs transmettent un chemin complet, et une valeur qui ressemble à un chemin
     * se retrouverait dans un en-tête {@code Content-Disposition}.</p>
     */
    private String nomSain(String nomDepose) {
        String nom = dernierSegment(nomDepose == null ? "" : nomDepose.replace('\\', '/'))
                .replace("\"", "").trim();
        return nom.isEmpty() ? "piece-jointe" : nom;
    }

    private String dernierSegment(String chemin) {
        int separateur = chemin.lastIndexOf('/');
        return separateur >= 0 ? chemin.substring(separateur + 1) : chemin;
    }
}
