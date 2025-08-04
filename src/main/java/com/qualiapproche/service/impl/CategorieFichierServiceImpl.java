package com.qualiapproche.service.impl;
import com.qualiapproche.dto.CategorieFichierDto;
import com.qualiapproche.entities.CategorieFichier;
import com.qualiapproche.entities.mappers.CategorieFichierMapper;
import com.qualiapproche.repository.CategorieFichierRepository;
import com.qualiapproche.repository.FichierRepository;
import com.qualiapproche.service.CategorieFichierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategorieFichierServiceImpl implements CategorieFichierService {

    private final CategorieFichierMapper categorieFichierMapper;
    private final CategorieFichierRepository categorieFichierRepository;
    private final FichierRepository fichierRepository;

    @Override
    public CategorieFichierDto create(CategorieFichierDto categorieFichierDto) {
        CategorieFichier categorieFichier = categorieFichierMapper.toEntity(categorieFichierDto);
        return categorieFichierMapper.toDto(categorieFichierRepository.save(categorieFichier));
    }

    @Override
    public CategorieFichierDto update(CategorieFichierDto categorieFichierDto) {
        return categorieFichierRepository.findById(categorieFichierDto.getId()).map(categorieFichierExisted -> {
            categorieFichierMapper.updateEntityFromDto(categorieFichierDto, categorieFichierExisted);
            return categorieFichierMapper.toDto(categorieFichierRepository.save(categorieFichierExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucune catégorie trouvée."));
    }

    @Override
    public List<CategorieFichierDto> allCategorieFichier() {
        return  categorieFichierMapper.toDtos(categorieFichierRepository.findAll()) ;
    }

    @Override
    public CategorieFichierDto getCategorieFichierById(UUID id) {
        if (categorieFichierRepository.existsById(id)) {
            return categorieFichierMapper.toDto(categorieFichierRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Cette catégorie n'existe pas.");

        }
    }

}
