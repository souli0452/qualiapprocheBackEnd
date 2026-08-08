--
-- referentiel-service — retrait de l'ancienne table d'abonnement.
--
-- `abonnements_directions` portait la licence d'avant : dates et modules chiffrés avec une clé
-- symétrique livrée dans le produit, renseignés au démarrage depuis `tenant-init.json`. Elle
-- n'autorisait plus rien depuis que la licence est signée par l'éditeur et vérifiée à l'entrée
-- (V3, `licences_installees`) — mais elle continuait d'alimenter les écrans et les rappels
-- d'échéance. Deux sources de vérité pour une même question, dont celle qui n'engageait rien
-- pilotait l'affichage : une installation pouvait montrer des modules ouverts jusqu'en 2027
-- pendant que la passerelle refusait chaque écriture en 402.
--
-- Rien à reprendre de son contenu : une licence valide est signée, et aucune ligne d'ici ne
-- l'est. Les installations existantes posent leur licence, ou démarrent l'essai gratuit.
--
-- CASCADE emporte la clé étrangère vers `structures` et la contrainte d'unicité qui l'accompagne.
--

DROP TABLE IF EXISTS public.abonnements_directions CASCADE;
