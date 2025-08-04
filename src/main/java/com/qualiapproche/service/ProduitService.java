package com.qualiapproche.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.ProduitDto;

public interface ProduitService {

    ProduitDto create(ProduitDto produitDto);
    ProduitDto update(ProduitDto produitDto);
    List<ProduitDto> allProduits();
    ProduitDto getProduitById(UUID id);

    void delete(UUID id);
}
