package com.qualiapproche.amelioration.repository;
import com.qualiapproche.amelioration.entities.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ActionRepository extends JpaRepository<Action, UUID> {
}
