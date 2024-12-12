package com.qualiapproche.service.kcService;

import com.qualiapproche.dto.PieceJointeDto;
import com.qualiapproche.entities.PieceJointe;
import com.qualiapproche.entities.mappers.PieceJointeMapper;
import com.qualiapproche.repository.PieceJointeRepository;
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
import java.util.ArrayList;
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

    public void savePj(List<PieceJointeDto> pieceJointeDtos, final String pjDirectory, Long entityId) {
        if (pieceJointeDtos != null) {
            List<PieceJointe> pieceJointes = pieceJointeDtos.stream()
                    .peek(pj -> {
                        byte[] content = pj.getFichier();
                        String fileName = pj.getNom();
                        pj.setEntityId(entityId);

                        if (content != null && content.length > 0) {
                            String uniqueFileName = fileName;
                            File file = new File(pjDirectory + File.separator + uniqueFileName);

                            // Vérification de l'existence du fichier et génération d'un nom unique si nécessaire
                            while (file.exists()) {
                                uniqueFileName = UUID.randomUUID() + "_" + fileName;
                                file = new File(pjDirectory + File.separator + uniqueFileName);
                            }

                            try {
                                log.info("Saving File :  {} in : {}", uniqueFileName, file.getAbsolutePath());
                                Files.write(file.toPath(), content,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.WRITE,
                                        StandardOpenOption.TRUNCATE_EXISTING);
                                pj.setUrl(file.getAbsolutePath());
                            } catch (IOException e) {
                                log.error("Erreur d'enregistrement du fichier", e);
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erreur d'enregistrement du fichier");
                            }
                        } else {
                            log.warn("Le fichier vide ne peut pas être sauvegardé : {}", fileName);
                        }
                    }).map(pieceJointeMapper::toEntity).collect(Collectors.toList());

            pieceJointeMapper.toDto(pieceJointeRepository.saveAll(pieceJointes));
        }

    }


    public List<PieceJointeDto> loadPjByEntity(final Long entityId, final String pjDirectory) {
        List<PieceJointe> pjList = pieceJointeRepository.findAllByEntityId(entityId);

        return pjList.stream().map(pj -> {
            PieceJointeDto pieceJointeDto = pieceJointeMapper.toDto(pj);
            pieceJointeDto.setFichier(loadFile(pj.getNom(), pjDirectory));

            return pieceJointeDto;
        }).toList();
    }

    public void removePjByEntity(final Long entityId, final String pjDirectory) {
        List<PieceJointe> pjList = pieceJointeRepository.findAllByEntityId(entityId);

        if (!pjList.isEmpty()) {
            pjList.forEach(pj -> {
                deleteFile(pj.getNom(), pjDirectory);
                pieceJointeRepository.delete(pj);
            });
        }
    }


    public void removePj(final Long pieceJointeId, final String pjDirectory) {
        pieceJointeRepository.findById(pieceJointeId)
                .ifPresentOrElse(pj -> {
                    deleteFile(pj.getNom(), pjDirectory);
                    pieceJointeRepository.delete(pj);
                }, () -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de trouver la pièce jointe");
                });
    }

    private void deleteFile(final String fileName, final String pjDirectory) {
        File file = new File(pjDirectory + File.separator + fileName);
        try {
            if (file.exists()) {
                Files.delete(file.toPath());
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public byte[] loadFile(final String fileName, final String pjDirectory) {
        File file = new File(pjDirectory + File.separator + fileName);
        try {
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            } else {
                log.debug("--== Fichier inexistant : {} ==--", fileName);
                return new byte[]{};
            }
        } catch (IOException e) {
            log.error("--== Erreur de chargement du fichier : " + fileName, e);
            return new byte[]{};
        }
    }

    public PieceJointeDto loadFileByPjId(Long id, String pubDirectory) {
        PieceJointe pieceJointe = pieceJointeRepository.getReferenceById(id);
        PieceJointeDto dto = pieceJointeMapper.toDto(pieceJointe);
        dto.setFichier(loadFile(pieceJointe.getNom(), pubDirectory));

        return dto;
    }

    public List<PieceJointeDto> loadFileByEntityId(Long entityId, String pubDirectory) {
        List<PieceJointe> pieceJointes = pieceJointeRepository.findAll().stream().filter(pj -> !pj.isZipFile() && pj.getEntityId().compareTo(entityId) == 0).toList();
        List<PieceJointeDto> pieceJointeDtos = new ArrayList<>();
        pieceJointes.forEach(pj -> {
            PieceJointeDto pjDto = pieceJointeMapper.toDto(pj);
            pjDto.setFichier(loadFile(pj.getNom(), pubDirectory));
            pieceJointeDtos.add(pjDto);
        });
        return pieceJointeDtos;
    }


    public List<PieceJointeDto> getPjByEntity(final Long entityId, final String pjDirectory) {
        List<PieceJointe> pjList = pieceJointeRepository.findAllByEntityId(entityId);

        return pjList.stream().map(pj -> {
            PieceJointeDto pieceJointeDto = pieceJointeMapper.toDto(pj);
            pieceJointeDto.setFichier(loadFile(pj.getNom(), pjDirectory));

            return pieceJointeDto;
        }).toList();
    }


      public byte[] loadFileByPjId(Long id){
      PieceJointe pieceJointe = pieceJointeRepository.getReferenceById(id);
      return loadFile(pieceJointe.getNom(),this.pjDirectory);
      }
}
