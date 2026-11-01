package com.qualiapproche.support.model;

import com.qualiapproche.common.cloisonnement.Cloisonnable;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "qms_document_user_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUserAccess implements Cloisonnable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private DocumentQms document;

    @Column(nullable = false)
    private String userId;

    private String userFullName;
    private String userEmail;

    @Column(nullable = false)
    private String role; // "READ_ONLY", "WRITE"

    /**
     * Le client à qui la ligne appartient, posé par Hibernate d'après la session
     * ({@code Cloisonnement}). Cloisonnée en propre et non par son document : elle est aussi
     * cherchée sans lui ({@code par utilisateur, pour « partagés avec moi »}).
     */
    @TenantId
    @Column(name = "direction_id")
    private UUID directionId;
}
