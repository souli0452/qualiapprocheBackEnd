package com.qualiapproche.service.impl;

import com.qualiapproche.entities.ConfigGlobal;
import com.qualiapproche.repository.ConfigGlobalRepository;
import com.qualiapproche.service.configGlobalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public ConfigGlobal updateConfigGlobal(ConfigGlobal c,UUID id) {
        log.info("hh",id);
        ConfigGlobal configGlobal = configGlobalRepository.findById(id).get();
        configGlobal.setEmailRq(c.getEmailRq());
        configGlobal.setRappelEcheance(c.getRappelEcheance());
        configGlobal.setNomCompletRq(c.getNomCompletRq());
        return configGlobalRepository.save(configGlobal);
    }
}
