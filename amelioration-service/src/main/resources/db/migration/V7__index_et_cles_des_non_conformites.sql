--
-- amelioration-service — les non-conformités reçoivent leurs index et leurs clés étrangères.
--
-- quali_nc n'avait aucun index hors sa clé primaire : chaque liste, chaque compteur, chaque
-- « mes dossiers » parcourait la table entière. Et rien ne liait un dossier à son type, son niveau,
-- son efficacité ou son action, pourtant dans la même base : supprimer un type laissait des
-- dossiers qui le désignaient dans le vide.
--
-- Les clés sont posées NOT VALID : elles tiennent toute écriture à venir, sans juger les lignes
-- déjà là. La base de test portait des plans d'action dont le dossier avait été supprimé — l'ancien
-- défaut de suppression, corrigé depuis — et une clé validée refusait de se poser, empêchant le
-- service de démarrer. Ces lignes sont l'historique de ce qui a été fait : on ne les efface ni ne
-- les détache pour satisfaire une contrainte.
--
-- Les suppressions concernées répondent désormais 409 en le disant, au lieu d'une erreur interne.
--

-- Index : les filtres réels des écrans et des requêtes du service.
CREATE INDEX idx_quali_nc_structure_statut ON public.quali_nc (structure_soumission_id, status);
CREATE INDEX idx_quali_nc_origine ON public.quali_nc (origine_id);
CREATE INDEX idx_quali_nc_impute_etat ON public.quali_nc (user_imput_id, etat_traitement);
CREATE INDEX idx_quali_nc_createur ON public.quali_nc (created_by_id);
CREATE INDEX idx_quali_nc_direction_statut ON public.quali_nc (direction_id, status);
CREATE INDEX idx_quali_nc_creation ON public.quali_nc (created_at);
CREATE INDEX idx_plan_action_dossier ON public.plan_action (non_conforme_id);
CREATE INDEX idx_piece_jointe_entite ON public.piece_jointe (entity_id);

-- Clés étrangères : RESTRICT, un paramétrage employé ne se supprime pas. NOT VALID : voir plus haut.
ALTER TABLE public.quali_nc ADD CONSTRAINT fk_quali_nc_type
    FOREIGN KEY (type_non_conformite_id) REFERENCES public.type_non_conformite (id) NOT VALID;
ALTER TABLE public.quali_nc ADD CONSTRAINT fk_quali_nc_niveau
    FOREIGN KEY (niveau_non_conformite_id) REFERENCES public.niveau_non_conformite (id) NOT VALID;
ALTER TABLE public.quali_nc ADD CONSTRAINT fk_quali_nc_efficacite
    FOREIGN KEY (efficacite_id) REFERENCES public.efficacite (id) NOT VALID;
ALTER TABLE public.quali_nc ADD CONSTRAINT fk_quali_nc_action
    FOREIGN KEY (action_id) REFERENCES public.action (id) NOT VALID;
ALTER TABLE public.plan_action ADD CONSTRAINT fk_plan_action_dossier
    FOREIGN KEY (non_conforme_id) REFERENCES public.quali_nc (id) NOT VALID;
