--
-- workflow-service — une étape peut écarter de sa décision celui qui a soumis le dossier.
--
-- Le rôle responsable d'une étape dit qui peut décider, et rien d'autre : un pilote de structure
-- qui dépose un document se trouvait habilité à le vérifier lui-même à l'étape suivante, puisqu'il
-- en porte le rôle. La séparation des signatures ne peut pas s'exprimer par un rôle — elle porte
-- sur une **identité** : celui-là, sur ce dossier-là.
--
-- D'où la colonne : les identifiants des personnes qui co-signent l'étape, séparés par des
-- virgules. Celle d'entre elles qui a ouvert le dossier n'y décide plus ; les autres signataires le
-- décident comme avant. Des personnes et non des rôles, car c'est justement ce que le rôle
-- responsable dit déjà, et ce qui manquait.
--
-- Nulle partout à l'installation de cette version, y compris sur les circuits livrés : la règle est
-- inactive tant qu'une étape ne nomme personne, et aucun dossier en cours ne change de sort. C'est
-- une décision d'organisation — une structure où une seule personne peut signer ne peut pas se
-- l'offrir sans immobiliser ses dossiers — et elle se prend depuis l'éditeur de circuits.
--

ALTER TABLE public.workflow_step
    ADD COLUMN IF NOT EXISTS cosignataires character varying(1000);
