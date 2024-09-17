package com.qualiapproche.service.impl;

import com.qualiapproche.dto.FournisseurDto;
import com.qualiapproche.entities.Fournisseur;
import com.qualiapproche.entities.mappers.FounisseurMapper;
import com.qualiapproche.repository.FournisseurRepository;
import com.qualiapproche.service.FournisseurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class FournisseurServiceImpl implements FournisseurService {
    private final FournisseurRepository fournisseurRepository;
    private final FounisseurMapper founisseurMapper;

    @Override
    public FournisseurDto create(FournisseurDto fournisseurDto) {
        Fournisseur fournisseur = founisseurMapper.toEntity(fournisseurDto);
        return founisseurMapper.toDto(fournisseurRepository.save(fournisseur));
    }

    @Override
    public FournisseurDto update(FournisseurDto fournisseurDto) {
        return fournisseurRepository.findById(fournisseurDto.getId()).map(fournisseurExisted -> {
            founisseurMapper.updateEntityFromDto(fournisseurDto, fournisseurExisted);
            return founisseurMapper.toDto(fournisseurRepository.save(fournisseurExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun fournisseur trouvé."));
    }

    @Override
    public List<FournisseurDto> allFournisseurs() {
        return  founisseurMapper.toDtos(fournisseurRepository.findAll()) ;
    }

    @Override
    public FournisseurDto getFounisseurById(UUID id) {
        if (fournisseurRepository.existsById(id)) {
            return founisseurMapper.toDto(fournisseurRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce fournisseur n'existe pas.");

        }
    }
}
