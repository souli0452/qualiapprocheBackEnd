package com.qualiapproche.repository;

import com.qualiapproche.entities.ActionExisgence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionExigenceRepository extends JpaRepository<ActionExisgence, UUID> {
    List<ActionExisgence> findActionExisgenceByAction_Id(UUID id);
}
