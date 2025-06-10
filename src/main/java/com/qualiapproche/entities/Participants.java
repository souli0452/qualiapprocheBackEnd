package com.qualiapproche.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
