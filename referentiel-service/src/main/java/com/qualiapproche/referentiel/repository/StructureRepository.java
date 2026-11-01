
package com.qualiapproche.referentiel.repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.common.enumeration.TypeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StructureRepository extends JpaRepository<Structure, UUID>,
        JpaSpecificationExecutor<Structure> {
    Optional<Structure> findByLibelleLong(String libelle);
    List<Structure> findByLibelleCourt(String libelle);

    boolean existsByLibelleLong(String libelle);

    boolean existsByLibelleLongAndIdNot(String nom, UUID id);

    List<Structure> findAllByTypeStructure(TypeStructure typeStructure);
    Page<Structure> findAllByTypeStructure(TypeStructure typeStructure, Pageable pageable);
    List<Structure> findAllByDirectionId(UUID id);
    Page<Structure> findAllByDirectionId(UUID id, Pageable pageable);
    List<Structure> findAllByDirectionIdAndTypeStructure(UUID id, TypeStructure typeStructure);
    Page<Structure> findAllByDirectionIdAndTypeStructure(UUID id, TypeStructure typeStructure, Pageable pageable);

    boolean existsByTypeStructure(TypeStructure typeStructure);

    /**
     * Fait d'une direction racine sa propre direction ({@code direction_id = id}).
     *
     * <p>Par une mise à jour directe : Hibernate tient la colonne de direction pour immuable, et
     * une direction ne peut pas être écrite sous elle-même avant d'exister.</p>
     */
    @Modifying
    @Query(value = "update structures set direction_id = id where id = :id and direction_id is distinct from id",
            nativeQuery = true)
    int faireSaPropreDirection(@Param("id") UUID id);

    /** Les directions racines : en contexte d'installation, les clients ; sous un client, lui seul. */
    List<Structure> findAllByTypeStructureAndDirectionIsNullOrderByLibelleLongAsc(TypeStructure typeStructure);
    boolean existsByTypeStructureAndIdNot(TypeStructure typeStructure, UUID id);


}
