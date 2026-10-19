package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.storage.ReglesDePieceJointe;
import com.qualiapproche.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Le dépôt et la relecture des fichiers du module : rapports et plans reçus tout faits.
 *
 * <p>Le serveur de fichiers est <b>facultatif</b>, comme pour la FAQ : sans lui, le service
 * démarre, les audits se planifient et se conduisent, et seul le chargement d'un fichier est
 * refusé — en 503, avec un message qui dit ce qui manque.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FichiersAudit {

    /** Sous-dossier du serveur de fichiers, à côté de ceux des autres modules. */
    private static final String DOSSIER = "audit";

    private final ObjectProvider<StorageService> stockage;

    public String deposer(MultipartFile fichier, String... sousDossiers) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu.", HttpStatus.BAD_REQUEST);
        }
        ReglesDePieceJointe.verifier(fichier.getOriginalFilename(), fichier.getSize());
        String[] chemin = new String[sousDossiers.length + 1];
        chemin[0] = DOSSIER;
        System.arraycopy(sousDossiers, 0, chemin, 1, sousDossiers.length);
        try {
            return exigerLeStockage(objetDuDepot(sousDossiers)).uploadFile(fichier, chemin);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Dépôt du fichier d'audit {} impossible.", fichier.getOriginalFilename(), e);
            throw new BusinessException("Le dépôt du fichier a échoué : " + fichier.getOriginalFilename(),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Le nom sous lequel garder un fichier déposé : celui du poste de l'utilisateur, sans le
     * chemin que certains navigateurs y joignent, et borné à la taille de la colonne.
     */
    public static String nomDOrigine(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename();
        if (nom == null) {
            return null;
        }
        nom = nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1).strip();
        if (nom.isEmpty()) {
            return null;
        }
        return nom.length() <= 255 ? nom : nom.substring(nom.length() - 255);
    }

    public InputStream contenu(String reference) {
        if (reference == null) {
            throw new BusinessException("Aucun fichier n'a été chargé.", HttpStatus.NOT_FOUND);
        }
        try {
            return exigerLeStockage("La relecture d'un fichier").downloadFile(reference);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lecture du fichier d'audit {} impossible.", reference, e);
            throw new BusinessException("Le fichier est introuvable ou inaccessible.", HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * Retire l'objet du serveur de fichiers. Un échec ne remonte pas : la ligne est déjà partie,
     * et un objet orphelin ne gêne personne — il se balaie plus tard.
     */
    public void retirer(String reference) {
        try {
            StorageService service = stockage.getIfAvailable();
            if (service != null && reference != null) {
                service.deleteFile(reference);
            }
        } catch (Exception e) {
            log.warn("Fichier d'audit {} retiré de la base, mais pas du serveur de fichiers : {}",
                    reference, e.getMessage());
        }
    }

    public static String extensionDe(String nom) {
        if (nom == null) {
            return null;
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? null : nom.substring(point + 1).toLowerCase();
    }

    /**
     * Ce que l'utilisateur cherchait à faire, pour le lui dire s'il ne le peut pas : le message
     * parlait de « rapport ou plan » à qui joignait une preuve. Le sous-dossier qui suit
     * l'identifiant de l'audit le désigne.
     */
    private static String objetDuDepot(String... sousDossiers) {
        String nature = sousDossiers.length > 1 ? sousDossiers[1] : "";
        return switch (nature) {
            case "preuves" -> "Joindre une preuve";
            case "plan" -> "Charger un plan d'audit reçu";
            case "rapport" -> "Charger un rapport d'audit reçu";
            default -> "Charger un fichier";
        };
    }

    private StorageService exigerLeStockage(String objet) {
        StorageService service = stockage.getIfAvailable();
        if (service == null) {
            throw new BusinessException(objet + " demande un serveur de fichiers, que cette installation"
                    + " n'a pas encore configuré : prévenez l'administrateur. Le reste de l'audit reste"
                    + " utilisable.", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return service;
    }
}
