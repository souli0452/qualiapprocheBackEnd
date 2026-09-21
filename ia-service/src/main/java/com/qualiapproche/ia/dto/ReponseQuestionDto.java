package com.qualiapproche.ia.dto;

import com.qualiapproche.ia.enumeration.QuestionPredefinie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** La réponse à une question prédéfinie : les données d'abord, le commentaire ensuite. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReponseQuestionDto {

    private QuestionPredefinie code;
    private String libelle;

    /** Les lignes réelles, à afficher sans retouche. */
    private List<ElementReponseDto> elements;

    /** Les chiffres réels, à afficher sans retouche — une question de statistiques n'a pas de lignes. */
    private List<ChiffreDto> chiffres;

    /** Total annoncé par le service métier — il peut dépasser ce que porte {@code elements}. */
    private long total;

    /** La phrase de l'assistant. Elle accompagne les données ; elle ne les remplace pas. */
    private String commentaire;

    /** Le fil auquel l'échange a été rattaché, pour poursuivre la conversation. */
    private UUID conversationId;

    private String avertissement;
}
