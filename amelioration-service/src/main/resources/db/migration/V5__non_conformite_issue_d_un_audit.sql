--
-- amelioration-service — V5 : une non-conformité peut naître d'un audit (module M13).
--
-- Un constat d'audit ou un critère RQAP-BF jugé non conforme est transmis ici à sa publication.
-- La transmission peut être rejouée — réseau coupé après la création, reprise depuis l'audit —
-- et ne doit jamais ouvrir deux dossiers pour un même écart. `cle_origine` nomme l'écart d'origine
-- (« AUDIT-CONSTAT:<uuid> », « AUDIT-RQAPBF:<uuid> ») ; l'unicité rend le rejeu inoffensif.
--
-- Nulle pour toute non-conformité déclarée directement : l'index unique ignore les nuls.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte.
--

ALTER TABLE public.quali_nc ADD COLUMN cle_origine character varying(120);

CREATE UNIQUE INDEX uk_quali_nc_cle_origine ON public.quali_nc (cle_origine);

COMMENT ON COLUMN public.quali_nc.cle_origine IS
    'Ecart d''origine quand la NC vient d''un autre module (audit) ; garantit qu''une transmission rejouee ne la duplique pas.';
