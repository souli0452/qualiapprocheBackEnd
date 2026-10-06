package com.qualiapproche.amelioration.service.impl;

import com.qualiapproche.amelioration.client.ReferentielClient;
import com.qualiapproche.amelioration.entities.NonConformite;
import com.qualiapproche.amelioration.entities.mappers.NonConformiteMapper;
import com.qualiapproche.amelioration.repository.NiveauNonConformiteRepository;
import com.qualiapproche.amelioration.repository.NonConformiteRepository;
import com.qualiapproche.amelioration.repository.TypeNonConformiteRepository;
import com.qualiapproche.amelioration.service.NonConformiteService;
import com.qualiapproche.common.dto.NonConformiteDAuditDto;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.dto.StructureDto;
import com.qualiapproche.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Les non-conformités qui naissent d'un audit (M13).
 *
 * <p>Ce sont des non-conformités ordinaires, déclarées par l'auditeur qui publie l'écart — c'est
 * sous son jeton que l'appel arrive, et c'est lui l'auteur du dossier. Deux choses seulement les
 * distinguent :</p>
 * <ul>
 *   <li>le circuit s'ouvre dans la <b>structure auditée</b>, et non dans celle de l'auditeur :
 *       c'est son pilote qui analyse les causes ;</li>
 *   <li>la transmission est <b>idempotente</b> : rejouée pour le même écart, elle rend le dossier
 *       déjà ouvert au lieu d'en créer un second.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonConformitesDAuditService {

    private final NonConformiteRepository nonConformites;
    private final NonConformiteService service;
    private final NonConformiteMapper mapper;
    private final TypeNonConformiteRepository types;
    private final NiveauNonConformiteRepository niveaux;
    private final ReferentielClient referentiel;

    /** Le dossier déjà ouvert pour cet écart, s'il existe. */
    public Optional<NonConformiteDto> dejaOuverte(String cleOrigine) {
        return nonConformites.findByCleOrigine(cleOrigine).map(mapper::toDto);
    }

    /**
     * Ouvre le dossier, sans le soumettre.
     *
     * <p>La soumission est un second geste, joué par l'appelant une fois cette création committée :
     * le moteur revient vers ce service pendant la soumission, et ne trouverait pas un dossier que
     * la transaction n'aurait pas encore rendu visible.</p>
     */
    public NonConformiteDto ouvrir(NonConformiteDAuditDto ecart) {
        UUID structureId = uuid(ecart.getStructureConcerneeId());
        StructureDto structure = structure(structureId);

        NonConformiteDto dto = new NonConformiteDto();
        dto.setJustification(ecart.getJustification().strip());
        dto.setOriginNonConformiteLibelle(ecart.getOrigineLibelle());
        dto.setNomProcessus(ecart.getNomProcessus());
        // Le dossier appartient à la structure auditée : c'est elle qui le porte et qui le numérote.
        dto.setStructureSoumissionId(structureId.toString());
        // Jamais nul : la numérotation s'en sert de sigle à défaut du libellé court, et échouait sur un nul.
        dto.setStructureSoumissionLibelle(structure == null || structure.getLibelleLong() == null ? "AUDIT" : structure.getLibelleLong());
        if (structure != null) {
            dto.setTypeProcessusId(structure.getTypeProcessusId());
            dto.setTypeProcessusLibelle(structure.getTypeProcessusLibelle());
        }
        if (ecart.getTypeNonConformiteId() != null) {
            types.findById(ecart.getTypeNonConformiteId()).ifPresent(t -> {
                dto.setTypeNonConformiteId(t.getId());
                dto.setTypeNonConformiteLibelle(t.getLibelle());
            });
        }
        if (ecart.getNiveauNonConformiteId() != null) {
            niveaux.findById(ecart.getNiveauNonConformiteId()).ifPresent(n -> {
                dto.setNiveauNonConformiteId(n.getId());
                dto.setNiveauNonConformiteLibelle(n.getLibelle());
            });
        }

        try {
            NonConformiteDto creee = service.createNonConformite(dto, structureId.toString());
            NonConformite nc = nonConformites.findById(creee.getId()).orElseThrow();
            nc.setCleOrigine(ecart.getCleOrigine());
            nonConformites.save(nc);
            log.info("Non-conformité {} ouverte depuis l'écart d'audit {}.", creee.getNumeroReference(),
                    ecart.getCleOrigine());
            return mapper.toDto(nc);
        } catch (IOException e) {
            throw new BusinessException("La non-conformité issue de l'audit n'a pas pu être enregistrée.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public NonConformiteDto soumettre(UUID id) {
        return service.soumettre(id);
    }

    /**
     * La structure auditée, pour son libellé et sa famille de processus. Son absence n'empêche
     * pas la déclaration : un écart ne doit pas se perdre parce que le référentiel n'a pas répondu.
     */
    private StructureDto structure(UUID id) {
        try {
            return referentiel.getStructureById(id);
        } catch (Exception e) {
            log.warn("Structure {} introuvable pour la non-conformité d'audit : {}", id, e.getMessage());
            return null;
        }
    }

    private static UUID uuid(String valeur) {
        try {
            return UUID.fromString(valeur.strip());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("La structure auditée n'est pas un identifiant valide : " + valeur,
                    HttpStatus.BAD_REQUEST);
        }
    }
}
