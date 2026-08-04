package com.qualiapproche.support.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Partage d'un document avec une structure entière, par opposition au partage nominatif que porte
 * {@link DocumentUserAccess}.
 *
 * <p>Un document n'est visible que dans sa structure d'émission. Pour qu'une autre structure y ait
 * accès, il faut un geste explicite de la structure émettrice — c'est cet enregistrement. La cible
 * est choisie au moment du partage, à l'étape où l'on juge utile de montrer le document : partager
 * revient à l'ouvrir à tous les membres de cette structure, sans avoir à les nommer un à un.</p>
 *
 * <p>L'accès ainsi obtenu est en lecture seule, et volontairement plus étroit qu'une simple
 * lecture : le destinataire consulte et télécharge, sans voir l'historique des versions, la piste
 * d'audit ni les décisions du circuit — ce sont les affaires internes de la structure émettrice.</p>
 */
@Entity
@Table(name = "qms_document_structure_access",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_structure", columnNames = {"document_id", "structure_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentStructureAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private DocumentQms document;

    /** Structure destinataire, telle que désignée à la soumission du document. */
    @Column(name = "structure_id", nullable = false)
    private String structureId;

    private String structureLibelle;

    /**
     * Étape du circuit à laquelle le partage a été consenti.
     *
     * <p>Le destinataire se choisit au moment du geste, pas à la soumission : savoir à quel moment
     * du circuit un document a franchi sa structure éclaire autant la relecture que de savoir avec
     * qui. Nul si le document ne suit aucun circuit.</p>
     */
    private String etapeCode;

    private String sharedByUserId;
    private String sharedByFullName;

    @Builder.Default
    private LocalDateTime sharedAt = LocalDateTime.now();
}