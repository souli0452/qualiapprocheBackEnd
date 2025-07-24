package com.qualiapproche.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qualiapproche.enumeration.Etat;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(NON_NULL)
@MappedSuperclass
@SuperBuilder
public class RejectNonConformiteDto {
    private UUID id;
    private String rejectReason;
    private Etat etapeTraitement;
}
