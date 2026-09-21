--
-- ia-service — V2 : index du contrôle du budget quotidien de jetons.
--
-- Avant chaque appel au modèle, le service somme les jetons consommés dans la journée, par
-- direction — ou par utilisateur quand le jeton ne porte pas de direction. Sans ces index, ce
-- contrôle balayerait toute la table de traçabilité à chaque sollicitation.
--

CREATE INDEX idx_suggestions_ia_direction_jour
    ON public.suggestions_ia (direction_id, created_at);

CREATE INDEX idx_suggestions_ia_createur_jour
    ON public.suggestions_ia (created_by_id, created_at);

COMMENT ON INDEX public.idx_suggestions_ia_direction_jour IS 'Appuie le décompte quotidien des jetons par direction (budget anti-dérive de coût).';
COMMENT ON INDEX public.idx_suggestions_ia_createur_jour IS 'Appuie le décompte quotidien des jetons par utilisateur, repli quand le jeton ne porte pas de direction.';
