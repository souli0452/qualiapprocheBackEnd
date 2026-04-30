package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.entities.PieceJointe;
import com.qualiapproche.amelioration.entities.mappers.PieceJointeMapper;
import com.qualiapproche.amelioration.repository.PieceJointeRepository;
import com.qualiapproche.common.dto.PieceJointeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PieceJointeService {

    private final PieceJointeMapper pieceJointeMapper;
    private final PieceJointeRepository pieceJointeRepository;

    @Value("${docs.pj.directory}")
    private String pjDirectory;

    public void savePj(List<PieceJointeDTO> pieceJointeDtos, UUID entityId) {
        if (pieceJointeDtos == null || pieceJointeDtos.isEmpty()) {
            return;
        }
        List<PieceJointe> toSave = pieceJointeDtos.stream()
                .peek(dto -> {
                    byte[] content = dto.getFichier();
                    String fileName = dto.getNom();
                    dto.setEntityId(entityId);
                    if (content != null && content.length > 0) {
                        String uniqueFileName = fileName;
                        File file = new File(pjDirectory + File.separator + uniqueFileName);
                        while (file.exists()) {
                            uniqueFileName = UUID.randomUUID() + "_" + fileName;
                            file = new File(pjDirectory + File.separator + uniqueFileName);
                        }
                        try {
                            log.info("Sauvegarde fichier : {} → {}", uniqueFileName, file.getAbsolutePath());
                            Files.write(file.toPath(), content,
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE,
                                    StandardOpenOption.TRUNCATE_EXISTING);
                            dto.setUrl(file.getAbsolutePath());
                        } catch (IOException e) {
                            log.error("Échec écriture fichier", e);
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la sauvegarde du fichier");
                        }
                    } else {
                        log.warn("Contenu vide ignoré pour : {}", fileName);
                    }
                })
                .map(pieceJointeMapper::toEntity)
                .collect(Collectors.toList());
        pieceJointeRepository.saveAll(toSave);
    }

    public List<PieceJointeDTO> getPjByEntityId(UUID entityId) {
        return pieceJointeRepository.findAllByEntityId(entityId)
                .stream()
                .map(pj -> {
                    PieceJointeDTO dto = pieceJointeMapper.toDto(pj);
                    dto.setFichier(loadFile(pj.getNom()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public byte[] getFileContentById(UUID id) {
        PieceJointe pj = pieceJointeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        return loadFile(pj.getNom());
    }

    public void deleteById(UUID id) {
        PieceJointe pj = pieceJointeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        deleteFile(pj.getNom());
        pieceJointeRepository.delete(pj);
    }

    public void deleteAllByEntityId(UUID entityId) {
        List<PieceJointe> pieces = pieceJointeRepository.findAllByEntityId(entityId);
        pieces.forEach(pj -> deleteFile(pj.getNom()));
        pieceJointeRepository.deleteAllByEntityId(entityId);
    }

    private byte[] loadFile(String fileName) {
        if (fileName == null) return new byte[0];
        File file = new File(pjDirectory + File.separator + fileName);
        try {
            if (file.exists() && file.isFile()) {
                return Files.readAllBytes(file.toPath());
            }
            log.debug("Fichier non trouvé : {}", fileName);
            return new byte[0];
        } catch (IOException e) {
            log.error("Erreur lecture fichier : {}", fileName, e);
            return new byte[0];
        }
    }

    private void deleteFile(String fileName) {
        if (fileName == null) return;
        File file = new File(pjDirectory + File.separator + fileName);
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
                log.info("Fichier supprimé : {}", fileName);
            }
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", fileName, e);
        }
    }
}