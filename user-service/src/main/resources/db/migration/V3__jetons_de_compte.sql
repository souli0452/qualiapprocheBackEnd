--
-- user-service — les liens envoyés par courriel portent un jeton vérifié.
--
-- La vérification d'adresse et la réinitialisation du mot de passe recevaient un `token` qu'elles
-- ne lisaient pas : connaître l'identifiant Keycloak d'un compte suffisait pour en fixer le mot de
-- passe, sans être connecté. Chaque lien porte désormais un jeton aléatoire dont seule l'empreinte
-- SHA-256 est gardée ici, à usage unique et borné dans le temps.
--
CREATE TABLE public.jeton_compte (
    id          uuid                        NOT NULL,
    user_id     varchar(64)                 NOT NULL,
    usage       varchar(32)                 NOT NULL,
    empreinte   varchar(64)                 NOT NULL,
    expire_le   timestamp without time zone NOT NULL,
    utilise_le  timestamp without time zone,
    cree_le     timestamp without time zone NOT NULL,
    CONSTRAINT jeton_compte_pkey PRIMARY KEY (id),
    CONSTRAINT uk_jeton_compte_empreinte UNIQUE (empreinte),
    CONSTRAINT ck_jeton_compte_usage CHECK (usage IN ('VERIFICATION_COURRIEL', 'REINITIALISATION'))
);

CREATE INDEX idx_jeton_compte_utilisateur ON public.jeton_compte (user_id, usage);
