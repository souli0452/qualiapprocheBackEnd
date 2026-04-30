package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.referentiel.entities.ConfigGlobal;
import com.qualiapproche.referentiel.repository.ConfigGlobalRepository;
import com.qualiapproche.referentiel.service.configGlobalService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConfigGlobalServiceImpl implements configGlobalService {
    private  final ConfigGlobalRepository configGlobalRepository;
    @Override
    public ConfigGlobal getConfigGlobal() {
        List<ConfigGlobal> configs = configGlobalRepository.findAll();
        if (!configs.isEmpty()) {
            return configs.get(0);
        } else {
            // Gérer le cas où il n’y a pas de config globale :
            // - lever une exception explicite,
            // - ou retourner null,
            // - ou retourner une valeur par défaut
            throw new IllegalStateException("Aucune configuration globale trouvée !");
        }

    }

    @Override
    public ConfigGlobal createConfigGlobal(ConfigGlobal configGlobal) {
        return configGlobalRepository.save(configGlobal);
    }

    @Override
    public ConfigGlobal updateConfigGlobal(ConfigGlobal c,String id) {
        ConfigGlobal configGlobal = configGlobalRepository.getReferenceById(UUID.fromString(id));
        configGlobal.setEmailRq(c.getEmailRq());
        configGlobal.setRappelEcheance(c.getRappelEcheance());
        configGlobal.setNomCompletRq(c.getNomCompletRq());
        return configGlobalRepository.save(configGlobal);
    }
}
