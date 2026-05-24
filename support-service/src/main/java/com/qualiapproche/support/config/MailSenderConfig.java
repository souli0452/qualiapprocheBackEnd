package com.qualiapproche.support.config;

import com.qualiapproche.common.config.MailConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailSenderConfig {

    @Bean
    public JavaMailSender javaMailSender(MailConfig mailConfig) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailConfig.getHost() != null ? mailConfig.getHost() : "localhost");
        mailSender.setPort(mailConfig.getPort() != 0 ? mailConfig.getPort() : 587);
        mailSender.setUsername(mailConfig.getUsername());
        mailSender.setPassword(mailConfig.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", mailConfig.getProtocol() != null ? mailConfig.getProtocol() : "smtp");
        props.put("mail.smtp.auth", mailConfig.getAuth() != null ? mailConfig.getAuth() : "true");
        props.put("mail.smtp.starttls.enable", mailConfig.getStarttlsEnable() != null ? mailConfig.getStarttlsEnable() : "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}
