package com.qualiapproche.support.service;

import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.repository.QmsDocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class QmsDocumentTypeService {

    private final QmsDocumentTypeRepository typeRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDefaultTypes() {
        if (typeRepository.count() == 0) {
            log.info("Bootstrapping default QMS document types...");
            List<QmsDocumentType> defaultTypes = List.of(
                    QmsDocumentType.builder().code("PRO").libelle("Procédure").folderName("Procedures").build(),
                    QmsDocumentType.builder().code("INS").libelle("Instruction de travail").folderName("Instructions de travail").build(),
                    QmsDocumentType.builder().code("ENR").libelle("Enregistrement").folderName("Enregistrements").build(),
                    QmsDocumentType.builder().code("FOR").libelle("Formulaire").folderName("Formulaires").build(),
                    QmsDocumentType.builder().code("NOR").libelle("Norme").folderName("Normes").build(),
                    QmsDocumentType.builder().code("REG").libelle("Texte réglementaire").folderName("Textes reglementaires").build()
            );
            typeRepository.saveAll(defaultTypes);
        }
    }

    public List<QmsDocumentType> getAllTypes() {
        return typeRepository.findAll();
    }

    /**
     * Page des types documentaires, restreinte au terme cherché s'il y en a un.
     *
     * <p>Rangée par libellé à défaut de tri demandé : sans ordre stable, deux pages
     * successives peuvent ramener la même entrée et en taire une autre.</p>
     */
    public Page<QmsDocumentType> getTypes(
            String recherche, Pageable pageable) {
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(),
                        pageable.getPageSize(), Sort.by("libelle"));
        if (recherche == null || recherche.isBlank()) {
            return typeRepository.findAll(range);
        }
        String terme = recherche.trim();
        return typeRepository.findByCodeContainingIgnoreCaseOrLibelleContainingIgnoreCase(
                terme, terme, range);
    }

    public QmsDocumentType getTypeById(UUID id) {
        return typeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Type de document introuvable avec l'ID: " + id));
    }

    public QmsDocumentType getTypeByCode(String code) {
        return typeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Type de document introuvable avec le code: " + code));
    }

    @Transactional
    public QmsDocumentType createType(QmsDocumentType docType) {
        docType.setCode(docType.getCode().toUpperCase());
        if (typeRepository.findByCode(docType.getCode()).isPresent()) {
            throw new IllegalArgumentException("Un type de document avec le code " + docType.getCode() + " existe déjà.");
        }
        return typeRepository.save(docType);
    }

    @Transactional
    public QmsDocumentType updateType(UUID id, QmsDocumentType updated) {
        QmsDocumentType existing = getTypeById(id);
        existing.setLibelle(updated.getLibelle());
        existing.setFolderName(updated.getFolderName());
        return typeRepository.save(existing);
    }

    @Transactional
    public void deleteType(UUID id) {
        typeRepository.deleteById(id);
    }
}
