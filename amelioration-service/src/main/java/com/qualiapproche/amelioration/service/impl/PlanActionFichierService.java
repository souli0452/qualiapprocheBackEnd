package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.PlanAction;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.amelioration.repository.PlanActionRepository;
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
 * Fichiers justificatifs d'un plan d'action.
 *
 * <p>Le dialogue de traitement proposait déjà de joindre des pièces : elles étaient converties en
 * base64 dans le navigateur et posées sur l'objet envoyé au serveur, où <b>rien ne les
 * enregistrait</b>. L'écran annonçait le succès, la pièce disparaissait au rechargement, et le
 * responsable qualité vérifiait l'efficacité d'une action sans les justificatifs qu'on croyait
 * avoir fournis.</p>
 *
 * <p>Le dépôt suit désormais le même chemin que celui des non-conformités — objet rangé sous le
 * sigle de la structure, pièce rattachée au dossier par son identifiant. Le plan est rangé sous la
 * structure de la non-conformité qui le motive : c'est elle qui répond de l'action.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanActionFichierService {

    private final PlanActionRepository planActionRepository;
    private final NonConformiteRepository nonConformiteRepository;
    private final PieceJointeRepository pieceJointeRepository;
    private final PieceJointeStockageService stockage;
    private final NonConformiteFichierService ncFichierService;

    /** Dépose un fichier sur un plan d'action et rend la description de la pièce créée. */
    @Transactional
    public PieceJointeDTO deposer(UUID planActionId, MultipartFile fichier) {
        PlanAction plan = planActionRepository.findById(planActionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ce plan d'action n'existe pas."));

        PieceJointe piece = stockage.deposer(fichier, planActionId, dossierDuPlan(plan));
        pieceJointeRepository.save(piece);

        log.info("Plan d'action {} : fichier « {} » déposé sous {}", planActionId, piece.getNom(), piece.getUrl());
        return stockage.getPjByEntityId(planActionId).stream()
                .filter(pj -> piece.getUrl().equals(pj.getUrl()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La pièce déposée n'est pas relisible."));
    }

    /** Pièces jointes du plan, sans leur contenu. */
    @Transactional(readOnly = true)
    public List<PieceJointeDTO> pieces(UUID planActionId) {
        return stockage.getPjByEntityId(planActionId);
    }

    /**
     * Sigle de structure sous lequel ranger les pièces du plan.
     *
     * <p>Celui de la non-conformité, et non celui du responsable du plan : une ré-attribution
     * déplacerait sinon les fichiers déjà déposés hors du dossier auquel ils se rapportent.</p>
     */
    private String dossierDuPlan(PlanAction plan) {
        if (plan.getNonConformeId() == null) {
            return null;
        }
        NonConformite nc = nonConformiteRepository.findById(plan.getNonConformeId()).orElse(null);
        return nc == null ? null : ncFichierService.dossierDeStructure(nc);
    }
}
