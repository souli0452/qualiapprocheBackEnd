--
-- referentiel-service — V9 : la FAQ perd sa rubrique et son rang.
--
-- Les deux colonnes sont nées avec V8 et n'ont jamais servi. La rubrique demandait à
-- l'administrateur d'inventer un classement avant d'avoir écrit sa première réponse ; le rang
-- lui demandait de les ordonner avant d'en avoir plusieurs. Deux décisions à prendre pour rien,
-- sur un écran dont l'intérêt est qu'on y écrive vite.
--
-- Ce qu'elles portaient se reporte sans perte. L'ordre d'affichage suit désormais la date de
-- création, que personne n'a à saisir. Le plafond de la consigne de l'assistant continue de
-- trancher par la fin : ce sont donc les réponses les plus anciennes qui survivent à la
-- troncature, et c'est défendable — une FAQ qui dépasse la trentaine d'entrées appelle un index,
-- pas un arbitrage entre elles.
--
-- V8 est déjà poussée : elle peut avoir été appliquée, et ne se modifie donc pas.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

DROP INDEX IF EXISTS public.idx_faq_publication;

ALTER TABLE public.faq DROP COLUMN IF EXISTS categorie;
ALTER TABLE public.faq DROP COLUMN IF EXISTS rang;

-- Le chargement ne demande que les entrées publiées d'une organisation, dans l'ordre où elles
-- ont été écrites : c'est exactement cet index.
CREATE INDEX idx_faq_publication ON public.faq (direction_id, publiee, created_at);
