package com.qualiapproche.ia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Un fil de conversation.
 *
 * <p>Les {@code messages} ne sont portés que lorsqu'on demande un fil précis : une liste de fils
 * qui traînerait tous leurs échanges rendrait des pages entières pour afficher des titres.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {

    private UUID id;
    private String titre;
    private int nombreMessages;
    private LocalDateTime derniereActiviteAt;
    private List<MessageDto> messages;
}
