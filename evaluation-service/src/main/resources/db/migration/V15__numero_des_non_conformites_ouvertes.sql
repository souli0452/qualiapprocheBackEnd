--
-- evaluation-service — le numéro de la fiche ouverte au module Non-conformités, gardé avec l'écart.
--
-- Le rapport ne disait pas quelle fiche NC chaque constat avait ouverte (recette du 17 oct. 2026,
-- observation 24). Le numéro est retenu à la transmission ; les écarts déjà transmis le gardent nul.
--
ALTER TABLE public.constat ADD COLUMN numero_non_conformite character varying(100);
ALTER TABLE public.notation_critere ADD COLUMN numero_non_conformite character varying(100);
