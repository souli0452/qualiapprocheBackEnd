package com.qualiapproche.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;









@Data
@AllArgsConstructor
@NoArgsConstructor


@SuperBuilder
public class PrestataireDto extends AuditEntityDto {
    private String nomPrestataire;
    private String adressePrestataire;
    private String telephonePrestataire;
    private String contactPrincipalPrestataire;
    private String emailPrestataire;
    private String siteWebPrestataire;
    private String statutPrestataire;
}
