package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.ActionExisgence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionExigenceRepository extends JpaRepository<ActionExisgence, UUID> {
    List<ActionExisgence> findActionExisgenceByAction_Id(UUID id);
}
