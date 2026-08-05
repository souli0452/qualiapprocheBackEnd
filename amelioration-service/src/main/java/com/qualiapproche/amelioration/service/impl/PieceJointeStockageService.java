package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.mappers.PieceJointeMapper;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.common.dto.PieceJointeDTO;
import com.qualiapproche.storage.AbstractFichierService;
import com.qualiapproche.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pièces jointes du module amélioration — non-conformités et plans d'action — sur le serveur
 * d'objets.
 *
 * <p>Remplace l'ancien {@code PieceJointeService}, qui écrivait les fichiers sur le disque du
 * service à partir du base64 reçu : un fichier déposé sur un nœud restait illisible depuis les
 * autres et disparaissait au redéploiement du conteneur. Seul le rattachement à la persistance
 * reste ici ; le rangement, le nommage et la lecture du contenu viennent de
 * {@link AbstractFichierService}.</p>
 *
 * <p><b>Ce que le service ne fait plus :</b> rendre le contenu avec la liste des pièces. Le
 * chargement des pièces s'exécute sur chaque page de chaque liste de dossiers — rapatrier les
 * fichiers au passage aurait fait un aller-retour vers le serveur d'objets par pièce et par ligne.
 * La liste porte donc la référence, et le contenu se lit à la demande.</p>
 */
@Slf4j
@Service
@Transactional
public class PieceJointeStockageService extends AbstractFichierService<PieceJointe> {

    /** Dossier de premier niveau, commun aux fichiers du module. */
    private static final String DOSSIER_MODULE = "non-conformite";

    private final PieceJointeRepository pieceJointeRepository;
    private final PieceJointeMapper pieceJointeMapper;

    public PieceJointeStockageService(StorageService storageService,
                                      PieceJointeRepository pieceJointeRepository,
                                      PieceJointeMapper pieceJointeMapper) {
        super(storageService);
        this.pieceJointeRepository = pieceJointeRepository;
        this.pieceJointeMapper = pieceJointeMapper;
    }

    // ---------------------------------------------------------------- contrat du service abstrait

    @Override
    protected String dossierDuModule() {
        return DOSSIER_MODULE;
    }

    @Override
    protected PieceJointe creerPiece(UUID dossierId) {
        PieceJointe piece = new PieceJointe();
        piece.setEntityId(dossierId);
        return piece;
    }

    @Override
    protected PieceJointe enregistrer(PieceJointe piece) {
        return pieceJointeRepository.save(piece);
    }

    @Override
    protected List<PieceJointe> piecesDe(UUID dossierId) {
        return pieceJointeRepository.findAllByEntityIdAndDeposeParCircuitFalse(dossierId);
    }

    @Override
    protected void oublier(List<PieceJointe> pieces) {
        pieceJointeRepository.deleteAll(pieces);
    }

    // ---------------------------------------------------------------- usage par les modules

    /**
     * Pièces jointes d'un dossier : nom, type et référence, sans le contenu.
     *
     * <p>Les pièces déposées au fil du circuit en sont exclues — elles ne relèvent pas de la
     * saisie de l'utilisateur et n'ont pas à revenir dans ce qu'il renvoie.</p>
     */
    @Transactional(readOnly = true)
    public List<PieceJointeDTO> getPjByEntityId(UUID dossierId) {
        return piecesDe(dossierId).stream()
                .map(pieceJointeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Aligne les pièces d'un dossier sur ce que le client renvoie.
     *
     * <p>Trois cas, et c'est le troisième qui compte : une entrée porteuse d'un contenu est un
     * nouveau dépôt ; une entrée qui ne porte qu'une référence déjà connue désigne une pièce à
     * <b>conserver</b> ; une pièce que le client ne mentionne plus est supprimée.</p>
     *
     * <p>La mise à jour effaçait auparavant toutes les pièces avant de réécrire ce que le client
     * envoyait. Cela ne tenait que parce que le client recevait le contenu de chaque fichier et le
     * renvoyait tel quel. Il ne reçoit plus que des références : au premier enregistrement, les
     * pièces existantes auraient été détruites et remplacées par des lignes sans contenu.</p>
     *
     * @param sousDossier second niveau de rangement, typiquement le sigle de la structure
     */
    public void synchroniser(List<PieceJointeDTO> souhaitees, UUID dossierId, String sousDossier) {
        if (souhaitees == null || souhaitees.isEmpty()) {
            // Liste vide et liste absente sont traitées pareillement, faute de pouvoir les
            // distinguer : les écrans qui n'affichent pas les pièces jointes envoient l'une ou
            // l'autre selon leur formulaire, et l'enregistrement d'un dossier depuis l'un d'eux
            // supprimait toutes ses pièces. Retirer la dernière pièce d'un dossier passe par la
            // suppression de cette pièce, qui dit ce qu'elle fait.
            return;
        }

        Set<String> referencesConservees = souhaitees.stream()
                .map(PieceJointeDTO::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PieceJointe> obsoletes = piecesDe(dossierId).stream()
                .filter(piece -> !referencesConservees.contains(piece.getUrl()))
                .toList();
        if (!obsoletes.isEmpty()) {
            supprimer(obsoletes);
        }

        souhaitees.stream()
                .filter(dto -> dto.getFichier() != null && dto.getFichier().length > 0)
                .forEach(dto -> deposer(dto.getFichier(), dto.getNom(), dto.getType(), dossierId, sousDossier));
    }

    /** Pièce désignée par son identifiant. */
    @Transactional(readOnly = true)
    public PieceJointe piece(UUID pieceId) {
        return pieceJointeRepository.findById(pieceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
    }

    /** Contenu d'une pièce désignée par son identifiant. L'appelant referme le flux. */
    @Transactional(readOnly = true)
    public InputStream contenuParId(UUID pieceId) {
        return contenu(piece(pieceId));
    }

    /** Supprime une pièce, contenu compris. */
    public void deleteById(UUID pieceId) {
        supprimer(List.of(piece(pieceId)));
    }
}
