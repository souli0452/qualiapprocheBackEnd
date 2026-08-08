package com.qualiapproche.referentiel.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une licence posée sur cette installation.
 *
 * <p>Le {@link #jeton} signé fait foi : les autres colonnes n'en sont qu'une copie, tenue pour
 * que les écrans et les requêtes n'aient pas à le déchiffrer à chaque appel. En cas de
 * divergence — une colonne modifiée à la main en base — c'est le jeton qui l'emporte : il est
 * revérifié au démarrage, et une licence retouchée est rejetée.</p>
 *
 * <p>Les licences successives sont <b>conservées</b>, jamais écrasées : la plus récemment
 * installée s'applique, et les précédentes disent l'historique de l'abonnement.</p>
 */
@Entity
@Table(name = "licences_installees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenceInstallee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Le jeton signé, tel que l'administrateur l'a collé.
     *
     * <p>Nul pour un essai local : personne ici ne peut signer, et c'est bien pourquoi l'essai
     * est borné à quelques jours.</p>
     */
    @Column(length = 4000)
    private String jeton;

    /** {@code COMMERCIALE} ou {@code ESSAI}. */
    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 40)
    private String reference;

    @Column(length = 40)
    private String partenaireCode;

    @Column(length = 200)
    private String partenaireNom;

    @Column(nullable = false)
    private LocalDate debut;

    @Column(nullable = false)
    private LocalDate fin;

    /** Modules ouverts, séparés par des virgules — une copie du jeton, pas une source. */
    @Column(length = 1000)
    private String modules;

    @Column(nullable = false)
    private int utilisateursMax;

    @Column(nullable = false)
    private LocalDateTime installeeLe;

    @Column(length = 120)
    private String installeePar;

    /**
     * Jour le plus avancé jamais observé par cette installation.
     *
     * <p>L'horloge appartient au client : reculer la date du serveur prolongerait indéfiniment une
     * licence expirée. Une horloge qui repart en arrière est donc détectée, et la licence tenue
     * pour expirée le temps que la date soit rétablie. Cela n'arrête pas un attaquant déterminé —
     * rien ne le ferait sur sa propre machine — mais cela arrête le contournement opportuniste.</p>
     */
    private LocalDate dernierJourVu;
}
