package com.qualiapproche.userservice.entities;

import com.qualiapproche.common.base.AuditEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
