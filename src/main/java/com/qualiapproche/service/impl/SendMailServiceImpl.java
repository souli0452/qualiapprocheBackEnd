package com.qualiapproche.service.impl;
import com.qualiapproche.config.utils.MailConfig;
import com.qualiapproche.service.SendMailService;
import com.qualiapproche.utils.AppUtils;
import com.qualiapproche.utils.EmailMessage;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SendMailServiceImpl implements SendMailService {
  private final MailConfig mailConfig;


  @Override
  public void sendMailToUserAfterDemandImputed(String currentUserEmail, String subject, String link, String templateName,String fullName,String numeroNc,String observation) {
    EmailMessage emailMessage = EmailMessage.builder()
      .subject(subject)
      .to_address(currentUserEmail).build();

    try {
      Map<String, Object> variables = new HashMap<>();
      variables.put("link", link);
      variables.put("fullName", fullName);
      variables.put("numeroNc", numeroNc);
      variables.put("observation", observation);
      AppUtils.sendEmailWithTheamleafEngine(emailMessage, mailConfig, variables, Collections.emptyList(), templateName);
    } catch (MessagingException | IOException e) {
      throw new RuntimeException(e);
    }
  }





}
