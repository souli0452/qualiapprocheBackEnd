--
-- evaluation-service — une activité du programme détaillé porte son jour.
--
-- Elle n'avait qu'une heure de début et de fin : sur un audit de plusieurs jours, les activités
-- de J1 et de J2 se mêlaient au tri par heure (recette du 17 oct. 2026, observation 15). Facultatif
-- pour les plans déjà élaborés, qui gardent leurs lignes telles quelles.
--
ALTER TABLE public.activite_plan ADD COLUMN jour date;
