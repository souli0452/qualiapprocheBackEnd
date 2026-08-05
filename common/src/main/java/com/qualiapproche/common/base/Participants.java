package com.qualiapproche.common.base;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Participants {
    @ElementCollection
    @CollectionTable(name = "non_conformite_participants", joinColumns = @JoinColumn(name = "non_conformite_id"))
    @Column(name = "participants")
    private Set<String> fullNames = new HashSet<>();
}
