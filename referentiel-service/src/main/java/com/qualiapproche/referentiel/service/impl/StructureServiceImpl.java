
package com.qualiapproche.referentiel.service.impl;

import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.dto.StructureDto;
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
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.service.CodeDeLInstallation;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@Service
@AllArgsConstructor
@Slf4j
public class StructureServiceImpl implements StructureService {
    private final StructureRepository structureRepository;
    private final StructureMapper mapper;
    private final LicenceInstalleeService licenceInstalleeService;
    private final CodeDeLInstallation installation;

    @Override
    public StructureDto saveStructure(StructureDto structureDto) {
        Structure structure = mapper.toEntity(structureDto);
        structure.setTitreHonorifiqueSignataire(structureDto.getTitreHonorifiqueSignataire());
        reporterLeCodePartenaire(structure);

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

    /**
     * Donne à la structure le code partenaire de l'installation, que l'écran ne connaît pas.
     *
     * <p>Le code ne figure dans aucun DTO : il n'entre pas et ne ressort pas, ni en consultation
     * ni en liste. Il est posé ici, où qu'une structure soit créée ou modifiée, parce qu'il
     * appartient à l'installation et non à qui remplit le formulaire.</p>
     *
     * <p><b>À la création</b>, la structure hérite du code de l'installation : toutes les
     * structures d'une même installation relèvent du même partenaire, et une seule qui en
     * manquerait suffirait à créer un angle mort.</p>
     *
     * <p><b>À la modification</b>, le code est relu en base et reporté. C'est indispensable :
     * l'enregistrement reconstruit l'entité depuis le DTO, et une colonne absente du DTO serait
     * écrite à nul. Modifier le libellé d'une direction depuis l'écran effacerait le code, et le
     * contrôle du destinataire s'éteindrait sans que rien ne le dise — jusqu'au jour où la licence
     * d'un autre client serait acceptée.</p>
     *
     * <p>Dans les deux cas, un enregistrement ne peut pas <i>changer</i> le code : il vient de
     * l'installation, et rien de ce que l'on saisit ne l'atteint.</p>
     */
    private void reporterLeCodePartenaire(Structure structure) {
        if (nonNull(structure.getId())) {
            structureRepository.findById(structure.getId())
                    .ifPresent(existante -> structure.setCodePartenaire(existante.getCodePartenaire()));
            return;
        }
        String code = installation.attendu();
        structure.setCodePartenaire(code.isBlank() ? null : CryptoUtils.encrypt(code));
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
    public Page<StructureDto> getAllStructures(TypeStructure typeStructure, UUID directionId,
                                               String recherche, Pageable pageable) {
        // Une spécification composée plutôt qu'un arbre de si : chaque critère facultatif
        // doublait le nombre de branches, et la recherche libre l'aurait encore doublé.
        Specification<Structure> criteres =
                (racine, requete, cb) -> cb.conjunction();

        if (!isNull(typeStructure)) {
            criteres = criteres.and((racine, requete, cb) ->
                    cb.equal(racine.get("typeStructure"), typeStructure));
        }
        if (!isNull(directionId)) {
            criteres = criteres.and((racine, requete, cb) ->
                    cb.equal(racine.get("direction").get("id"), directionId));
        }
        if (recherche != null && !recherche.isBlank()) {
            String motif = "%" + recherche.trim().toLowerCase() + "%";
            criteres = criteres.and((racine, requete, cb) -> cb.or(
                    cb.like(cb.lower(racine.get("libelleLong")), motif),
                    cb.like(cb.lower(racine.get("libelleCourt")), motif)));
        }

        Pageable range = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(),
                        pageable.getPageSize(), Sort.by("libelleLong"));
        return structureRepository.findAll(criteres, range).map(mapper::toDto);
    }

    @Override
    public Page<StructureDto> getAllStructuresAll(Pageable pageable) {
        return structureRepository.findAll(pageable).map(mapper::toDto);
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

    /**
     * La direction, accompagnée de l'état de la licence installée.
     *
     * <p>Les champs de licence proviennent de {@link LicenceInstalleeService}, seule source de
     * vérité : c'est la même que celle dont la passerelle se sert pour autoriser ou refuser une
     * écriture. Ils étaient auparavant lus dans {@code abonnements_directions}, alimentée au
     * démarrage par un fichier du produit — les écrans annonçaient donc des modules ouverts et
     * une licence valide pendant que la passerelle répondait 402.</p>
     *
     * <p>Sans licence posée, {@code licenceActive} est faux et la liste des modules vide : la
     * consultation reste ouverte, les actions non. L'écran dédié propose alors l'essai gratuit.</p>
     */
    @Override
    public StructureDto getDirection() {
        List<Structure> directions = structureRepository.findAllByTypeStructure(TypeStructure.DIRECTION);
        if (directions.isEmpty()) {
            log.error("Aucune structure de type DIRECTION n'existe.");
            return null;
        }

        StructureDto dto = mapper.toDto(directions.get(0));
        EtatLicenceDto licence = licenceInstalleeService.etat();

        dto.setLicenceActive(licence.isActionsOuvertes());
        dto.setModulesSubscribed(licence.getModules());
        dto.setDateDebutLicence(licence.getDebut() != null ? licence.getDebut().atStartOfDay() : null);
        dto.setDateFinLicence(licence.getFin() != null ? licence.getFin().atStartOfDay() : null);
        dto.setLicenseDaysRemaining("ABSENTE".equals(licence.getStatut()) ? 0L : licence.getJoursRestants());

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
