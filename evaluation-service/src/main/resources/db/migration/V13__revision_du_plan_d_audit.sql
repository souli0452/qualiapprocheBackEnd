--
-- evaluation-service — un plan validé se révise : il repasse en brouillon, dans une nouvelle version.
--
-- Une fois validé, le plan ne se corrigeait plus, ni par dévalidation ni par nouvelle version
-- (recette du 17 oct. 2026, observation 18). La révision garde la trace de qui l'a ouverte et quand.
--
ALTER TABLE public.plan_audit ADD COLUMN version integer NOT NULL DEFAULT 1;
ALTER TABLE public.plan_audit ADD COLUMN revise_le timestamp(6) without time zone;
ALTER TABLE public.plan_audit ADD COLUMN revise_par character varying(255);
