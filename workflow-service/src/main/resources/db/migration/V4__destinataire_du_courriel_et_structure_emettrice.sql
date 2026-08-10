--
-- workflow-service — une étape peut annoncer son franchissement ailleurs qu'à celui qui doit y agir.
--
-- Le courriel d'étape part vers les porteurs du rôle responsable, dans la structure où le dossier
-- se trouve. La clôture d'une non-conformité contredit cette règle sur les deux points : elle doit
-- être annoncée au pilote du processus **soumissionnaire**, alors que l'étape appartient au
-- responsable qualité et que le dossier a changé de structure à la validation qualité.
--
-- D'où deux colonnes :
--   * `workflow_step.destinataire_courriel` — la désignation « RÔLE@PORTÉE », vide partout ailleurs ;
--   * `workflow_validation_instance.structure_emettrice_id` — la structure d'origine du dossier,
--     que rien ne déplace, là où `structure_id` suit le dossier.
--
-- Les dossiers déjà ouverts reçoivent leur structure d'origine de celle qu'ils portent aujourd'hui :
-- c'est faux pour ceux qui ont déjà été transférés, exact pour tous les autres, et c'est de toute
-- façon la meilleure approximation disponible — la structure d'où ils partaient n'a été enregistrée
-- nulle part.
--

ALTER TABLE public.workflow_step
    ADD COLUMN IF NOT EXISTS destinataire_courriel character varying(80);

ALTER TABLE public.workflow_validation_instance
    ADD COLUMN IF NOT EXISTS structure_emettrice_id character varying(255);

UPDATE public.workflow_validation_instance
   SET structure_emettrice_id = structure_id
 WHERE structure_emettrice_id IS NULL
   AND structure_id IS NOT NULL;
