--
-- referentiel-service — la licence de l'installation.
--
-- QualiSira s'installe chez le client : la base, les fichiers et le serveur lui appartiennent.
-- Aucun secret conservé ici ne protège donc quoi que ce soit — c'est pourquoi la licence n'est pas
-- chiffrée mais SIGNÉE par l'éditeur. Le jeton est revérifié à chaque lecture avec la clé publique
-- embarquée dans le produit : modifier une colonne de cette table à la main n'ouvre rien, la
-- signature ne suivrait pas.
--
-- Les licences successives sont conservées : la plus récemment installée s'applique, les
-- précédentes disent l'historique de l'abonnement.
--

CREATE TABLE IF NOT EXISTS public.licences_installees (
    id                uuid NOT NULL,
    -- Le jeton signé, tel que l'administrateur l'a collé. Nul pour un essai local, que personne
    -- ici ne peut signer — et c'est bien pourquoi l'essai est court.
    jeton             character varying(4000),
    type              character varying(20) NOT NULL,
    reference         character varying(40),
    partenaire_code   character varying(40),
    partenaire_nom    character varying(200),
    debut             date NOT NULL,
    fin               date NOT NULL,
    -- Copie des modules ouverts, pour ne pas relire le jeton à chaque requête. En cas de
    -- divergence, c'est le jeton qui fait foi.
    modules           character varying(1000),
    utilisateurs_max  integer NOT NULL DEFAULT 0,
    installee_le      timestamp(6) without time zone NOT NULL,
    installee_par     character varying(120),
    -- Jour le plus avancé jamais observé : une horloge reculée ne prolonge pas une licence.
    dernier_jour_vu   date,
    CONSTRAINT licences_installees_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_licences_installees_installee_le
    ON public.licences_installees (installee_le DESC);
