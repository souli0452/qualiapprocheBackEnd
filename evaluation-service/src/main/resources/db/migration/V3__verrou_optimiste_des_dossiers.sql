--
-- evaluation-service — un verrou optimiste sur les audits et les constats.
--
-- Responsable d'équipe, auditeurs et validateurs écrivent le même audit, et un constat se relit
-- pendant qu'on le compile ; sans verrou, la dernière écriture effaçait les autres en silence.
-- Chaque écriture compare désormais ce compteur et l'incrémente ; la perdante reçoit un 409.
--
ALTER TABLE public.audit ADD COLUMN verrou_version bigint NOT NULL DEFAULT 0;
ALTER TABLE public.constat ADD COLUMN verrou_version bigint NOT NULL DEFAULT 0;
