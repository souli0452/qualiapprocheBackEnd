package com.qualiapproche.userservice.repository;

import com.qualiapproche.userservice.entities.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    List<UserRoleAssignment> findByUserId(String userId);
}
