package com.qualiapproche.userservice.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_roles")
public class AppRole extends AuditEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "app_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission_value")
    private List<String> permissions = new ArrayList<>();
}
