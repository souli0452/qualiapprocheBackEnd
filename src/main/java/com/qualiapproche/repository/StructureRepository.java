
package com.qualiapproche.repository;
import com.qualiapproche.entities.Structure;
import com.qualiapproche.enumeration.TypeStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StructureRepository extends JpaRepository<Structure, UUID> {
    Optional<Structure> findByLibelleLong(String libelle);
    List<Structure> findByLibelleCourt(String libelle);

    boolean existsByLibelleLong(String libelle);

    boolean existsByLibelleLongAndIdNot(String nom, UUID id);

    List<Structure> findAllByTypeStructure(TypeStructure typeStructure);
    List<Structure> findAllByDirectionId(UUID id);
    List<Structure> findAllByDirectionIdAndTypeStructure(UUID id, TypeStructure typeStructure);


}
