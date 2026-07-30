package com.qualiapproche.workflow.repository;

import com.qualiapproche.workflow.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
    List<Workflow> findByResourceType(String resourceType);
    List<Workflow> findByResourceTypeAndActifTrue(String resourceType);

    /**
     * Empreinte du catalogue : nombre de circuits et dernière date de modification connue.
     * Permet au moteur de détecter, sans tout recharger, qu'un autre pod a modifié la
     * configuration. Une requête légère, appelée à chaque consultation du catalogue.
     */
    @org.springframework.data.jpa.repository.Query(
            "select concat(count(w), '-', coalesce(max(w.updateAt), max(w.createdAt))) from Workflow w")
    String signatureCatalogue();
}
