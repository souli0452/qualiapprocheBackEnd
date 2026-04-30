package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.ActionRisque;
import com.qualiapproche.referentiel.entities.Archivage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActionRisqueRepossitory  extends JpaRepository<ActionRisque, UUID> {
    List<ActionRisque> findActionRisqueByAction_Id(UUID id);
}
