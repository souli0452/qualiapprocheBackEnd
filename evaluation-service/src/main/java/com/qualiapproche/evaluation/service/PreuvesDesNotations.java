package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.PreuveDto;
import com.qualiapproche.evaluation.entities.NotationCritere;
import com.qualiapproche.evaluation.entities.PreuveNotation;
import com.qualiapproche.evaluation.enumeration.StatutEvaluation;
import com.qualiapproche.evaluation.repository.NotationCritereRepository;
import com.qualiapproche.evaluation.repository.PreuveNotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Les pièces justificatives d'une notation RQAP-BF : elles se joignent et se retirent tant que
 * l'évaluation est en brouillon, se lisent ensuite avec elle (recette du 17 oct. 2026, observation 31).
 */
@Service
@RequiredArgsConstructor
public class PreuvesDesNotations {

    private final NotationCritereRepository notations;
    private final PreuveNotationRepository preuves;
    private final FichiersAudit fichiers;

    @Transactional(readOnly = true)
    public List<PreuveDto> lister(UUID notationId) {
        NotationCritere n = visible(notationId);
        return preuves.findAllByNotationIdOrderByCreatedAtAsc(n.getId()).stream().map(PreuvesDesNotations::versDto).toList();
    }

    @Transactional
    public List<PreuveDto> joindre(UUID notationId, List<MultipartFile> aDeposer) {
        NotationCritere n = modifiable(notationId);
        List<PreuveDto> posees = new ArrayList<>();
        for (MultipartFile fichier : aDeposer == null ? List.<MultipartFile>of() : aDeposer) {
            if (fichier == null || fichier.isEmpty()) {
                continue;
            }
            String reference = fichiers.deposer(fichier, n.getEvaluation().getAudit().getId().toString(), "preuves",
                    "rqapbf", n.getId().toString());
            posees.add(versDto(preuves.save(PreuveNotation.builder()
                    .notationId(n.getId())
                    .nom(FichiersAudit.nomDOrigine(fichier))
                    .ext(FichiersAudit.extensionDe(fichier.getOriginalFilename()))
                    .type(fichier.getContentType())
                    .url(reference)
                    .build())));
        }
        return posees;
    }

    @Transactional(readOnly = true)
    public PreuveNotation preuve(UUID preuveId) {
        PreuveNotation p = preuves.findById(preuveId)
                .orElseThrow(() -> new BusinessException("Pièce introuvable.", HttpStatus.NOT_FOUND));
        visible(p.getNotationId());
        return p;
    }

    @Transactional
    public void retirer(UUID preuveId) {
        PreuveNotation p = preuves.findById(preuveId)
                .orElseThrow(() -> new BusinessException("Pièce introuvable.", HttpStatus.NOT_FOUND));
        modifiable(p.getNotationId());
        preuves.delete(p);
        fichiers.retirer(p.getUrl());
    }

    public java.io.InputStream contenu(PreuveNotation p) {
        return fichiers.contenu(p.getUrl());
    }

    private NotationCritere visible(UUID notationId) {
        NotationCritere n = notations.findById(notationId)
                .orElseThrow(() -> new BusinessException("Notation introuvable.", HttpStatus.NOT_FOUND));
        java.util.UUID direction = Perimetre.exigerUneDirection();
        if (!direction.equals(n.getEvaluation().getDirectionId())) {
            throw new BusinessException("Notation introuvable.", HttpStatus.NOT_FOUND);
        }
        return n;
    }

    private NotationCritere modifiable(UUID notationId) {
        NotationCritere n = visible(notationId);
        if (n.getEvaluation().getStatut() != StatutEvaluation.BROUILLON) {
            throw new BusinessException("L'évaluation est validée : ses pièces ne se modifient plus.", HttpStatus.CONFLICT);
        }
        return n;
    }

    private static PreuveDto versDto(PreuveNotation p) {
        return PreuveDto.builder().id(p.getId()).nom(p.getNom()).ext(p.getExt()).type(p.getType()).build();
    }
}
