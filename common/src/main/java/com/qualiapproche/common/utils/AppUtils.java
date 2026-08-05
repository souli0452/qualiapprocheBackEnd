package com.qualiapproche.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.Key;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 03/09/2022-10:15
 **/

public final class AppUtils {
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_FORMAT_HOUR = "dd/MM/yyyy HH:mm:ss";
    private static final String ENCRYPTION_KEY = "o9szYIOq1rRMiouNhNvaq96lqUvCekxR";

    public static LocalDate formateStringToLocalDate(final String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
        return date != null ? LocalDate.parse(date, formatter) : null;
    }

    public static String formateLocalDateToString(final LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String formatInstantToString(final Instant instant) {
        if (instant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.from(ZoneOffset.UTC));
            return formatter.format(instant);
        }
        return null;
    }

    public static String formatLocalDateToLetter(final LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
    }

    public static String formatAmountForJasper(final Double amount) {
        Locale locale = new Locale("fr", "FR");
        DecimalFormat decimalFormat = (DecimalFormat)
                NumberFormat.getNumberInstance(locale);
        decimalFormat.applyPattern("###,###");
        return decimalFormat.format(amount).replace("\u00A0", " ");
    }

    public static String formatAmountForJasper(final BigDecimal amount) {
        Locale locale = new Locale("fr", "FR");
        DecimalFormat decimalFormat = (DecimalFormat)
                NumberFormat.getNumberInstance(locale);
        decimalFormat.applyPattern("###,###");
        return decimalFormat.format(amount).replace("\u00A0", " ");
    }

    public static String formatAmountForJasper(final String amount) {
        if (amount != null && !amount.isEmpty()) {
            BigDecimal bigDecimal = new BigDecimal(amount);
            Locale locale = new Locale("fr", "FR");
            DecimalFormat decimalFormat = (DecimalFormat)
                    NumberFormat.getNumberInstance(locale);
            decimalFormat.applyPattern("###,###");
            return decimalFormat.format(bigDecimal).replace("\u00A0", " ");
        } else {
            return "0";
        }
    }

    public static String generateSequenceNumber(final String lastNume) {
        DecimalFormat df = new DecimalFormat("000000000");
        int value = Integer.parseInt(lastNume) + 1;
        return df.format(value);
    }

    public static String convertInstantToString(final Instant date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        return formatter.format(date);
    }

    public static String convertLocalDateToString(final LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(DATE_FORMAT);
        if (date != null) {
            return date.format(formatter);
        } else {
            return null;
        }

    }

    public static BigDecimal somme(final List<BigDecimal> valeurs) {
        final BigDecimal[] total = {BigDecimal.ZERO};
        valeurs.forEach(s -> {
            total[0] = total[0].add(s);
        });
        return total[0];
    }

    public static String generateKey(final String lastNume) {
        DecimalFormat df = new DecimalFormat("000000");
        int value = Integer.parseInt(lastNume) + 1;
        return df.format(value);
    }

    public static BigDecimal zeroIfNull(final BigDecimal bigDecimal) {
        return bigDecimal == null ? BigDecimal.ZERO : bigDecimal;
    }

    public static Long zeroIfNull(final Long aLong) {
        return aLong == null ? 0L : aLong;
    }

    public static Integer zeroIfNull(final Integer integer) {
        return integer == null ? 0 : integer;
    }

    public static Double zeroIfNull(final Double aDouble) {
        return aDouble == null ? 0D : aDouble;
    }

    public static Integer monthNumberFromLocalDate(final Instant date) {
        if (date != null) {
            LocalDate myDate = LocalDate.ofInstant(date, ZoneId.systemDefault());
            if (myDate != null) {
                return myDate.getMonth().getValue();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static Integer monthNumberFromLocalDate(final LocalDate date) {
        if (date != null) {
            return date.getMonth().getValue();
        } else {
            return 0;
        }
    }

    public static HashMap<Integer, String> buildMoisList() {
        HashMap<Integer, String> hashMapList = new HashMap<>();
        hashMapList.put(1, "JANVIER");
        hashMapList.put(2, "FÉVRIER");
        hashMapList.put(3, "MARS");
        hashMapList.put(4, "AVRIL");
        hashMapList.put(5, "MAI");
        hashMapList.put(6, "JUIN");
        hashMapList.put(7, "JUILLET");
        hashMapList.put(8, "AOÛT");
        hashMapList.put(9, "SEPTEMBRE");
        hashMapList.put(10, "OCTOBRE");
        hashMapList.put(11, "NOVEMBRE");
        hashMapList.put(12, "DÉCECEMBRE");
        return hashMapList;
    }

    public static Instant formatLocalDateToInstant(final LocalDate date) {
        if (date != null) {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else {
            return null;
        }
    }


    public static String encrypt(String value) {
        try {
            Key clef = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, clef);
            return new String(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    public String decrypt(String value) {
        try {
            Key clef = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, clef);
            return new String(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDecimal(BigDecimal number) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' ');
        DecimalFormat decimalFormat = new DecimalFormat("#,##0", symbols);
        return decimalFormat.format(number);
    }

    public static String convertInstantToStringAndHour(final Instant date) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(DATE_FORMAT_HOUR)
                .withLocale(Locale.FRENCH)
                .withZone(ZoneId.systemDefault());
        return formatter.format(date);
    }


  public static String formatDemandNumber(String numero) {
    final String[] numeroSplited = numero.split("/");
    final int length = numeroSplited.length;
    final String numeroDemande = numeroSplited[length - 2].concat("-").concat(numeroSplited[length - 1]);
    return numeroDemande;
  }
}
