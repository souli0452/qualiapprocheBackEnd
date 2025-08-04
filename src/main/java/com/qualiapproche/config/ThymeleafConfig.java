package com.qualiapproche.config;

import lombok.Getter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class ThymeleafConfig {
  @Getter
  private static TemplateEngine templateEngine;

  static {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode("HTML");
    templateResolver.setCharacterEncoding("UTF-8");

    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
  }

}
