package com.qualiapproche.common.messagerie;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un message déjà traité par ce service consommateur. Un courtier livre « au moins une fois » :
 * sans cette trace, un message relivré après un traitement réussi — l'acquittement perdu — aurait
 * envoyé deux fois le même courriel, ou rejoué la même transition.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractMessageTraite {

    @Id
    private UUID id;

    @Column(name = "recu_le", nullable = false)
    private LocalDateTime recuLe;
}
