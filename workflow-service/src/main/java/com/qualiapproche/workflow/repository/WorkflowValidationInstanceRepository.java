package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.WorkflowValidationInstance;
import com.qualiapproche.workflow.model.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface WorkflowValidationInstanceRepository extends JpaRepository<WorkflowValidationInstance, UUID> {

    /** Faits déjà déclarés sur au moins un dossier, sous leur forme stockée. */
    @org.springframework.data.jpa.repository.Query(
            "select distinct i.faits from WorkflowValidationInstance i where i.faits is not null")
    java.util.List<String> faitsDeclares();

    Optional<WorkflowValidationInstance> findTopByResourceIdAndStatusOrderByStartedAtDesc(String resourceId, ValidationStatus status);
    Optional<WorkflowValidationInstance> findTopByResourceIdOrderByStartedAtDesc(String resourceId);
    boolean existsByEtatCodeInAndStatus(List<String> etatCodes, ValidationStatus status);

    /**
     * Instances de plusieurs ressources, la plus récente de chacune en tête.
     *
     * <p>Sert la consultation par lot : l'appelant retient la première ligne rencontrée pour
     * chaque ressource. Une requête unique remplace les N que provoquait
     * {@code findTopByResourceIdOrderByStartedAtDesc} appelée en boucle.</p>
     */
    List<WorkflowValidationInstance> findByResourceIdInOrderByStartedAtDesc(Collection<String> resourceIds);

    /**
     * Circuits en cours d'une famille de ressources.
     *
     * <p>Base du « ce que j'ai à décider » : seul un circuit en cours peut offrir une décision, et
     * le type de ressource borne la recherche au module qui la demande.</p>
     */
    List<WorkflowValidationInstance> findByResourceTypeAndStatus(String resourceType, ValidationStatus status);
}
