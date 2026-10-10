--
-- evaluation-service — le nom d'origine des documents chargés (plan reçu, rapport externe).
--
-- Seule la référence sur le serveur de fichiers était gardée, sans le nom ni l'extension : l'écran
-- ne pouvait pas dire quel fichier avait été chargé, et le téléchargement rendait un fichier
-- « plan-audit » sans extension, que le poste ne savait pas ouvrir. Les fichiers déjà chargés
-- restent sans nom : le téléchargement leur garde le nom générique.
--
ALTER TABLE public.plan_audit ADD COLUMN nom_fichier_recu varchar(255);
ALTER TABLE public.audit ADD COLUMN nom_fichier_rapport varchar(255);
