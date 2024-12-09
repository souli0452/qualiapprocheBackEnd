package com.qualiapproche.service;

import java.util.List;
import java.util.UUID;

import com.qualiapproche.dto.NonConformiteDto;

public interface NonConformiteService {
    NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);
    List<NonConformiteDto> allNonConformites();
    NonConformiteDto getNonConformiteById(UUID id);

    void delete(UUID id);

}