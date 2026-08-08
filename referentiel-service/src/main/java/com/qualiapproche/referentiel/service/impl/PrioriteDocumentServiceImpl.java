package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.PrioriteDocumentDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.mappers.PrioriteDocumentMapper;
import com.qualiapproche.referentiel.repository.PrioriteDocumentRepository;
import com.qualiapproche.referentiel.service.PrioriteDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class PrioriteDocumentServiceImpl implements PrioriteDocumentService {

    private final PrioriteDocumentMapper mapper;
    private final PrioriteDocumentRepository repository;

    /** Rangé par ordre croissant, le libellé départageant les rangs identiques ou absents. */
    private static final Sort ORDRE = Sort.by(Sort.Order.asc("ordre").nullsLast(), Sort.Order.asc("libelle"));

    @Override
    public PrioriteDocumentDto create(PrioriteDocumentDto dto) {
        exigerUnLibelle(dto.getLibelle());
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    @Override
    public PrioriteDocumentDto update(PrioriteDocumentDto dto) {
        exigerUnLibelle(dto.getLibelle());
        return repository.findById(dto.getId())
                .map(existant -> {
                    mapper.updateEntityFromDto(dto, existant);
                    return mapper.toDto(repository.save(existant));
                })
                .orElseThrow(() -> new BusinessException(
                        "Aucun priorité ne porte cet identifiant : " + dto.getId(), HttpStatus.NOT_FOUND));
    }

    @Override
    public PrioriteDocumentDto getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new BusinessException(
                        "Aucun priorité ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<PrioriteDocumentDto> getAll(String recherche, Pageable pageable) {
        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), ORDRE);
        if (recherche == null || recherche.isBlank()) {
            return repository.findAll(range).map(mapper::toDto);
        }
        String terme = recherche.trim();
        return repository
                .findByLibelleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(terme, terme, range)
                .map(mapper::toDto);
    }

    @Override
    public List<PrioriteDocumentDto> getAll() {
        return mapper.toDtos(repository.findAll(ORDRE));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new BusinessException(
                    "Aucun priorité ne porte cet identifiant : " + id, HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }

    private void exigerUnLibelle(String libelle) {
        if (libelle == null || libelle.isBlank()) {
            throw new BusinessException("Le libellé est obligatoire.", HttpStatus.BAD_REQUEST);
        }
    }
}
