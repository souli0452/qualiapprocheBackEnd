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
     * Tous les circuits avec leurs étapes, pour le chargement du catalogue du moteur.
     *
     * <p>Le graphe évite une lecture différée par circuit. Seules les étapes sont ramenées ici :
     * y adjoindre leurs transitions ferait joindre deux collections dans la même requête, ce
     * qu'Hibernate refuse. L'adaptateur les charge donc en une seconde requête, et le catalogue
     * complet tient en deux allers-retours au lieu de croître avec le nombre d'étapes.</p>
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "steps")
    @org.springframework.data.jpa.repository.Query("select distinct w from Workflow w")
    List<Workflow> findAllAvecEtapes();

    /**
     * Empreinte du catalogue : nombre de circuits et dernière date de modification connue.
     * Permet au moteur de détecter, sans tout recharger, qu'un autre pod a modifié la
     * configuration. Une requête légère, appelée à chaque consultation du catalogue.
     */
    @org.springframework.data.jpa.repository.Query(
            "select concat(count(w), '-', coalesce(max(w.updateAt), max(w.createdAt))) from Workflow w")
    String signatureCatalogue();
}
