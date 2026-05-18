
package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.referentiel.entities.AbonnementDirection;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.entities.mappers.StructureMapper;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.StructureService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@AllArgsConstructor
@Slf4j
public class StructureServiceImpl implements StructureService {
    private final StructureRepository structureRepository;
    private final StructureMapper mapper;
    private final com.qualiapproche.referentiel.repository.AbonnementDirectionRepository abonnementDirectionRepository;

    @Override
    public StructureDto saveStructure(StructureDto structureDto) {
        Structure structure = mapper.toEntity(structureDto);
        structure.setTitreHonorifiqueSignataire(structureDto.getTitreHonorifiqueSignataire());

        if ((isNull(structure.getId()) && structureRepository.existsByLibelleLong(structure.getLibelleLong())
                || nonNull(structure.getId()) && structureRepository
                        .existsByLibelleLongAndIdNot(structure.getLibelleLong(), structureDto.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une structure avec le même nom exisite déjà.");
        }

        // Vérification de la contrainte : une seule DIRECTION
        if (structure.getTypeStructure() == TypeStructure.DIRECTION) {
            boolean directionExists = isNull(structure.getId())
                    ? structureRepository.existsByTypeStructure(TypeStructure.DIRECTION)
                    : structureRepository.existsByTypeStructureAndIdNot(TypeStructure.DIRECTION, structure.getId());

            if (directionExists) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une structure de type DIRECTION existe déjà.");
            }
        }

        if (isNull(structure.getId())) {
            // Initialisation de la licence uniquement pour DIRECTION
            if (structure.getTypeStructure() == TypeStructure.DIRECTION) {
                structure.setLicenceActive(true);
            } else {
                structure.setLicenceActive(false);
            }
        }

        structure = structureRepository.save(structure);

        return mapper.toDto(structure);
    }

    @Override
    public StructureDto updateStructure(StructureDto structureDto) {
        if (isNull(structureDto.getId()) || !structureRepository.existsById(structureDto.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Aucune structure avec cet ID : %s trouvée.", structureDto.getId()));
        }

        return saveStructure(structureDto);
    }

    @Override
    public StructureDto getStructureById(UUID directionId) {
        Structure structure = structureRepository.findById(directionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune structure trouvée avec l'id: {}" + directionId));

        return mapper.toDto(structure);
    }

    @Override
    public String getStructureNameById(UUID directionId) {
        Structure structure = structureRepository.findById(directionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune structure trouvée avec l'id: {}" + directionId));

        return structure.getLibelleCourt();
    }

    @Override
    public List<StructureDto> getAllStructures(TypeStructure typeStructure, UUID directionId) {
        List<Structure> structures;

        if (isNull(typeStructure) && isNull(directionId)) {
            structures = structureRepository.findAll();
        } else if (isNull(typeStructure)) {
            structures = structureRepository.findAllByDirectionId(directionId);
        } else if (isNull(directionId)) {
            structures = structureRepository.findAllByTypeStructure(typeStructure);
        } else {
            structures = structureRepository.findAllByDirectionIdAndTypeStructure(directionId, typeStructure);
        }

        return structures.stream().map(mapper::toDto).toList();
    }

    @Override
    public List<StructureDto> getAllStructuresAll() {
        return structureRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public void deleteStructure(UUID id) {
        if (!structureRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Aucune structure avec cet ID : %s trouvée.", id));
        }
        structureRepository.deleteById(id);
    }

    @Override
    public StructureDto findStructureByLibelle(String libelle) {
        List<Structure> structures = structureRepository.findByLibelleCourt(libelle);
        if (structures.size() > 1) {
            return structureRepository.findByLibelleLong(libelle).map(mapper::toDto)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Aucune structure trouvée avec le nom : " + libelle));
        } else if (structures.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucune structure trouvée avec le nom : " + libelle);
        }

        return mapper.toDto(structures.get(0));
    }

    @Override
    public StructureDto getDirection() {
        log.info("Récupération de la licence globale (Tenant License)...");

        // On récupère le premier abonnement trouvé en base (Licence unique de la
        // plateforme)
        List<AbonnementDirection> allAbos = abonnementDirectionRepository.findAll();

        if (allAbos.isEmpty()) {
            log.error("DEBUG LICENCE: Aucun abonnement trouvé dans la table 'abonnements_directions'.");
            // Tentative de renvoyer au moins la direction avec son statut par défaut
            List<Structure> directions = structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION);
            if (!directions.isEmpty()) {
                Structure targetDir = directions.get(0);
                StructureDto dto = mapper.toDto(targetDir);
                // On respecte le statut 'licenceActive' de l'entité Structure si aucun
                // abonnement spécifique n'existe
                dto.setLicenceActive(targetDir.getLicenceActive() != null && targetDir.getLicenceActive());
                dto.setModulesSubscribed(new java.util.ArrayList<>()); // Liste vide par défaut
                dto.setLicenseDaysRemaining(0L);
                return dto;
            }
            return null;
        }

        AbonnementDirection abo = allAbos.get(0);
        log.info("DEBUG LICENCE: Utilisation de l'abonnement ID: {}", abo.getId());

        // On cherche la direction liée ou on prend la première direction racine
        Structure targetDir = abo.getDirection();
        if (targetDir == null) {
            List<Structure> directions = structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION);
            if (!directions.isEmpty()) {
                targetDir = directions.get(0);
            }
        }

        if (targetDir == null) {
            log.error("DEBUG LICENCE: Aucune direction trouvée pour porter l'abonnement.");
            return null;
        }

        StructureDto dto = mapper.toDto(targetDir);
        dto.setDateDebutLicence(abo.getDateDebut());
        dto.setDateFinLicence(abo.getDateFin());

        if (abo.getDateFin() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDateTime.now(), abo.getDateFin());
            dto.setLicenseDaysRemaining(days);
            dto.setLicenceActive(days >= -7);
            log.info("DEBUG LICENCE: Jours restants: {}, Active: {}", days, dto.getLicenceActive());
        } else {
            dto.setLicenceActive(true); // Si pas de date de fin, on considère active par défaut pour le tenant
            dto.setLicenseDaysRemaining(999L);
        }

        // Décryptage des modules
        if (abo.getLicense() != null) {
            try {
                log.info("DEBUG LICENCE: Valeur cryptée en base: {}", abo.getLicense());
                String decrypted = com.qualiapproche.common.utils.CryptoUtils.decrypt(abo.getLicense());
                if (decrypted != null) {
                    log.info("DEBUG LICENCE: Modules décryptés: {}", decrypted);
                    dto.setModulesSubscribed(java.util.Arrays.asList(decrypted.split(",")));
                }
            } catch (Exception e) {
                log.error("DEBUG LICENCE: Échec décryptage: {}. Tentative lecture en clair...", e.getMessage());
                // Fallback si jamais c'est stocké en clair par erreur
                if (abo.getLicense().contains("NON_CONFORMITE")) {
                    dto.setModulesSubscribed(java.util.Arrays.asList(abo.getLicense().split(",")));
                }
            }
        }
        return dto;
    }

    @Override
    public java.util.Map<String, Object> getStructureLicenseStatus(UUID structureId) {
        log.info("Vérification du statut de licence pour la structure: {}", structureId);
        StructureDto direction = getDirection();
        java.util.Map<String, Object> status = new java.util.HashMap<>();

        if (direction != null) {
            status.put("licenseActive", direction.getLicenceActive());
            status.put("daysRemaining",
                    direction.getLicenseDaysRemaining() != null ? direction.getLicenseDaysRemaining().intValue() : 0);
            status.put("modules", direction.getModulesSubscribed());
        } else {
            status.put("licenseActive", false);
            status.put("daysRemaining", 0);
            status.put("modules", new java.util.ArrayList<>());
        }
        return status;
    }
}
