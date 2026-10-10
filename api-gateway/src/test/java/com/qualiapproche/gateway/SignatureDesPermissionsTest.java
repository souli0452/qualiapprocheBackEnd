package com.qualiapproche.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** La passerelle signe comme les services vérifient : même vecteur que common. */
class SignatureDesPermissionsTest {

    @Test
    @DisplayName("Même vecteur que la vérification côté services (common, SignatureDesPermissionsTest)")
    void meme_vecteur_que_les_services() {
        assertThat(SignatureDesPermissions.signer("secret-de-test", "jeton-abc", "AUDIT-READ,MANAGE_USER"))
                .isEqualTo("JGDYHwZnUP-lRmgZ5UmDUyMIY7aGBvIJxc88fqELsXw");
    }
}
