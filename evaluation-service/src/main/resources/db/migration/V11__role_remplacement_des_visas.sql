--
-- evaluation-service — le circuit de signature du rapport : à quel titre on signe, et le
-- remplacement d'un signataire qui n'a pas encore signé.
--
-- Après la première signature, un signataire absent ou choisi par erreur ne pouvait plus être
-- remplacé, et la clôture restait bloquée (recette du 17 oct. 2026, observation 26). Le visa
-- remplacé n'est pas effacé : il reste dans le circuit, marqué REMPLACEE, avec qui l'a remplacé
-- et quand.
--
ALTER TABLE public.signature ADD COLUMN role_visa character varying(16);
ALTER TABLE public.signature ADD COLUMN remplace_le timestamp(6) without time zone;
ALTER TABLE public.signature ADD COLUMN remplace_par character varying(255);
