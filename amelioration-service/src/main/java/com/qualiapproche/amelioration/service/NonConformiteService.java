package com.qualiapproche.amelioration.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

import com.qualiapproche.common.dto.NcCountsDto;
import com.qualiapproche.common.dto.NcDashboardDto;
import com.qualiapproche.common.dto.NcEvolutionDto;
import com.qualiapproche.common.dto.NcStats;
import com.qualiapproche.common.dto.NonConformiteDto;
import com.qualiapproche.common.enumeration.Etat;
import com.qualiapproche.common.enumeration.Status;
import com.qualiapproche.common.enumeration.TypeDemande;
import com.qualiapproche.common.enumeration.Circuit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NonConformiteService {
    NonConformiteDto createNonConformite(NonConformiteDto dto) throws IOException;

    /**
     * Soumet au pilote du processus une non-conformité enregistrée, sans attendre une seconde visite
     * de son auteur.
     *
     * @return le dossier tel qu'il est une fois l'étape franchie
     */
    NonConformiteDto soumettre(UUID id);

    void deleteMultiple(List<NonConformiteDto> nonConformiteDtos);

    List<NcStats> getNcStats(String structureSoumissionId);

    NonConformiteDto updateNonConformite(UUID id, NonConformiteDto dto) throws IOException;

    List<NonConformiteDto> updateNonConformites(List<NonConformiteDto> dtos) throws IOException;

    // NonConformiteDto create(NonConformiteDto nonConformiteDto);
    NonConformiteDto update(NonConformiteDto nonConformiteDto);

    Page<NonConformiteDto> allNonConformites(Pageable pageable);

    Page<NonConformiteDto> findImupted(String userId, Etat etat, Pageable pageable);

    // Méthode pour récupérer les non-conformités par état
    Page<NonConformiteDto> getNonConformitesByEtatNonConformite(Etat etat, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByEtatAnStructure(Etat etat, String uuid, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByStructure(String uuid, Pageable pageable);

    Page<NonConformiteDto> getNonConformitesByEtatAndStructureOrigine(Etat etat, String uuid, Pageable pageable);

    NonConformiteDto getNonConformiteById(UUID id);

    Page<NonConformiteDto> findAll(Status status, String structureSoumissionId, Pageable pageable);

    Page<NonConformiteDto> findAllByStructure(String structureSoumissionId, Pageable pageable);

    void delete(UUID id);

    Map<String, Long> getNonConformiteStatsByStructure(int anne);

    Map<String, Map<String, Long>> getStatsParAnnee(int annee);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesParAnnee(int annee);

    NonConformiteDto getByNumeroRef(String numeroRef);

    Map<String, Map<String, Long>> getStatsMensuellesParService(int annee, String origineServiceId);

    Map<String, Map<String, Map<String, Long>>> getStatsDetailleesServiceParAnnee(int annee, String origineServiceId);


    Map<String, Map<String, Map<String, Long>>> getStatsNiveauParAnnee(int annee, String origineServiceId);

    Page<NonConformiteDto> findAllByInitiator(String userId, Pageable pageable);

    Page<NonConformiteDto> findByUser(String userId, Pageable pageable);
    Page<NonConformiteDto> findImputedByUser(String userId, Pageable pageable);
    Page<NonConformiteDto> findArchivedByUser(String userId, Pageable pageable);
    NcCountsDto getCountsByUser(String userId);

    Page<NonConformiteDto> findByStructure(String structureId, Pageable pageable);
    Page<NonConformiteDto> findByStructureAllUsers(String structureId, Pageable pageable);

    NcDashboardDto getDashboardRQ();
    NcDashboardDto getDashboardPilot(String structureId);
    NcDashboardDto getDashboardUser(String userId);

    NcEvolutionDto getNcEvolutionStats(int annee, Integer mois, String structureId);

    Page<NonConformiteDto> getNonConformitesByNiveau(UUID niveauId, Pageable pageable);

    Page<NonConformiteDto> search(
            String numeroReference, String nomProcessus, String origineId, String origineService,
            String structureSoumissionId, String structureResponsableId,
            Etat etatTraitement, Status status, TypeDemande typeDemande, Circuit circuit,
            String userImputeEmail, String typeNonConformiteLibelle, String niveauNonConformiteLibelle,
            UUID typeNonConformiteId, UUID niveauNonConformiteId,
            LocalDateTime publicationDateFrom, LocalDateTime publicationDateTo,
            Pageable pageable);

    /**
     * Non-conformités sur lesquelles l'appelant a une décision à prendre.
     *
     * <p>La liste de traitement se composait de toutes les non-conformités portant un état donné :
     * chacun voyait celles des autres structures et ouvrait des dossiers sur lesquels le moteur
     * refusait ensuite toute action. Ce sont désormais les circuits qui désignent les dossiers, et
     * eux seuls — l'habilitation d'étape est une propriété du circuit, pas de l'état du dossier.</p>
     */
    Page<NonConformiteDto> aTraiterParLAppelant(Pageable pageable);

    /**
     * Reporte sur la non-conformité l'issue d'une transition franchie.
     *
     * @param issue       ce que le moteur a établi : {@code EN_COURS}, {@code APPROVED} ou
     *                    {@code REJECTED}. C'est lui qui fait foi.
     * @param nomEtape    libellé de l'étape atteinte, conservé pour l'affichage seulement
     * @param etatCode    état de traitement métier porté par l'étape ({@code VALIDATION_RS},
     *                    {@code CLOTURE}…), nul sur une fin de circuit
     * @param champs      valeurs saisies lors de la décision, indexées par nom de champ. Le moteur
     *                    ne transporte que des chaînes : un champ de type fichier y porte la
     *                    référence de l'objet déposé, pas son contenu.
     */
    void updateWorkflowState(UUID nonConformiteId, String issue, String nomEtape, String etatCode,
                             java.util.Map<String, String> champs);
}
