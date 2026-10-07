package com.qualiapproche.userservice.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.userservice.entities.AppRole;
import com.qualiapproche.userservice.repository.AppRoleRepository;
import com.qualiapproche.userservice.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ce que l'on apprend quand la suppression d'un rôle échoue.
 *
 * <p>Elle ne disait rien. Le contrôleur traduisait toute exception en « introuvable », et un rôle
 * parfaitement existant se déclarait absent dès qu'un utilisateur le portait encore : la
 * contrainte de la base refusait le retrait, la violation remontait en exception brute, et le
 * filet l'avalait. Rien n'indiquait qu'il suffisait de retirer le rôle à ses porteurs.</p>
 */
class SuppressionDUnRoleTest {

    private static final UUID ROLE = UUID.fromString("a11432a4-774b-441f-8863-091cf7bbd60c");

    private AppRoleRepository roles;
    private UserRoleAssignmentRepository rattachements;
    private KcRoleService service;

    @BeforeEach
    void preparer() {
        roles = mock(AppRoleRepository.class);
        rattachements = mock(UserRoleAssignmentRepository.class);
        service = new KcRoleService(null, null, null, roles, rattachements);
    }

    private AppRole role(String nom) {
        AppRole role = new AppRole();
        role.setId(ROLE);
        role.setName(nom);
        return role;
    }

    @Test
    @DisplayName("un rôle qui n'existe pas rend 404, et le dit")
    void roleAbsent_rend404() {
        when(roles.findById(ROLE)).thenReturn(Optional.empty());

        BusinessException refus = catchThrowableOfType(
                () -> service.deleteRoleById(ROLE), BusinessException.class);

        assertThat(refus.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(refus.getMessage()).contains(ROLE.toString());
    }

    @Test
    @DisplayName("un rôle encore porté rend 409, en nommant combien d'utilisateurs le portent")
    void rolePorte_rend409() {
        when(roles.findById(ROLE)).thenReturn(Optional.of(role("PILOTE")));
        when(rattachements.countByRole_Id(ROLE)).thenReturn(3L);

        BusinessException refus = catchThrowableOfType(
                () -> service.deleteRoleById(ROLE), BusinessException.class);

        // 409 et non 404 : le rôle existe, c'est sa suppression qui est impossible en l'état.
        assertThat(refus.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(refus.getMessage())
                .contains("PILOTE")
                .contains("3 utilisateurs")
                .contains("Retirez-le-leur");
        verify(roles, never()).delete(any());
    }

    @Test
    @DisplayName("un seul porteur se dit au singulier : le message se lit, il ne se décode pas")
    void unSeulPorteur_seDitAuSingulier() {
        when(roles.findById(ROLE)).thenReturn(Optional.of(role("AGENT")));
        when(rattachements.countByRole_Id(ROLE)).thenReturn(1L);

        BusinessException refus = catchThrowableOfType(
                () -> service.deleteRoleById(ROLE), BusinessException.class);

        assertThat(refus.getMessage()).contains("1 utilisateur.").doesNotContain("1 utilisateurs");
    }

    @Test
    @DisplayName("un rôle que personne ne porte se supprime")
    void roleSansPorteur_seSupprime() {
        AppRole role = role("Directeur General");
        when(roles.findById(ROLE)).thenReturn(Optional.of(role));
        when(rattachements.countByRole_Id(ROLE)).thenReturn(0L);

        service.deleteRoleById(ROLE);

        verify(roles).delete(role);
    }

    @Test
    @DisplayName("les porteurs sont comptés avant toute suppression, jamais après")
    void lesPorteurs_sontComptesAvant() {
        when(roles.findById(ROLE)).thenReturn(Optional.of(role("PILOTE")));
        when(rattachements.countByRole_Id(ROLE)).thenReturn(2L);

        catchThrowableOfType(() -> service.deleteRoleById(ROLE), BusinessException.class);

        // S'en remettre à la contrainte de la base laissait la violation remonter en exception
        // brute, que l'appelant ne savait pas distinguer d'une absence.
        var ordre = org.mockito.Mockito.inOrder(roles, rattachements);
        ordre.verify(roles).findById(ROLE);
        ordre.verify(rattachements).countByRole_Id(ROLE);
        ordre.verify(roles, never()).delete(any());
    }
}
