package com.qualiapproche.gateway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * La signature que la passerelle pose sur l'en-tête {@code X-User-Permissions}.
 *
 * <p>Sans elle, un service joint directement — sans passer par la passerelle — croyait toute
 * permission qu'on lui annonçait : un jeton valide et {@code X-User-Permissions: SUPER_ADMIN}
 * suffisaient. La passerelle signe désormais les permissions qu'elle a résolues, avec un secret
 * qu'elle partage avec les services, et le jeton d'accès de la requête entre dans la signature :
 * une signature relevée sur une requête ne vaut pas avec un autre jeton.</p>
 *
 * <p><b>Le même calcul est écrit dans common</b> ({@code com.qualiapproche.common.config.SignatureDesPermissions},
 * qui le vérifie côté services ; la passerelle ne dépend pas de common). Les deux tests fixent le
 * même vecteur : si l'un change, l'autre casse.</p>
 *
 * <p>HMAC-SHA256, clé = secret, message = {@code jeton + "\n" + permissions}, rendu en base64url
 * sans remplissage.</p>
 */
public final class SignatureDesPermissions {

    /** L'en-tête qui porte la signature, à côté de {@code X-User-Permissions}. */
    public static final String EN_TETE = "X-User-Permissions-Signature";

    private SignatureDesPermissions() {
    }

    public static String signer(String secret, String jeton, String permissions) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal((jeton + "\n" + permissions).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponible", e);
        }
    }

    /** Vraie seulement si tout est présent et que la signature correspond, comparée en temps constant. */
    public static boolean verifier(String secret, String jeton, String permissions, String signature) {
        if (secret == null || secret.isBlank() || jeton == null || permissions == null || signature == null) {
            return false;
        }
        byte[] attendue = signer(secret, jeton, permissions).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(attendue, signature.getBytes(StandardCharsets.UTF_8));
    }
}
