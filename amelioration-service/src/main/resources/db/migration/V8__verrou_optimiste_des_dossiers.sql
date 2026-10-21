--
-- amelioration-service — un verrou optimiste sur les non-conformités et leurs plans d'action.
--
-- Le webhook du circuit, la saisie de l'imputé et la décision d'un validateur écrivent le même
-- dossier ; sans verrou, le dernier écrivait par-dessus les autres, sans que personne le sache.
-- Chaque écriture compare désormais ce compteur et l'incrémente ; la perdante reçoit un 409.
--
ALTER TABLE public.quali_nc ADD COLUMN verrou_version bigint NOT NULL DEFAULT 0;
ALTER TABLE public.plan_action ADD COLUMN verrou_version bigint NOT NULL DEFAULT 0;
