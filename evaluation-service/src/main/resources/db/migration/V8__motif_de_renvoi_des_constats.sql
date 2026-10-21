--
-- evaluation-service — le renvoi d'un constat à son auteur garde son motif à part.
--
-- Le motif était écrit dans commentaire_equipe : il effaçait le commentaire de la réunion
-- d'équipe déjà saisi (recette du 17 oct. 2026, observation 23). Il a désormais sa colonne, avec
-- qui l'a renvoyé et quand : l'auteur le lit sur son brouillon.
--
ALTER TABLE public.constat ADD COLUMN motif_renvoi text;
ALTER TABLE public.constat ADD COLUMN renvoye_le timestamp(6) without time zone;
ALTER TABLE public.constat ADD COLUMN renvoye_par character varying(255);
