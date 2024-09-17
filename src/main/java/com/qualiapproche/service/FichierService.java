package com.qualiapproche.service;

import com.qualiapproche.dto.FichierDto;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

public interface FichierService {
    @PostConstruct
    void init();

    void saveFichier(FichierDto fichierDto, UUID categorieId) throws IOException;

    void deleteFichier(UUID fichierId) throws IOException;

    Resource load(String filename);

    void deleteAll();

    Stream<Path> loadAll();

}
