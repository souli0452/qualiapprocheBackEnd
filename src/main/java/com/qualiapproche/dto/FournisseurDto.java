package com.qualiapproche.dto;

import com.qualiapproche.entities.Audite;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FournisseurDto extends AuditEntityDto{
    private String nom;
    private String adresse;
    private String telephone;
    private String email;
    private String siteWeb;
    private String contactPrincipal;
    private String statut;
    private List<Audite> audites=new ArrayList<>();

}
