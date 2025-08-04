package com.qualiapproche.utils;

import com.qualiapproche.repository.NonConformiteRepository;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;

public class UtilsClass {
private NonConformiteRepository nonConformiteRepository;
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
