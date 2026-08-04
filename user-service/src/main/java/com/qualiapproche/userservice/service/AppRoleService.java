package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Création et modification des rôles applicatifs, sous la règle qui fait du <b>nom</b> du rôle son
 * identité fonctionnelle.
 *
 * <p>Ce n'est pas un choix pris ici : c'est déjà le nom que désignent les étapes des circuits
 * ({@code WorkflowStep.responsableRole}), les entrées du catalogue d'étapes, la resynchronisation
 * des rôles standard au démarrage ({@code RoleInitializer.syncRole}), le calcul des permissions
 * ({@code KcUserService.computePermissions}) et la résolution des destinataires d'une notification
 * ({@code getUsersByRole}). Tous reposent sur l'égalité exacte de cette chaîne, et aucun ne connaît
 * l'identifiant technique du rôle.</p>
 *
 * <p>D'où les deux règles appliquées ici, sans lesquelles le nom ne peut pas tenir ce rôle :</p>
 *
 * <ul>
 *   <li><b>Le nom ne se modifie pas.</b> Le point d'entrée était un {@code save()} sur l'entité
 *       reçue : renommer un rôle depuis l'écran d'administration était donc possible, et se payait
 *       en cascade — plus aucun titulaire habilité à décider sur les étapes qui le désignaient,
 *       plus aucun destinataire à notifier, et pour un rôle standard, sa recréation sous l'ancien
 *       nom au démarrage suivant, laissant deux rôles là où l'administrateur croyait n'en avoir
 *       qu'un. Rien de tout cela n'était signalé : le renommage réussissait.</li>
 *   <li><b>Deux rôles ne peuvent pas porter le même nom.</b> Un homonyme rendrait ambiguë chacune
 *       de ces résolutions, et ferait lever {@code findByName}.</li>
 * </ul>
 *
 * <p>Le libellé reste donc figé une fois publié, comme le code d'une étape ou d'une entrée de
 * catalogue. La description, elle, se modifie librement : c'est là que se corrige la formulation
 * présentée à l'écran d'attribution.</p>
 */
@Service
@RequiredArgsConstructor
public class AppRoleService {

    private final AppRoleRepository appRoleRepository;

    @Transactional
    public AppRole enregistrer(AppRole role) {
        String nom = role.getName() == null ? null : role.getName().trim();
        if (nom == null || nom.isBlank()) {
            throw new BusinessException("Le nom du rôle est obligatoire.", HttpStatus.BAD_REQUEST);
        }
        role.setName(nom);

        return role.getId() == null ? creer(role) : modifier(role, nom);
    }

    private AppRole creer(AppRole role) {
        if (!appRoleRepository.findAllByName(role.getName()).isEmpty()) {
            throw new BusinessException(
                    "Un rôle nommé « " + role.getName() + " » existe déjà. Les étapes des circuits "
                            + "désignent leur responsable par ce nom : deux rôles homonymes rendraient "
                            + "indécidable qui est habilité.",
                    HttpStatus.CONFLICT);
        }
        return appRoleRepository.save(role);
    }

    private AppRole modifier(AppRole role, String nom) {
        AppRole existant = appRoleRepository.findById(role.getId())
                .orElseThrow(() -> new BusinessException(
                        "Rôle introuvable : " + role.getId(), HttpStatus.NOT_FOUND));

        if (!nom.equals(existant.getName())) {
            throw new BusinessException(
                    "Le nom du rôle ne peut pas être modifié (" + existant.getName() + " → " + nom
                            + "). Les étapes des circuits, le catalogue d'étapes et les notifications "
                            + "désignent ce rôle par son nom : le changer les priverait toutes de leur "
                            + "titulaire. Créez un nouveau rôle si nécessaire, ou modifiez la "
                            + "description.",
                    HttpStatus.CONFLICT);
        }

        existant.setDescription(role.getDescription());
        List<String> permissions = role.getPermissions();
        if (permissions != null) {
            existant.setPermissions(permissions);
        }
        return appRoleRepository.save(existant);
    }
}
