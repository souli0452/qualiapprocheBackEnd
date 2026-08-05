package com.qualiapproche.common.utils;


import com.qualiapproche.common.config.ThymeleafConfig;
import com.qualiapproche.common.config.MailConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class MailUtils {

    public static void sendEmailWithTheamleafEngine(EmailMessage emailMessage, MailConfig mailConfig,
            Map<String, Object> variables, List<File> attachments, String templateName)
            throws MessagingException, IOException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", mailConfig.getAuth() != null ? mailConfig.getAuth() : "true");
        props.put("mail.smtp.starttls.enable", mailConfig.getStarttlsEnable() != null ? mailConfig.getStarttlsEnable() : "true");
        props.put("mail.smtp.host", mailConfig.getHost() != null ? mailConfig.getHost() : "localhost");
        props.put("mail.smtp.protocol", mailConfig.getProtocol() != null ? mailConfig.getProtocol() : "smtp");
        props.put("mail.smtp.port", mailConfig.getPort() != 0 ? mailConfig.getPort() : 587);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailConfig.getUsername(), mailConfig.getPassword());
            }
        });

        // Génération du corps du message à partir du template Thymeleaf
        Context context = new Context();
        context.setVariables(variables);
        String htmlBody = ThymeleafConfig.getTemplateEngine().process(templateName, context);

        if (emailMessage == null || emailMessage.getToAddress() == null || emailMessage.getToAddress().trim().isEmpty()) {
            System.err.println("Warning: Attempted to send email but toAddress is null or empty. Subject: "
                    + (emailMessage != null ? emailMessage.getSubject() : "N/A"));
            return;
        }

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(mailConfig.getUsername(), false));

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailMessage.getToAddress()));
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
