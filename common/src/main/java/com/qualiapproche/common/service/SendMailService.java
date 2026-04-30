package com.qualiapproche.common.service;

public interface SendMailService {
    void sendVerificationEmail(String recipientEmail, String firstName, String lastName, String password, String url);
    void sendReinitializePasswordEmail(String recipientEmail, String resetUrl);
    void sendResetPasswordEmail(String recipientEmail, String temporaryPassword, String url);
    void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link, String templateName, String fullName, String numeroNc, String observation);
}
