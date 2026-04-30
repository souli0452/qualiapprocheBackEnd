package com.qualiapproche.amelioration.repository;

import com.qualiapproche.amelioration.entities.ActionCorrectivePreventive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActionCorrectivePreventiveRepository extends JpaRepository<ActionCorrectivePreventive, UUID> {
}
