package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Le code du partenaire chez qui cette installation tourne — le repère auquel une licence est
 * confrontée.
 *
 * <p>C'est lui qui rend une licence <b>exclusive</b> à son destinataire. La signature prouve
 * qu'une licence n'a pas été fabriquée ; elle ne dit rien de l'installation qui la présente. Sans
 * repère local, le code inscrit dans la licence n'est qu'une étiquette d'affichage, et la licence
 * d'un client s'installe chez un autre.</p>
 *
 * <p>Le repère ne peut pas venir de la licence — ce serait demander à la pièce à vérifier de
 * fournir la référence de sa propre vérification. Il vient donc d'ici, dans cet ordre :</p>
 * <ol>
 *   <li>la propriété {@code qualisira.licence.partenaire-code}, posée au déploiement ;</li>
 *   <li>à défaut, la colonne {@code code_partenaire} de la direction, obfusquée — que le
 *       provisionnement y pose depuis {@code tenant-init.json}, où elle s'écrit en clair : un
 *       fichier de livraison doit se relire.</li>
 * </ol>
 *
 * <p>Ce code ne sort par aucune API : il est absent des DTO, et {@code Structure} le marque
 * {@code @JsonIgnore}. L'exposer en consultation ou en liste apprendrait à qui sait lire une
 * réponse JSON la valeur exacte qu'il lui faudrait recopier.</p>
 *
 * <p>La propriété l'emporte : elle se change sans reconstruire le produit, ce qu'un changement de
 * raison sociale ou une reprise de dossier finit toujours par exiger.</p>
 *
 * <h2>Ce que l'obfuscation vaut, et ce qu'elle ne vaut pas</h2>
 *
 * <p>{@link CryptoUtils} chiffre avec une clé, un sel et un vecteur d'initialisation <b>écrits en
 * dur dans {@code common}</b>, donc livrés au client. Quiconque détient le produit peut déchiffrer
 * cette valeur et en rechiffrer une autre. Ce n'est pas une protection : c'est ce qui décourage
 * l'édition désinvolte d'un fichier, rien de plus.</p>
 *
 * <p>Ce qui protège réellement est ailleurs, et tient déjà : la licence est signée par une clé
 * privée qui ne quitte jamais l'outil d'émission, et sa durée est courte. Le javadoc de
 * {@code TenantProvisioningService} rappelle pourquoi ce fichier a cessé de porter la licence
 * elle-même — chiffrée par cette même clé, elle laissait s'accorder tous les modules jusqu'en 2099
 * en éditant un JSON. Le code partenaire, lui, ne s'accorde aucun droit : il ne fait que désigner
 * à qui la licence doit correspondre.</p>
 */
@Component
@Slf4j
public class CodeDeLInstallation {

    private final StructureRepository structures;

    @Value("${qualisira.licence.partenaire-code:}")
    private String duDeploiement;

    /**
     * Résolu à la première demande, non au démarrage.
     *
     * <p>Le provisionnement qui pose le code est un {@code CommandLineRunner} : il s'exécute une
     * fois tous les objets construits. Résoudre en {@code @PostConstruct} lirait donc une base
     * encore vide au tout premier démarrage, et l'installation se croirait sans partenaire
     * jusqu'au redémarrage suivant.</p>
     *
     * <p>Retenu <b>dès qu'il vaut quelque chose</b>, et pas avant : il est posé une fois pour
     * toutes, et le relire à chaque appel ferait une requête par vérification de licence. Mais
     * retenir une absence l'aurait figée pour la vie du processus — une installation qui pose son
     * code après coup ne l'aurait vu qu'au redémarrage suivant, et les structures créées entre
     * les deux n'en auraient jamais hérité. Tant qu'il manque, on redemande : c'est le cas
     * dégradé, et il se répare de lui-même.</p>
     */
    private String attendu;

    public CodeDeLInstallation(StructureRepository structures) {
        this.structures = structures;
    }

    /** Le code attendu, ou une chaîne vide si l'installation n'en déclare aucun. */
    public String attendu() {
        if (attendu == null || attendu.isBlank()) {
            attendu = resoudre();
        }
        return attendu;
    }

    private String resoudre() {
        if (duDeploiement != null && !duDeploiement.isBlank()) {
            log.info("Installation déclarée pour le partenaire « {} » (propriété de déploiement) : "
                    + "seules ses licences seront acceptées.", duDeploiement.trim());
            return duDeploiement.trim();
        }

        String enBase = depuisLaBase();
        if (enBase.isBlank()) {
            log.warn("Aucun code partenaire n'est déclaré — ni par "
                    + "qualisira.licence.partenaire-code, ni sur la direction en base. Toute "
                    + "licence authentique sera acceptée, y compris celle d'un autre client. "
                    + "À renseigner avant toute livraison.");
        } else {
            log.info("Installation déclarée pour le partenaire « {} » : seules ses licences seront "
                    + "acceptées.", enBase);
        }
        return enBase;
    }

    /**
     * La licence présentée est-elle bien celle de cette installation ?
     *
     * <p>Comparaison insensible à la casse et aux espaces : le code voyage par courriel et par
     * fichier de configuration, où il se recopie à la main.</p>
     *
     * <p>Une licence sans code partenaire est refusée dès lors qu'un code est attendu : c'est le
     * cas d'une licence d'un format antérieur, qui ne peut pas prouver son destinataire.</p>
     *
     * <p>Sans code déclaré, tout passe — le comportement d'avant, conservé pour ne pas mettre à
     * l'arrêt les installations déjà livrées. Le démarrage le signale : un contrôle qui n'opère
     * pas doit se dire, jamais se supposer.</p>
     */
    public boolean reconnait(String codeDeLaLicence) {
        String repere = attendu();
        if (repere.isBlank()) {
            return true;
        }
        return codeDeLaLicence != null && codeDeLaLicence.trim().equalsIgnoreCase(repere);
    }

    private String depuisLaBase() {
        try {
            return structures.findAllByTypeStructure(TypeStructure.DIRECTION).stream()
                    .map(Structure::getCodePartenaire)
                    .filter(code -> code != null && !code.isBlank())
                    .findFirst()
                    .map(CryptoUtils::decrypt)
                    .map(String::trim)
                    .orElse("");
        } catch (Exception e) {
            // Une valeur illisible ne doit pas empêcher le service de servir : elle vaut absence
            // de code, et le contrôle ne s'applique pas — ce que le journal dit assez fort pour
            // qu'on ne le découvre pas le jour où une licence étrangère est acceptée.
            log.error("Le code partenaire en base est illisible ({}) : aucun contrôle du "
                    + "destinataire ne sera exercé. Reposez-le, ou renseignez "
                    + "qualisira.licence.partenaire-code.", e.getMessage());
            return "";
        }
    }
}
