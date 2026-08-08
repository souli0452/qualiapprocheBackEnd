--
-- referentiel-service — le code du partenaire chez qui cette installation tourne.
--
-- C'est le repère auquel une licence est confrontée. La signature prouve qu'une licence n'a pas
-- été fabriquée ; elle ne dit rien de l'installation qui la présente. Sans ce repère, le code
-- inscrit dans la licence n'est qu'une étiquette d'affichage, et la licence d'un client
-- s'installe chez un autre et y ouvre tout.
--
-- Renseigné au provisionnement depuis `tenant-init.json`, où il est écrit en clair pour rester
-- relisible, et rangé ici obfusqué. Que l'on ne s'y trompe pas sur ce que cette obfuscation
-- vaut : la clé de `CryptoUtils` est livrée avec le produit, donc qui détient l'application peut
-- déchiffrer cette colonne et en réécrire une autre. Elle décourage la retouche désinvolte, elle
-- ne l'interdit pas — exactement comme la V4 le rappelle de l'ancienne table d'abonnement.
--
-- Ce qui protège réellement reste ailleurs, et n'a pas bougé : la licence est SIGNÉE par une clé
-- privée qui ne quitte jamais l'outil d'émission, et sa durée est courte.
--
-- Nul est admis : une installation qui n'a pas déclaré son partenaire n'exerce aucun contrôle du
-- destinataire — le démarrage le signale plutôt que de laisser croire à une protection qui
-- n'opère pas.
--

ALTER TABLE public.structures
    ADD COLUMN IF NOT EXISTS code_partenaire character varying(120);

COMMENT ON COLUMN public.structures.code_partenaire IS
    'Code du partenaire (obfusque) auquel les licences installees doivent correspondre.';
