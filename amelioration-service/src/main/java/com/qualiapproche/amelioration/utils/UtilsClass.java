package com.qualiapproche.amelioration.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class UtilsClass {
        /*-------------------------------------------------------------------------------------------------/
    /*    Méthode de géneration du code unique pour la suivie des statut d'une demande                /
    /*----------------------------------------------------------------------------------------------*/


    public static String generateNumeroReferences(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return new BigInteger(1, bytes).toString(16);
    }




}
