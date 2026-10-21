--
-- evaluation-service — le jour où l'audit a réellement démarré.
--
-- Un audit prévu le 22/10 a pu démarrer le 17/10 sans que la date réelle soit retenue (recette du
-- 17 oct. 2026, observation 13). Elle se pose au démarrage ; les audits déjà démarrés la gardent nulle.
--
ALTER TABLE public.audit ADD COLUMN date_debut_reelle date;
