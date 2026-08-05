package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.enumeration.ModuleAbonnement;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.repository.AbonnementDirectionRepository;
import com.qualiapproche.referentiel.repository.StructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AbonnementService {

    private final AbonnementDirectionRepository abonnementDirectionRepository;
    private final StructureRepository structureRepository;

    public boolean hasAccess(UUID directionId, ModuleAbonnement module) {
        return structureRepository.findById(directionId)
                .flatMap(abonnementDirectionRepository::findByDirection)
                .map(abonnement -> {
                    if (!abonnement.isActive()) {
                        return false;
                    }
                    List<ModuleAbonnement> modules = decryptModules(abonnement.getLicense());
                    return modules.contains(module);
                })
                .orElse(false);
    }

    private List<ModuleAbonnement> decryptModules(String license) {
        if (license == null || license.isEmpty()) {
            return List.of();
        }
        try {
            String decrypted = CryptoUtils.decrypt(license);
            return Arrays.stream(decrypted.split(","))
                    .map(ModuleAbonnement::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
