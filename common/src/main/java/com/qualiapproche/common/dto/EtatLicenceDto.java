package com.qualiapproche.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Où en est la licence de cette installation — ce que l'écran d'accueil a besoin de savoir.
 *
 * <p>Trois situations, et trois conduites différentes :</p>
 * <ul>
 *   <li>{@code ABSENTE} — rien n'a jamais été installé : on demande une licence, ou on propose
 *       l'essai ;</li>
 *   <li>{@code ACTIVE} — tout est ouvert ; un compte à rebours s'affiche à l'approche du
 *       terme ;</li>
 *   <li>{@code EXPIREE} — les données restent consultables, les actions sont suspendues. Couper
 *       l'accès aux données qualité d'un client transformerait un retard de paiement en litige,
 *       et le pousserait à chercher comment contourner.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtatLicenceDto {

    /** {@code ABSENTE}, {@code ACTIVE} ou {@code EXPIREE}. */
    private String statut;

    /**
     * Les actions d'écriture sont-elles permises ?
     *
     * <p>Faux hors licence valide. La consultation, elle, ne se ferme jamais.</p>
     */
    private boolean actionsOuvertes;

    /** {@code COMMERCIALE} ou {@code ESSAI}. */
    private String type;

    private String reference;
    private String partenaireNom;

    private LocalDate debut;
    private LocalDate fin;

    /** Négatif une fois le terme passé : « expirée depuis 12 jours » se lit directement. */
    private long joursRestants;

    private List<String> modules;

    /** {@code 0} vaut « sans limite ». */
    private int utilisateursMax;

    /**
     * L'essai gratuit est-il encore proposable ?
     *
     * <p>Une seule fois par installation : sans cette limite, il suffirait d'en redemander un à
     * chaque échéance, et l'essai remplacerait l'abonnement.</p>
     */
    private boolean essaiDisponible;

    /** Phrase à afficher telle quelle — c'est elle qui dit quoi faire. */
    private String message;
}
