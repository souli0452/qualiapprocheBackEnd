package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class PrestataireDto extends AuditEntityDto{
    private String nomPrestataire;
    private String adressePrestataire;
    private String telephonePrestataire;
    private String contactPrincipalPrestataire;
    private String emailPrestataire;
    private String siteWebPrestataire;
    private String statutPrestataire;
}
