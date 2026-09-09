--
-- amelioration-service — ce qui se rédige ne se compte plus en caractères.
--
-- Ces colonnes portent de la prose : une description, un commentaire, un motif. Leur longueur
-- était celle du défaut de la colonne — 255 le plus souvent —, jamais une limite choisie. Une
-- saisie un peu argumentée était refusée par la base, au moment précis où elle est le plus
-- longue à réécrire.
--
-- `text` ne coûte rien dans PostgreSQL : il se stocke exactement comme un `varchar(n)`, la
-- longueur déclarée n'étant qu'une contrainte vérifiée à l'écriture. L'élargissement ne perd aucune
-- donnée et ne réécrit pas la table.
--
-- Volontairement absents : `nom`, `code`, `libelle`, `titre` et les identifiants, qui restent
-- bornés. Ce sont des repères, pas de la prose.
--

ALTER TABLE public.action ALTER COLUMN description TYPE text;
ALTER TABLE public.audite ALTER COLUMN description_audite TYPE text;
ALTER TABLE public.efficacite ALTER COLUMN description TYPE text;
ALTER TABLE public.evaluation ALTER COLUMN description TYPE text;
ALTER TABLE public.niveau_non_conformite ALTER COLUMN description TYPE text;
ALTER TABLE public.risque ALTER COLUMN commentaire_risque TYPE text;
ALTER TABLE public.risque ALTER COLUMN description TYPE text;
ALTER TABLE public.type_non_conformite ALTER COLUMN description TYPE text;
