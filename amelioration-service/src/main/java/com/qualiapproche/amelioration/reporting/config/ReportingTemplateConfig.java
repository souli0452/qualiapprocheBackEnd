package com.qualiapproche.amelioration.reporting.config;

import com.qualiapproche.amelioration.reporting.dto.EReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author :  <A HREF="mailto:siguizana08@gmail.com">TRAORE BRAHIMA</A>
 * @version : 1.0
 * @since : 2022/02/09 à 00:16
 */

@Configuration
public class ReportingTemplateConfig {
    private static final String REPORT_ROOT = "/reports/";;
    private static final String NON_CONFORMITE = REPORT_ROOT.concat("conformite.jasper");

    /**
     * Building a rest template instance.
     *
     * @return {@link ReportingTemplate}
     */
    @Bean
    public ReportingTemplate configure() {
        Map<String, String> map = Stream.of(
                new AbstractMap.SimpleImmutableEntry<>(EReportType.NON_CONFORMITE.name(), NON_CONFORMITE)
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new ReportingTemplate(map);
    }

    @Data
    @AllArgsConstructor
    public static class ReportingTemplate {
        private Map<String, String> templateMap;
    }
}
