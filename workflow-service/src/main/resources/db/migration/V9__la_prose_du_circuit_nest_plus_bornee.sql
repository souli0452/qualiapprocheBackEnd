--
-- workflow-service — ce qui se rédige ne se compte plus en caractères.
--
-- L'observation d'une décision d'étape était bornée à 255 caractères : le défaut de la colonne,
-- jamais choisi. Une motivation un peu argumentée — celle qui explique un rejet, précisément
-- celle qu'on relit six mois plus tard — était refusée par la base au moment de décider, quand
-- elle est le plus longue à réécrire. Les autres bornes étaient tout aussi arbitraires : 255 pour
-- la description d'un circuit ou d'une étape, 500 pour celle d'un gabarit de courriel, 1000 pour
-- le texte d'une ligne de boîte de réception, 2000 pour l'observation portée par le dossier.
--
-- `text` n'est pas un type coûteux dans PostgreSQL : il se stocke exactement comme un
-- `varchar(n)`, la longueur déclarée n'étant qu'une contrainte vérifiée à l'écriture. On ne perd
-- donc rien à la retirer, et on gagne de ne plus refuser une saisie pour une limite que personne
-- n'a décidée.
--
-- L'élargissement ne perd aucune donnée et ne réécrit pas la table : PostgreSQL traite
-- varchar(n) -> text comme un changement de type sans réécriture.
--
-- Volontairement absent : `nom`, `code`, `libelle`, `titre` et les identifiants restent bornés.
-- Ce sont des repères, pas de la prose — les laisser croître sans limite abîmerait les écrans qui
-- les affichent en colonne.
--

ALTER TABLE public.workflow_validation_history
    ALTER COLUMN comments TYPE text;

ALTER TABLE public.workflow_validation_instance
    ALTER COLUMN observation TYPE text;

ALTER TABLE public.workflow
    ALTER COLUMN description TYPE text;

ALTER TABLE public.workflow_step
    ALTER COLUMN description TYPE text;

ALTER TABLE public.workflow_email_template
    ALTER COLUMN description TYPE text;

ALTER TABLE public.notification_utilisateur
    ALTER COLUMN message TYPE text;
