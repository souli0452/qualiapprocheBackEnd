package com.qualiapproche.userservice.service;

import com.qualiapproche.common.config.PermissionChecker;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.utils.SecurityUtils;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.entities.UserRoleAssignment;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Nul ne donne ce qu'il n'a pas.
 *
 * <p>{@code MANAGE_USER} et {@code ROLE_MANAGE} disaient <i>qui</i> peut toucher aux comptes et
 * aux rôles, jamais <i>jusqu'où</i>. Un gestionnaire des utilisateurs pouvait donc s'attribuer le
 * rôle {@code SUPER_ADMIN}, ajouter à son propre rôle la permission d'installer une licence, ou
 * fixer le mot de passe d'un administrateur pour ouvrir sa session : chaque geste était autorisé,
 * leur somme était une prise de contrôle.</p>
 *
 * <p>Les règles tenues ici, à partir des permissions applicatives que le demandeur détient
 * <b>en base</b> (et non de l'en-tête relayé par la passerelle : c'est l'état de référence) :</p>
 * <ul>
 *   <li>attribuer ou retirer un rôle exige d'en détenir toutes les permissions ;</li>
 *   <li>mettre dans un rôle — ou en ôter — une permission exige de la détenir ;</li>
 *   <li>nul ne modifie les rôles de son propre compte : une autre personne doit le faire, ce qui
 *       laisse la trace de deux intervenants ;</li>
 *   <li>réinitialiser le mot de passe, changer le statut ou modifier la fiche d'un compte exige
 *       de détenir au moins tout ce qu'il détient — changer l'adresse d'un administrateur, puis
 *       demander un lien de réinitialisation, revenait à prendre son compte.</li>
 * </ul>
 *
 * <p>Le {@code SUPER_ADMIN} n'est pas borné : il détient tout par définition. Un appel de
 * <b>service</b> ne l'est pas non plus — c'est ainsi que referentiel-service crée l'administrateur
 * d'une installation neuve, avant que personne n'existe pour le faire.</p>
 */
@Component
@RequiredArgsConstructor
public class GardeDesHabilitations {

    /** Noms sous lesquels le rôle d'administration totale est reconnu, comme à l'ouverture de session. */
    private static final Set<String> NOMS_SUPER_ADMIN = Set.of("SUPER_ADMIN", "SUPERADMIN");

    private final UserRoleAssignmentRepository affectations;
    private final PermissionChecker perm;

    /**
     * Vérifie que le demandeur peut faire passer le compte {@code cibleUserId} de ses rôles actuels
     * à {@code rolesVoulus}.
     *
     * @param cibleUserId compte visé, ou {@code null} pour un compte en cours de création
     * @param rolesVoulus rôles que le compte portera à l'issue du geste
     * @throws BusinessException 403 si le geste dépasse les habilitations du demandeur
     */
    public void exigerAttributionPermise(String cibleUserId, Collection<AppRole> rolesVoulus) {
        if (perm.appelDeService()) {
            return;
        }
        String demandeur = demandeur();
        List<AppRole> actuels = cibleUserId == null ? List.of() : rolesDe(cibleUserId);
        List<AppRole> voulus = rolesVoulus == null ? List.of() : rolesVoulus.stream().filter(Objects::nonNull).toList();

        Set<String> nomsActuels = noms(actuels);
        Set<String> nomsVoulus = noms(voulus);
        if (nomsActuels.equals(nomsVoulus)) {
            // Rien ne change : enregistrer la fiche de quelqu'un, y compris la sienne, reste permis.
            return;
        }
        if (demandeur.equals(cibleUserId)) {
            throw refus("Vous ne pouvez pas modifier les rôles de votre propre compte : "
                    + "demandez-le à un autre administrateur.");
        }
        List<AppRole> mesRoles = rolesDe(demandeur);
        if (estSuperAdmin(mesRoles)) {
            return;
        }
        Set<String> mesPermissions = permissions(mesRoles);

        // Ce qui change, dans un sens ou dans l'autre : un rôle qu'on ne pourrait pas attribuer, on
        // ne peut pas non plus le retirer à celui qui le porte.
        List<AppRole> touches = new java.util.ArrayList<>();
        voulus.stream().filter(r -> !nomsActuels.contains(r.getName())).forEach(touches::add);
        actuels.stream().filter(r -> !nomsVoulus.contains(r.getName())).forEach(touches::add);

        for (AppRole role : touches) {
            if (estSuperAdmin(List.of(role))) {
                throw refus("Seul un SUPER_ADMIN peut attribuer ou retirer le rôle « " + role.getName() + " ».");
            }
            Set<String> manquantes = manquantes(role.getPermissions(), mesPermissions);
            if (!manquantes.isEmpty()) {
                throw refus("Le rôle « " + role.getName() + " » porte des permissions que vous ne détenez pas "
                        + manquantes + " : vous ne pouvez ni l'attribuer ni le retirer.");
            }
        }
    }

    /**
     * Vérifie que le demandeur détient au moins tout ce que détient le compte visé.
     *
     * @param cibleUserId compte visé
     * @param geste       ce que le demandeur s'apprête à faire, pour le message (« réinitialiser
     *                    son mot de passe »…)
     * @throws BusinessException 403 si le compte est plus privilégié que le demandeur
     */
    public void exigerAscendantSur(String cibleUserId, String geste) {
        if (perm.appelDeService()) {
            return;
        }
        String demandeur = demandeur();
        if (demandeur.equals(cibleUserId)) {
            return;
        }
        List<AppRole> mesRoles = rolesDe(demandeur);
        if (estSuperAdmin(mesRoles)) {
            return;
        }
        List<AppRole> sesRoles = rolesDe(cibleUserId);
        if (estSuperAdmin(sesRoles) || !manquantes(permissions(sesRoles), permissions(mesRoles)).isEmpty()) {
            throw refus("Ce compte détient des habilitations que vous n'avez pas : vous ne pouvez pas "
                    + geste + ". Adressez-vous à un administrateur qui les détient.");
        }
    }

    /**
     * Vérifie que le demandeur détient chaque permission qu'il ajoute à un rôle ou qu'il en retire.
     *
     * @param avant permissions du rôle avant le geste (vide pour une création)
     * @param apres permissions du rôle après le geste
     * @throws BusinessException 403 si une permission changée n'est pas détenue
     */
    public void exigerPermissionsDetenues(Collection<String> avant, Collection<String> apres) {
        if (perm.appelDeService()) {
            return;
        }
        Set<String> avantSet = avant == null ? Set.of() : new HashSet<>(avant);
        Set<String> apresSet = apres == null ? Set.of() : new HashSet<>(apres);
        Set<String> changees = new LinkedHashSet<>();
        apresSet.stream().filter(p -> !avantSet.contains(p)).forEach(changees::add);
        avantSet.stream().filter(p -> !apresSet.contains(p)).forEach(changees::add);
        if (changees.isEmpty()) {
            return;
        }
        List<AppRole> mesRoles = rolesDe(demandeur());
        if (estSuperAdmin(mesRoles)) {
            return;
        }
        Set<String> manquantes = manquantes(changees, permissions(mesRoles));
        if (!manquantes.isEmpty()) {
            throw refus("Vous ne pouvez ni ajouter à un rôle ni en retirer une permission que vous ne "
                    + "détenez pas " + manquantes + ".");
        }
    }

    /**
     * Réserve un geste au SUPER_ADMIN, reconnu à ses rôles applicatifs <b>en base</b>.
     *
     * <p>Sert aux écritures des rôles du realm Keycloak. Ces rôles finissent dans le jeton, et
     * plusieurs contrôles les croient sans passer par les rôles applicatifs : {@code PermissionChecker}
     * les compte comme des permissions, et le moteur de workflow ({@code WorkflowConditionAdapter})
     * accorde le droit de décider à une étape à qui porte un rôle du jeton du nom de l'habilitation
     * exigée ({@code RESPONSABLE_QUALITE}…). Réserver quelques noms ne suffisait donc pas : tout nom
     * de rôle applicatif devenait, côté Keycloak, une habilitation donnée sans détenir ses
     * permissions. Aucun écran ni aucun service n'écrit de rôle Keycloak — l'administration passe
     * par les rôles applicatifs — : seul le SUPER_ADMIN, qui détient tout, garde la main.</p>
     *
     * <p>Contrairement aux autres règles, un appel de service n'est pas exempté : aucun n'en a
     * l'usage, et ce serait rouvrir la porte qu'on ferme.</p>
     *
     * @param geste ce que le demandeur s'apprête à faire, pour le message
     * @throws BusinessException 403 si le demandeur n'est pas SUPER_ADMIN
     */
    public void exigerSuperAdmin(String geste) {
        if (!estSuperAdmin(rolesDe(demandeur()))) {
            throw refus("Seul un SUPER_ADMIN peut " + geste + " : les rôles Keycloak passent dans le jeton "
                    + "et ouvrent des droits sans contrôle. Attribuez plutôt un rôle applicatif.");
        }
    }

    /** L'auteur de la requête, d'après le jeton vérifié. Sans lui, rien ne peut être décidé. */
    private String demandeur() {
        String id = SecurityUtils.getCurrentUserId();
        if (id == null) {
            throw refus("Identité du demandeur inconnue : reconnectez-vous.");
        }
        return id;
    }

    private List<AppRole> rolesDe(String userId) {
        return affectations.findByUserId(userId).stream()
                .map(UserRoleAssignment::getRole)
                .filter(Objects::nonNull)
                .toList();
    }

    static boolean estSuperAdmin(Collection<AppRole> roles) {
        return roles.stream().map(AppRole::getName).filter(Objects::nonNull)
                .map(String::toUpperCase).anyMatch(NOMS_SUPER_ADMIN::contains);
    }

    private static Set<String> noms(Collection<AppRole> roles) {
        return roles.stream().map(AppRole::getName).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Set<String> permissions(Collection<AppRole> roles) {
        return roles.stream().map(AppRole::getPermissions).filter(Objects::nonNull)
                .flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /** Les permissions demandées que le demandeur ne détient pas, triées pour un message stable. */
    private static Set<String> manquantes(Collection<String> demandees, Set<String> detenues) {
        if (demandees == null) {
            return Set.of();
        }
        return demandees.stream().filter(Objects::nonNull).filter(p -> !detenues.contains(p))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static BusinessException refus(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }
}
