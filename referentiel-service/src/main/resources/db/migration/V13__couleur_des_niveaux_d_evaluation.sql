--
-- referentiel-service — une couleur par niveau de l'échelle d'évaluation des auditeurs.
--
-- La teinte d'un niveau se déduisait de son rang, et seulement dans le tableau du paramétrage :
-- elle devient un réglage, repris partout où le niveau s'affiche (notation de l'équipe,
-- portefeuille des auditeurs). Les niveaux déjà posés reçoivent la teinte qu'ils avaient selon
-- leur rang dans l'échelle de leur direction : rouge, orange, bleu, vert clair, vert, du plus bas au
-- plus haut.
--
ALTER TABLE public.niveau_evaluation_auditeur ADD COLUMN IF NOT EXISTS couleur varchar(7);

UPDATE public.niveau_evaluation_auditeur n
SET couleur = CASE
        WHEN x.rang < 0.2 THEN '#dc2626'
        WHEN x.rang < 0.4 THEN '#f59e0b'
        WHEN x.rang < 0.6 THEN '#2563eb'
        WHEN x.rang < 0.8 THEN '#65a30d'
        ELSE '#15803d'
    END
FROM (SELECT id,
             CASE WHEN count(*) OVER (PARTITION BY direction_id) > 1
                  THEN percent_rank() OVER (PARTITION BY direction_id ORDER BY ordre)
                  ELSE 1 END AS rang
      FROM public.niveau_evaluation_auditeur) x
WHERE n.id = x.id;
