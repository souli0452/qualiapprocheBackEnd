package com.qualiapproche.utils;
import com.qualiapproche.config.ThymeleafConfig;
import com.qualiapproche.config.utils.MailConfig;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.context.Context;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
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
import java.util.*;

/**
 * @author : <a href="siguizana08@gmail.com">BRAHIMA TRAORE </a>.
 * @version : 1.0
 * @since : 03/09/2022-10:15
 **/
@SuppressWarnings("ALL")
@Slf4j
public final class AppUtils {
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String DATE_FORMAT_HOUR = "dd/MM/yyyy HH:mm:ss";
    private static final String key = "o9szYIOq1rRMiouNhNvaq96lqUvCekxR";


  /**
     * Formate String en LocalDate.
     *
     * @param date
     * @return LocalDate
     */
    public static LocalDate formateStringToLocalDate(final String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MM/yyyy");
        return date != null ? LocalDate.parse(date, formatter) : null;
    }

    /**
     * Formate LocalDate to String.
     *
     * @param localDate
     * @return String
     */
    public static String formateLocalDateToString(final LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Formate Instance to string.
     *
     * @param instant
     * @return String
     */
    public static String formatInstantToString(final Instant instant) {
        if (instant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.from(ZoneOffset.UTC));
            return formatter.format(instant);
        }
        return null;
    }

    /**
     * Formate LocalDate to letter.
     *
     * @param localDate
     * @return String
     */
    public static String formatLocalDateToLetter(final LocalDate localDate) {
        return localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
    }

    /**
     * formats the amount to jasper amount. exp: 100 000 000.
     *
     * @param amount
     * @return the formatted amount as string
     */
    public static String formatAmountForJasper(final Double amount) {
        Locale locale = new Locale("fr", "FR");
        DecimalFormat decimalFormat = (DecimalFormat)
                NumberFormat.getNumberInstance(locale);
        decimalFormat.applyPattern("###,###");
        return decimalFormat.format(amount).replace("\u00A0", " ");
    }

    /**
     * ormats the amount to jasper amount. exp: 100 000 000.
     *
     * @param amount
     * @return String
     */
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

    /**
     * Génération du numero de commande.
     *
     * @param lastNume
     * @return String
     */
    public static String generateSequenceNumber(final String lastNume) {
        DecimalFormat df = new DecimalFormat("000000000");
        int value = Integer.parseInt(lastNume) + 1;
        return df.format(value);
    }

    /**
     * Instant date converter.
     *
     * @param date date to convert
     * @return Converted date
     */
    public static String convertInstantToString(final Instant date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        return formatter.format(date);
    }

    /**
     * LocalDate to srting.
     *
     * @param date
     * @return String
     */
    public static String convertLocalDateToString(final LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern(DATE_FORMAT);
        if (date != null) {
            return date.format(formatter);
        } else {
            return null;
        }

    }

    /**
     * Faire la somme d'une liste de BiDecimal.
     *
     * @param valeurs
     * @return BigDecimal
     */
    public static BigDecimal somme(final List<BigDecimal> valeurs) {
        final BigDecimal[] total = {BigDecimal.ZERO};
        valeurs.forEach(s -> {
            total[0] = total[0].add(s);
        });
        return total[0];
    }

    /**
     * Article key.
     *
     * @param lastNume
     * @return String
     */
    public static String generateKey(final String lastNume) {
        DecimalFormat df = new DecimalFormat("000000");
        int value = Integer.parseInt(lastNume) + 1;
        return df.format(value);
    }
 /*   public static String formatAmountInLetter(final Long montant, final String prefix) {
        return FrenchNumberToWords.convert(montant) + prefix;
    }
*/
    /**
     * Converte amount in Capitalize letter.
     *
     * @param montant
     * @return String
     */
   /* public static String formatAmountInLetterCapitalize(final BigDecimal montant) {
        if (montant != null) {
            return StringUtils.capitalize(formatAmountInLetter(montant.longValueExact(), " "));
        }
        return "";
    }*/

    /**
     * Returns zero when the given BigDecimal is null.
     *
     * @param bigDecimal
     * @return BigDecimal
     */
    public static BigDecimal zeroIfNull(final BigDecimal bigDecimal) {
        return bigDecimal == null ? BigDecimal.ZERO : bigDecimal;
    }

    /**
     * Returns zero when the given Long is null.
     *
     * @param aLong
     * @return Long
     */
    public static Long zeroIfNull(final Long aLong) {
        return aLong == null ? 0L : aLong;
    }

    /**
     * Returns zero when the given Integer is null.
     *
     * @param integer
     * @return Integer
     */
    public static Integer zeroIfNull(final Integer integer) {
        return integer == null ? 0 : integer;
    }

    /**
     * Returns zero when the given Double is null.
     *
     * @param aDouble
     * @return Double
     */
    public static Double zeroIfNull(final Double aDouble) {
        return aDouble == null ? 0D : aDouble;
    }

    /**
     * Return moth number.
     *
     * @param date Integer
     * @return Integer
     */
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

    /**
     * Return moth number.
     *
     * @param date Integer
     * @return Integer
     */
    public static Integer monthNumberFromLocalDate(final LocalDate date) {
        if (date != null) {
            return date.getMonth().getValue();
        } else {
            return 0;
        }
    }

    /**
     * Build mois list.
     *
     * @return {@link HashMap <Integer, String>}
     */
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

    /**
     * LocaDate to Instant.
     *
     * @param date
     * @return Instant
     */
    public static Instant formatLocalDateToInstant(final LocalDate date) {
        if (date != null) {
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } else {
            return null;
        }
    }


    public static String encrypt(String value) {
        try {
            Key clef = new SecretKeySpec(key.getBytes("UTF-8"), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, clef);
            return new String(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            return null;
        }
    }

    public String decrypt(String value) {
        try {
            Key clef = new SecretKeySpec(key.getBytes("UTF-8"), "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, clef);
            return new String(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            log.error("{}", e.getMessage());
            return null;
        }
    }

    public static String formatDecimal(BigDecimal number) {
        // Customize the DecimalFormatSymbols
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(' '); // Use space as grouping separator
        // Create DecimalFormat with the custom symbols
        DecimalFormat decimalFormat = new DecimalFormat("#,##0", symbols);
        return decimalFormat.format(number);
    }

    /**
     * Instant date converter.
     *
     * @param date date to convert
     * @return Converted date
     */
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


  public static void sendEmailWithTheamleafEngine(EmailMessage emailMessage, MailConfig mailConfig, Map<String, Object> variables, List<File> attachments, String templateName) throws MessagingException, IOException {
    Properties props = new Properties();
    props.put("mail.smtp.auth", mailConfig.getAuth());
    props.put("mail.smtp.starttls.enable", mailConfig.getStarttlsEnable());
    props.put("mail.smtp.host", mailConfig.getHost());
    props.put("mail.smtp.protocol", mailConfig.getProtocol());
    props.put("mail.smtp.port", mailConfig.getPort());

    Session session = Session.getInstance(props, new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
      }
    });

    // Génération du corps du message à partir du template Thymeleaf
    Context context = new Context();
    context.setVariables(variables);
    String htmlBody = ThymeleafConfig.getTemplateEngine().process(templateName, context);

    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(mailConfig.getUsername(), false));

    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailMessage.getTo_address()));
    msg.setSubject(emailMessage.getSubject());
    msg.setSentDate(new Date());

    // Contenu principal de l'email
    MimeBodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(htmlBody, "text/html; charset=utf-8");

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    // Ajouter des pièces jointes
    if (attachments != null) {
      for (File file : attachments) {
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.attachFile(file);
        multipart.addBodyPart(attachmentBodyPart);
      }
    }

    msg.setContent(multipart);
    Transport.send(msg);
  }



}
