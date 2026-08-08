package com.qualiapproche.common.licence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Ce que la licence installée affirme : à qui, pour quoi, jusqu'à quand.
 *
 * <p>Charge utile signée par l'éditeur. Les noms de propriétés sont courts parce qu'ils voyagent
 * dans une chaîne que l'administrateur copie-colle.</p>
 *
 * <p>Le contenu est <b>lisible</b> — la licence est signée, non chiffrée : le client doit pouvoir
 * vérifier ce qu'il a acheté. Ce qu'il ne peut pas, c'est en fabriquer une autre.</p>
 *
 * <p>{@link JsonIgnoreProperties} tolère les propriétés inconnues : une licence émise par une
 * version plus récente de l'outil reste lisible plutôt que de faire échouer la vérification.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContenuDeLicence(

        @JsonProperty("ref") String reference,
        @JsonProperty("cli") String partenaireCode,
        @JsonProperty("nom") String partenaireNom,
        @JsonProperty("deb") LocalDate debut,
        @JsonProperty("fin") LocalDate fin,
        @JsonProperty("mod") List<String> modules,
        // Nombre d'utilisateurs actifs autorisés ; 0 vaut « sans limite ».
        @JsonProperty("usr") int utilisateursMax,
        // COMMERCIALE ou ESSAI.
        @JsonProperty("typ") String type,
        @JsonProperty("edt") String edition
) {

    public boolean estUnEssai() {
        return "ESSAI".equalsIgnoreCase(type);
    }

    public boolean sansLimiteDUtilisateurs() {
        return utilisateursMax <= 0;
    }

    /**
     * La licence couvre-t-elle ce jour ?
     *
     * <p>Distinct de la vérification de signature : une licence expirée est <b>authentique</b>, et
     * l'écran doit pouvoir dire « votre abonnement a pris fin le … » plutôt que « licence
     * invalide », qui laisserait croire à une erreur de saisie.</p>
     */
    public boolean couvre(LocalDate jour) {
        return debut != null && fin != null && !jour.isBefore(debut) && !jour.isAfter(fin);
    }

    public boolean ouvre(String module) {
        return modules != null && modules.stream().anyMatch(m -> m.equalsIgnoreCase(module));
    }

    /** Négatif une fois le terme passé : « expirée depuis 12 jours » se lit directement. */
    public long joursRestants() {
        return fin == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), fin);
    }
}
