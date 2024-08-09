package com.qualiapproche.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.*;

import java.util.List;

@Entity
 @AllArgsConstructor @NoArgsConstructor @Getter @Setter
public class Produit extends AuditEntity {

    private String libelleProduit;
    private String descriptionProduit;
    @ManyToMany
    private List<Audite> audites;
}
