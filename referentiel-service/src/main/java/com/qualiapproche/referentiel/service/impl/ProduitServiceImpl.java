package com.qualiapproche.referentiel.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.qualiapproche.common.dto.ProduitDto;
import com.qualiapproche.referentiel.entities.Produit;
import com.qualiapproche.referentiel.entities.mappers.ProduitMapper;
import com.qualiapproche.referentiel.repository.ProduitRepository;
import com.qualiapproche.referentiel.service.ProduitService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProduitServiceImpl implements ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitMapper produitMapper;

    @Override
    public ProduitDto create(ProduitDto produitDto) {
        Produit produit = produitMapper.toEntity(produitDto);
        return produitMapper.toDto(produitRepository.save(produit));
    }

    @Override
    public ProduitDto update(ProduitDto produitDto) {
        return produitRepository.findById(produitDto.getId()).map(produitExisted -> {
            produitMapper.updateEntityFromDto(produitDto, produitExisted);
            return produitMapper.toDto(produitRepository.save(produitExisted));
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK, "Aucun Produit trouvé."));
    }

    @Override
    public List<ProduitDto> allProduits() {
        return produitMapper.toDtos(produitRepository.findAll());
    }

    @Override
    public ProduitDto getProduitById(UUID id) {
        if (produitRepository.existsById(id)) {
            return produitMapper.toDto(produitRepository.getReferenceById(id));
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce produit n'existe pas.");
        }
    }

    @Override
    public void delete(UUID id) {
        if (produitRepository.existsById(id)) {
            produitRepository.deleteById(id);
        } else {
            throw new ResponseStatusException(HttpStatus.OK, "Ce produit n'existe pas.");
        }
    }
}
