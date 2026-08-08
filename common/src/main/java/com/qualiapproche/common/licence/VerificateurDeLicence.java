package com.qualiapproche.common.licence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Vérifie l'authenticité d'une licence QualiSira.
 *
 * <p>Une licence est une chaîne de trois parties séparées par des points :</p>
 * <pre>QSL1.&lt;contenu en base64url&gt;.&lt;signature en base64url&gt;</pre>
 *
 * <p>Cette application ne connaît que la <b>clé publique</b> de l'éditeur : elle permet de
 * vérifier une licence, jamais d'en signer une. C'est ce qui distingue ce dispositif du
 * chiffrement symétrique employé jusqu'ici, dont la clé unique — livrée avec le produit —
 * laissait fabriquer n'importe quelle licence à qui savait la lire.</p>
 *
 * <p>Ne juge <b>que</b> l'authenticité : ni les dates, ni les modules ne sont examinés ici. C'est
 * à l'appelant de le faire, pour qu'il puisse distinguer une licence contrefaite — qu'on refuse —
 * d'une licence authentique mais expirée, dont on peut dire exactement quand elle a pris fin.</p>
 *
 * <p>Recopie fidèle de la classe homonyme du service d'émission : le format est un contrat entre
 * les deux, et une divergence rendrait invérifiables des licences pourtant valides.</p>
 */
public final class VerificateurDeLicence {

    /** Version du format. Un changement de structure incrémentera ce préfixe. */
    public static final String PREFIXE = "QSL1";

    private static final String ALGORITHME = "Ed25519";

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

    private VerificateurDeLicence() {
    }

    /**
     * @param jeton             le texte collé par l'administrateur, espaces et retours à la ligne
     *                          tolérés — un copier-coller depuis un courriel en ajoute
     * @param clePubliqueBase64 la clé publique de l'éditeur, au format X.509
     * @throws LicenceIllisibleException si le format, la signature ou le contenu ne tiennent pas
     */
    public static ContenuDeLicence lire(String jeton, String clePubliqueBase64) {
        if (jeton == null || jeton.isBlank()) {
            throw new LicenceIllisibleException("Aucune licence n'a été fournie.");
        }
        if (clePubliqueBase64 == null || clePubliqueBase64.isBlank()) {
            throw new LicenceIllisibleException(
                    "Cette installation n'a pas de clé de vérification : elle ne peut authentifier "
                            + "aucune licence. Contactez l'éditeur.");
        }

        // Un copier-coller depuis un courriel ou un PDF ramène des espaces et des sauts de ligne.
        // Les refuser pour cela seul serait incompréhensible pour qui a collé la bonne licence.
        String[] parties = jeton.replaceAll("\\s", "").split("\\.");
        if (parties.length != 3 || !PREFIXE.equals(parties[0])) {
            throw new LicenceIllisibleException(
                    "Ce texte n'est pas une licence QualiSira. Vérifiez que la copie est complète.");
        }

        try {
            byte[] octets = Base64.getDecoder().decode(clePubliqueBase64.replaceAll("\\s", ""));
            PublicKey clePublique = KeyFactory.getInstance(ALGORITHME)
                    .generatePublic(new X509EncodedKeySpec(octets));

            Signature signature = Signature.getInstance(ALGORITHME);
            signature.initVerify(clePublique);
            signature.update(parties[1].getBytes(StandardCharsets.US_ASCII));
            if (!signature.verify(Base64.getUrlDecoder().decode(parties[2]))) {
                throw new LicenceIllisibleException(
                        "La signature de cette licence est invalide : elle n'a pas été émise par "
                                + "l'éditeur, ou son texte a été modifié.");
            }
        } catch (LicenceIllisibleException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenceIllisibleException(
                    "La signature de cette licence n'a pas pu être vérifiée.", e);
        }

        try {
            return JSON.readValue(Base64.getUrlDecoder().decode(parties[1]), ContenuDeLicence.class);
        } catch (Exception e) {
            throw new LicenceIllisibleException("Le contenu de cette licence est illisible.", e);
        }
    }
}
