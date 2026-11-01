package com.qualiapproche.referentiel.plateforme;

import com.qualiapproche.common.cloisonnement.AmorceDesDirections;
import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.cloisonnement.ExecutionCloisonnee;
import com.qualiapproche.common.cloisonnement.RattrapageDesDirections;
import com.qualiapproche.common.config.CloisonnementInterneController.DemandeDeRattrapage;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.common.dto.EtatLicenceDto;
import com.qualiapproche.common.enumeration.TypeStructure;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.CryptoUtils;
import com.qualiapproche.referentiel.client.UserClient;
import com.qualiapproche.referentiel.client.cloisonnement.ComptesDesClients;
import com.qualiapproche.referentiel.client.cloisonnement.ServiceCloisonne;
import com.qualiapproche.referentiel.entities.Structure;
import com.qualiapproche.referentiel.repository.StructureRepository;
import com.qualiapproche.referentiel.service.CodeDeLInstallation;
import com.qualiapproche.referentiel.service.LicenceInstalleeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les clients de l'installation : chacun est une direction racine, avec ses comptes, son
 * paramétrage et sa licence.
 *
 * <p>Créer un client, c'est :</p>
 * <ol>
 *   <li>poser sa direction — hors de tout client, puisqu'elle n'existe pas encore — et lui donner
 *       sa propre identité pour direction ;</li>
 *   <li>amorcer son paramétrage dans chaque service (rôles, circuits, modèles de courriel, types
 *       de documents, réglages…) : sans les rôles, son administrateur ne pourrait pas être créé ;</li>
 *   <li>créer son super administrateur, rattaché à sa direction.</li>
 * </ol>
 *
 * <p>Un service injoignable ne fait pas échouer la création : le rapport le dit, et l'amorce se
 * rejoue sans dommage ({@link #amorcer(UUID)}), comme elle se rejoue au démarrage de chaque
 * service pour les clients qu'il connaît.</p>
 */
@Slf4j
@Service
public class GestionDesClients {

    /** Ce que l'administrateur de la plateforme saisit pour un nouveau client. */
    public record NouveauClient(String libelleLong, String libelleCourt, String email, String code,
                                KcUserDto superAdmin) {
    }

    /** Un client, vu de la plateforme : son identité et l'état de sa licence, rien de ses données. */
    public record Client(UUID id, String libelleLong, String libelleCourt, String email,
                         String statutLicence, Long joursRestants, List<String> modules) {
    }

    private final StructureRepository structures;
    private final ExecutionCloisonnee execution;
    private final UserClient comptes;
    private final ComptesDesClients comptesDesClients;
    private final List<ServiceCloisonne> services;
    private final AmorceDesDirections amorceLocale;
    private final RattrapageDesDirections rattrapageLocal;
    private final LicenceInstalleeService licences;
    private final CodeDeLInstallation codes;

    public GestionDesClients(StructureRepository structures, ExecutionCloisonnee execution, UserClient comptes,
                             ComptesDesClients comptesDesClients, List<ServiceCloisonne> services,
                             AmorceDesDirections amorceLocale, RattrapageDesDirections rattrapageLocal,
                             LicenceInstalleeService licences, CodeDeLInstallation codes) {
        this.structures = structures;
        this.execution = execution;
        this.comptes = comptes;
        this.comptesDesClients = comptesDesClients;
        this.services = services;
        this.amorceLocale = amorceLocale;
        this.rattrapageLocal = rattrapageLocal;
        this.licences = licences;
        this.codes = codes;
    }

    /** Les directions racines de l'installation. */
    public List<Structure> directions() {
        return execution.enInstallation(() ->
                structures.findAllByTypeStructureAndDirectionIsNullOrderByLibelleLongAsc(TypeStructure.DIRECTION));
    }

    public List<Client> clients() {
        return directions().stream().map(this::vue).toList();
    }

    private Client vue(Structure direction) {
        EtatLicenceDto licence = execution.pour(direction.getId(), licences::etat);
        return new Client(direction.getId(), direction.getLibelleLong(), direction.getLibelleCourt(),
                direction.getEmail(), licence.getStatut(), licence.getJoursRestants(), licence.getModules());
    }

    /**
     * Crée un client de bout en bout.
     *
     * @return l'identifiant de sa direction et, par étape, ce qui a échoué
     */
    public Map<String, Object> creer(NouveauClient demande) {
        exigerUnClientComplet(demande);
        UUID direction = creerLaDirection(demande.libelleLong().trim(),
                demande.libelleCourt() == null || demande.libelleCourt().isBlank()
                        ? demande.libelleLong().trim() : demande.libelleCourt().trim(),
                demande.email().trim(), demande.code());
        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("direction", direction);
        rapport.put("amorce", amorcer(direction));
        rapport.put("superAdmin", creerLeSuperAdmin(direction, demande.superAdmin()));
        log.info("Client « {} » créé (direction {}).", demande.libelleLong(), direction);
        return rapport;
    }

    /**
     * Pose une direction racine et en fait sa propre direction.
     *
     * <p>Hors de tout client : la ligne ne peut pas être écrite sous une direction qui n'existe pas
     * encore. Elle reçoit d'abord celle de l'installation, puis la sienne, par une mise à jour
     * directe — Hibernate tient la colonne pour immuable.</p>
     *
     * @return l'identifiant de la direction, existante ou créée
     */
    public UUID creerLaDirection(String libelleLong, String libelleCourt, String email, String code) {
        return execution.enInstallation(() -> {
            Structure existante = structures
                    .findAllByTypeStructureAndDirectionIsNullOrderByLibelleLongAsc(TypeStructure.DIRECTION).stream()
                    .filter(s -> libelleLong.equalsIgnoreCase(s.getLibelleLong()))
                    .findFirst().orElse(null);
            if (existante != null) {
                return existante.getId();
            }
            Structure direction = structures.save(Structure.builder()
                    .libelleLong(libelleLong)
                    .libelleCourt(libelleCourt)
                    .email(email)
                    .typeStructure(TypeStructure.DIRECTION)
                    .licenceActive(false)
                    .codePartenaire(code == null || code.isBlank() ? null : CryptoUtils.encrypt(code.trim()))
                    .build());
            structures.flush();
            structures.faireSaPropreDirection(direction.getId());
            codes.oublier(direction.getId());
            return direction.getId();
        });
    }

    /** Joue l'amorce du client dans chaque service, celui-ci compris ; rend les échecs par service. */
    public Map<String, Object> amorcer(UUID direction) {
        exigerUnClient(direction);
        Map<String, Object> echecs = new LinkedHashMap<>();
        List<String> locaux = amorceLocale.amorcer(direction);
        if (!locaux.isEmpty()) {
            echecs.put("referentiel-service", locaux);
        }
        for (ServiceCloisonne service : services) {
            String nom = nomDe(service);
            try {
                Map<String, Object> reponse = service.amorcer(direction);
                Object leurs = reponse == null ? null : reponse.get("echecs");
                if (leurs instanceof List<?> liste && !liste.isEmpty()) {
                    echecs.put(nom, liste);
                }
            } catch (Exception e) {
                log.warn("Amorce de {} pour la direction {} impossible : {}", nom, direction, e.getMessage());
                echecs.put(nom, "injoignable : " + e.getMessage());
            }
        }
        return echecs;
    }

    /**
     * Crée le super administrateur d'un client, sous sa direction.
     *
     * <p>Après l'amorce de user-service : sans le rôle SUPER_ADMIN du client, le compte serait
     * créé sans rôle.</p>
     *
     * @return « créé », « existait déjà », « aucun » ou « échec : … »
     */
    public Object creerLeSuperAdmin(UUID direction, KcUserDto superAdmin) {
        if (superAdmin == null || superAdmin.getUsername() == null || superAdmin.getUsername().isBlank()) {
            return "aucun";
        }
        superAdmin.setStructure(direction.toString());
        superAdmin.setRoles(List.of("SUPER_ADMIN"));
        superAdmin.setEnabled(true);
        try {
            // Sous la direction du client : l'en-tête X-Direction-Id part avec l'appel, et
            // user-service rattache le compte à ce client.
            Cloisonnement.pour(direction, () -> comptes.createUser(superAdmin));
            return "créé";
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                return "existait déjà";
            }
            log.error("Super administrateur du client {} non créé : {}", direction, e.getMessage());
            return "échec : " + e.getMessage();
        }
    }

    /**
     * Rend à chaque client les lignes écrites avant le cloisonnement, dans chaque service.
     *
     * <p>Chaque structure est rapportée à sa direction racine ; s'il n'y a qu'un client, tout ce
     * qui n'a pas de direction lui revient.</p>
     */
    public Map<String, Object> rattraper() {
        Map<UUID, UUID> correspondances = execution.enInstallation(() -> {
            Map<UUID, UUID> lues = new HashMap<>();
            structures.findAll().forEach(s -> {
                if (Cloisonnement.estUnClient(s.getDirectionId())) {
                    lues.put(s.getId(), s.getDirectionId());
                }
            });
            return lues;
        });
        List<Structure> clients = directions();
        UUID parDefaut = clients.size() == 1 ? clients.get(0).getId() : null;
        DemandeDeRattrapage demande = new DemandeDeRattrapage(correspondances, parDefaut);

        Map<String, Object> rapport = new LinkedHashMap<>();
        rapport.put("referentiel-service", rattrapageLocal.rattraper(correspondances, parDefaut));
        for (ServiceCloisonne service : services) {
            String nom = nomDe(service);
            try {
                rapport.put(nom, service.rattraper(demande));
            } catch (Exception e) {
                rapport.put(nom, "injoignable : " + e.getMessage());
            }
        }
        try {
            rapport.put("comptes", comptesDesClients.rattraper(demande));
        } catch (Exception e) {
            rapport.put("comptes", "injoignable : " + e.getMessage());
        }
        return rapport;
    }

    /** Crée un administrateur de la plateforme, s'il n'existe pas ; vrai si le compte est là. */
    public boolean creerUnAdministrateurDePlateforme(KcUserDto compte) {
        try {
            comptesDesClients.creerAdministrateurDePlateforme(compte);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                return true;
            }
            log.warn("Administrateur de la plateforme {} non créé : {}", compte.getUsername(), e.getMessage());
            return false;
        }
    }

    /** Vrai si un service n'a pas répondu : le rapport porte alors « injoignable ». */
    public static boolean incomplet(Map<String, Object> rapport) {
        return rapport.values().stream().anyMatch(v -> v instanceof String texte && texte.startsWith("injoignable"));
    }

    private void exigerUnClient(UUID direction) {
        boolean connu = directions().stream().anyMatch(d -> d.getId().equals(direction));
        if (!connu) {
            throw new BusinessException("Aucun client ne porte cet identifiant : " + direction, HttpStatus.NOT_FOUND);
        }
    }

    private static void exigerUnClientComplet(NouveauClient demande) {
        if (demande == null || demande.libelleLong() == null || demande.libelleLong().isBlank()) {
            throw new BusinessException("Le nom du client est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        if (demande.email() == null || demande.email().isBlank()) {
            throw new BusinessException("Le courriel du client est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        KcUserDto admin = demande.superAdmin();
        if (admin == null || admin.getUsername() == null || admin.getUsername().isBlank()
                || admin.getEmail() == null || admin.getEmail().isBlank()) {
            throw new BusinessException("Le super administrateur du client (identifiant et courriel) est "
                    + "obligatoire : sans lui, personne ne pourrait y entrer.", HttpStatus.BAD_REQUEST);
        }
    }

    /** Le nom du service visé, lu sur l'interface Feign (« CloisonnementSupport » → support-service). */
    private static String nomDe(ServiceCloisonne service) {
        for (Class<?> interfaceFeign : service.getClass().getInterfaces()) {
            if (ServiceCloisonne.class.isAssignableFrom(interfaceFeign) && interfaceFeign != ServiceCloisonne.class) {
                return interfaceFeign.getSimpleName().replace("Cloisonnement", "").toLowerCase() + "-service";
            }
        }
        return service.getClass().getSimpleName();
    }
}
