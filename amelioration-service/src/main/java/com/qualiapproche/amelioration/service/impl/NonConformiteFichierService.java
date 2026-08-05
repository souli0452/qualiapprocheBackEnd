package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.common.dto.PieceJointeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Fichiers d'une non-conformité : ce que le module sait de ses dossiers, appliqué au stockage.
 *
 * <p>{@link PieceJointeStockageService} sait déposer et relire ; cette façade sait <b>où</b> ranger
 * — sous le sigle de la structure, comme le module documentaire — et <b>pour qui</b>. Le sigle est
 * lu sur la non-conformité et non reçu de l'appelant : un client n'a pas à décider dans quel
 * dossier de structure un fichier atterrit.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonConformiteFichierService {

    private final NonConformiteRepository nonConformiteRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final PieceJointeStockageService stockage;

    /**
     * Dépose un fichier reçu en multipart et rend la référence de l'objet créé.
     *
     * <p>La pièce est marquée comme déposée par le circuit : elle accompagne une décision, elle
     * n'appartient pas aux pièces jointes que l'utilisateur compose sur la fiche — et l'alignement
     * de celles-ci ne doit donc jamais l'emporter.</p>
     */
    @Transactional
    public String deposer(UUID nonConformiteId, MultipartFile fichier) {
        NonConformite nc = chargerNc(nonConformiteId);

        PieceJointe piece = stockage.deposer(fichier, nonConformiteId, dossierDeStructure(nc));
        piece.setDeposeParCircuit(true);
        pieceJointeRepository.save(piece);

        log.info("Non-conformité {} : fichier « {} » déposé sous {}",
                nonConformiteId, piece.getNom(), piece.getUrl());
        return piece.getUrl();
    }

    /**
     * Aligne les pièces jointes de la fiche sur ce que le client renvoie, rangées sous la
     * structure de la non-conformité.
     */
    @Transactional
    public void synchroniser(List<PieceJointeDTO> souhaitees, UUID nonConformiteId) {
        NonConformite nc = nonConformiteRepository.findById(nonConformiteId).orElse(null);
        stockage.synchroniser(souhaitees, nonConformiteId, nc == null ? null : dossierDeStructure(nc));
    }

    /**
     * Pièce désignée par sa référence, à condition qu'elle appartienne bien à cette
     * non-conformité : la référence circule côté client, elle ne vaut pas autorisation de lire le
     * fichier d'un autre dossier.
     */
    @Transactional(readOnly = true)
    public PieceJointe pieceJointe(UUID nonConformiteId, String reference) {
        return pieceJointeRepository.findByUrlAndEntityId(reference, nonConformiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun fichier de cette non-conformité ne porte cette référence."));
    }

    private NonConformite chargerNc(UUID nonConformiteId) {
        return nonConformiteRepository.findById(nonConformiteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Non conformité introuvable"));
    }

    /**
     * Sigle de la structure sous laquelle ranger les fichiers de cette non-conformité.
     *
     * <p>La structure responsable prime : c'est elle qui traite le dossier. À défaut, on retombe
     * sur le libellé court du service émetteur puis sur la structure de soumission — une NC en
     * cours de qualification n'a pas encore de responsable, et son fichier ne doit pas pour autant
     * finir hors de tout dossier.</p>
     */
    public String dossierDeStructure(NonConformite nc) {
        return premierRenseigne(
                nc.getStructureResponsableSigle(),
                nc.getOrigineServiceLibelleCourt(),
                nc.getStructureSoumissionLibelle());
    }

    private String premierRenseigne(String... valeurs) {
        for (String valeur : valeurs) {
            if (valeur != null && !valeur.isBlank()) {
                return valeur;
            }
        }
        // Le service de stockage retombe alors sur son dossier de repli.
        return null;
    }
}
