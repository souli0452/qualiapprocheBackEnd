package com.qualiapproche.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des pièces jointes d'un dossier métier, entièrement sur le serveur d'objets.
 *
 * <p>Chaque module qui manipule des fichiers en dérive une classe et n'a plus à écrire que sa
 * persistance : le rangement, le nommage, la lecture et la suppression du contenu sont ici, une
 * seule fois. Les modules écrivaient jusqu'alors sur le disque du service — un fichier déposé sur
 * un nœud restait illisible depuis les autres et disparaissait au redéploiement du conteneur.</p>
 *
 * <p>Le contenu ne transite plus par la base ni par les objets de transfert : ce que ce service
 * conserve d'un fichier, c'est sa <b>référence</b>. Elle suffit à le relire à la demande, et évite
 * qu'une liste de dossiers ne rapatrie au passage tous leurs fichiers.</p>
 *
 * @param <E> entité de pièce jointe propre au module
 */
@Slf4j
public abstract class AbstractFichierService<E extends FichierStocke> {

    protected final StorageService storageService;

    protected AbstractFichierService(StorageService storageService) {
        this.storageService = storageService;
    }

    // ---------------------------------------------------------------- à fournir par le module

    /**
     * Dossier de premier niveau sous lequel ranger les fichiers du module
     * (ex. {@code "non-conformite"}).
     */
    protected abstract String dossierDuModule();

    /** Instancie une pièce vide, déjà rattachée au dossier désigné. */
    protected abstract E creerPiece(UUID dossierId);

    /** Enregistre la pièce et rend l'instance persistée. */
    protected abstract E enregistrer(E piece);

    /** Pièces rattachées à un dossier. */
    protected abstract List<E> piecesDe(UUID dossierId);

    /** Retire les pièces de la base — le contenu, lui, est supprimé par ce service. */
    protected abstract void oublier(List<E> pieces);

    // ---------------------------------------------------------------- comportement commun

    /**
     * Dépose un fichier reçu en multipart et enregistre la pièce correspondante.
     *
     * @param sousDossier second niveau de rangement (sigle de structure, type de document…) ;
     *                    {@code null} accepté, un dossier de repli est alors employé
     */
    public E deposer(MultipartFile fichier, UUID dossierId, String sousDossier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new StorageException("Aucun fichier n'a été transmis.");
        }
        // Bornes communes à tous les modules : sans elles, seul le plafond multipart du serveur
        // (1 Go) s'appliquait, et rien n'excluait un exécutable déposé comme justificatif.
        ReglesDePieceJointe.verifier(fichier.getOriginalFilename(), fichier.getSize());
        String reference = executer(
                () -> storageService.uploadFile(fichier, dossierDuModule(), sousDossier),
                "dépôt du fichier « " + fichier.getOriginalFilename() + " »");

        return enregistrerPiece(dossierId, reference, fichier.getOriginalFilename(), fichier.getContentType());
    }

    /**
     * Dépose un contenu déjà en mémoire.
     *
     * <p>Sert aux écrans qui envoient encore leurs pièces encodées en base64 dans le corps de la
     * requête : le fichier rejoint le serveur d'objets comme les autres, sans qu'il faille
     * d'abord reprendre l'écran.</p>
     */
    public E deposer(byte[] contenu, String nom, String type, UUID dossierId, String sousDossier) {
        if (contenu == null || contenu.length == 0) {
            throw new StorageException("Le fichier « " + nom + " » est vide.");
        }
        ReglesDePieceJointe.verifier(nom, contenu.length);
        String reference = executer(
                () -> storageService.uploadContent(contenu, nom, type, dossierDuModule(), sousDossier),
                "dépôt du fichier « " + nom + " »");

        return enregistrerPiece(dossierId, reference, nom, type);
    }

    /**
     * Contenu d'une pièce. L'appelant referme le flux.
     */
    public InputStream contenu(E piece) {
        return executer(() -> storageService.downloadFile(piece.getUrl()),
                "lecture du fichier « " + piece.getNom() + " »");
    }

    /**
     * Supprime les pièces d'un dossier, contenu compris.
     *
     * <p>L'échec de la suppression d'un objet n'interrompt pas le reste : la pièce est retirée de
     * la base de toute façon. Un objet orphelin sur le serveur coûte de l'espace ; une pièce qui
     * subsiste en base sans contenu se présente à l'utilisateur comme un fichier qu'il ne peut pas
     * ouvrir.</p>
     */
    public void supprimerTout(UUID dossierId) {
        supprimer(piecesDe(dossierId));
    }

    /**
     * Supprime des pièces désignées, contenu compris. Voir {@link #supprimerTout(UUID)} pour le
     * traitement d'un contenu introuvable.
     */
    public void supprimer(List<E> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            return;
        }
        pieces.forEach(this::supprimerContenu);
        oublier(pieces);
    }

    private E enregistrerPiece(UUID dossierId, String reference, String nom, String type) {
        E piece = creerPiece(dossierId);
        piece.setNom(nom);
        piece.setExt(extension(nom));
        piece.setType(type);
        piece.setUrl(reference);
        return enregistrer(piece);
    }

    private void supprimerContenu(E piece) {
        if (piece.getUrl() == null || piece.getUrl().isBlank()) {
            return;
        }
        try {
            storageService.deleteFile(piece.getUrl());
        } catch (Exception e) {
            log.warn("Le contenu de « {} » ({}) n'a pas pu être supprimé du serveur d'objets ; "
                    + "la pièce est retirée malgré tout.", piece.getNom(), piece.getUrl(), e);
        }
    }

    private String extension(String nom) {
        if (nom == null || !nom.contains(".")) {
            return null;
        }
        return nom.substring(nom.lastIndexOf('.') + 1);
    }

    /**
     * Exécute une opération de stockage en traduisant son échec.
     *
     * <p>Le client MinIO déclare {@code throws Exception} : laissé tel quel, il oblige chaque
     * appelant à un {@code try/catch} qui finit invariablement par avaler l'erreur ou par la
     * remonter en erreur serveur nue.</p>
     */
    private <T> T executer(OperationDeStockage<T> operation, String description) {
        try {
            return operation.executer();
        } catch (Exception e) {
            log.error("Échec du stockage — {}", description, e);
            throw new StorageException("Le serveur de fichiers n'a pas pu traiter cette demande : "
                    + description + ".", e);
        }
    }

    @FunctionalInterface
    private interface OperationDeStockage<T> {
        T executer() throws Exception;
    }
}
