package com.qualiapproche.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qualiapproche.listeners.UserIdListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.UUID;
@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners({UserIdListener.class})
public class AuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy ' à ' HH:mm:ss")
    private LocalDateTime createdAt;
    @Column(name = "update_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy ' à ' HH:mm:ss")
    private LocalDateTime UpdateAt;

}
