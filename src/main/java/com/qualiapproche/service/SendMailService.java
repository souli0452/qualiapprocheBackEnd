package com.qualiapproche.service;
public interface SendMailService {
  void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link, String templateName);

}
