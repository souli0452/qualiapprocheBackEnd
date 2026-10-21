package com.qualiapproche.userservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un jeton porté par un lien envoyé par courriel : vérifier une adresse, réinitialiser un mot de
 * passe. Seule son empreinte est conservée — une fuite de la table ne donne aucun lien utilisable.
 *
 * <p>N'hérite pas d'{@code AuditEntity} : il se consomme hors de toute session, sans jeton
 * d'accès dont tirer un auteur ou une direction.</p>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jeton_compte")
public class JetonDeCompte {

    /** Ce que le lien permet de faire. */
    public enum Usage { VERIFICATION_COURRIEL, REINITIALISATION }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Usage usage;

    @Column(nullable = false, length = 64, unique = true)
    private String empreinte;

    @Column(name = "expire_le", nullable = false)
    private LocalDateTime expireLe;

    @Column(name = "utilise_le")
    private LocalDateTime utiliseLe;

    @Column(name = "cree_le", nullable = false)
    private LocalDateTime creeLe;
}
