package com.qualiapproche.repository;

import com.qualiapproche.entities.ActionCorrectivePreventive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActionCorrectivePreventiveRepository extends JpaRepository<ActionCorrectivePreventive, UUID> {
}
