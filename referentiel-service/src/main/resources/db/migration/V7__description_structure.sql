--
-- referentiel-service — la description d'une structure trouve enfin sa colonne.
--
-- Le contrat d'API la promettait depuis le premier jour : « ce dont la structure a la charge,
-- utile là où deux intitulés se ressemblent ». L'écran la saisissait, l'envoyait, puis la voyait
-- revenir vide à la relecture. Rien ne la rejetait : le champ existait dans le DTO sans exister
-- dans l'entité, et le mapper écartait en silence ce qu'il ne savait où poser. Une description
-- saisie n'était donc jamais écrite, et une structure relue n'en avait jamais.
--
-- Ajout d'une colonne nulle : les structures en place n'ont rien à décrire tant que personne ne
-- l'a écrit, et aucune valeur n'est à reprendre puisqu'aucune n'a jamais été conservée.
--
-- Même longueur que la valeur d'un paramètre : assez pour une phrase ou deux, pas un texte libre.
--

ALTER TABLE public.structures
    ADD COLUMN IF NOT EXISTS description character varying(2000);

COMMENT ON COLUMN public.structures.description IS
    'Ce dont la structure a la charge ; distingue deux intitules qui se ressemblent.';
