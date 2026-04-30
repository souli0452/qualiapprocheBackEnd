package com.qualiapproche.common.utils;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * @author :  <A HREF="mailto:dieudonneouedra@gmail.com">Dieudonné OUEDRAOGO (Wendkouny)</A>
 * @version : 1.0
 * Copyright (c) 2024 SWITCH MAKER, All rights reserved.
 * @since : 2024/09/12 à 16:14
 */
public class CryptoUtils {
    private static final String SALTVALUE = "abcdefgigklmnopqrstuvwxyz";

    private static final byte[] BYTES = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private static final IvParameterSpec IVSPEC = new IvParameterSpec(BYTES);
    private static final String SECRET_KEY = "bXVzdGJlMTZieXRlc2tleQ";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String ALGO = "AES";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String decryptMessage = "Une erreur s'est produit lors du décodage";


    public static String encrypt(final String strToEncrypt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALTVALUE.getBytes(), ITERATION_COUNT, KEY_LENGTH);
            SecretKeySpec secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGO);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IVSPEC);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException
                 | InvalidKeySpecException | BadPaddingException
                 | IllegalBlockSizeException | NoSuchPaddingException e) {
            // // log.debug("Error occured during encryption: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Une erreur s'est produit lors de la génération de la licence");
        }
    }

    /**
     * Decryptage de licence.
     *
     * @param strToDecrypt String
     * @return String
     */
    public static String decrypt(final String strToDecrypt) {
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALTVALUE.getBytes(), ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp;
        try {
            tmp = factory.generateSecret(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), ALGO);
        Cipher cipher;

        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, decryptMessage);
        }

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IVSPEC);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, decryptMessage);
        }

        try {
            return new String(cipher.doFinal(Base64.getUrlDecoder().decode(strToDecrypt)), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, decryptMessage);
        }
    }
}
