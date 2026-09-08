--
-- workflow-service — la boîte de réception d'une personne.
--
-- L'entité est arrivée sans son script : le moteur dépose désormais une ligne quand un dossier
-- avance et que quelqu'un doit intervenir, mais aucune migration ne créait la table. La validation
-- du schéma au démarrage l'a signalé — c'est précisément son travail — et le service ne démarrait
-- plus du tout, y compris pour tout ce qui ne touche pas aux notifications.
--
-- La table conserve ce qui s'est passé ; la cloche, elle, se recalcule à chaque appel et reste la
-- source de vérité de ce qu'il y a à faire. Les deux ne se remplacent pas.
--
-- La clé d'unicité est ce qui rend la boîte tenable : le producteur fournit une clé stable, et un
-- second dépôt sur la même clé met la ligne à jour au lieu d'en empiler une de plus. Sans elle,
-- une relance quotidienne écrirait sept fois la même échéance en une semaine.
--

CREATE TABLE public.notification_utilisateur (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    destinataire_id character varying(100) NOT NULL,
    cle_unicite character varying(200) NOT NULL,
    code character varying(80) NOT NULL,
    source character varying(40) NOT NULL,
    titre character varying(200) NOT NULL,
    message character varying(1000) NOT NULL,
    resource_id character varying(100),
    resource_type character varying(40),
    lien character varying(500),
    gravite character varying(20) NOT NULL,
    lue boolean NOT NULL,
    lue_le timestamp(6) without time zone,
    CONSTRAINT notification_utilisateur_gravite_check
        CHECK (((gravite)::text = ANY
            ((ARRAY['INFO'::character varying, 'ATTENTION'::character varying,
                    'URGENT'::character varying])::text[])))
);

ALTER TABLE ONLY public.notification_utilisateur
    ADD CONSTRAINT notification_utilisateur_pkey PRIMARY KEY (id);

--
-- Deux dépôts sur la même clé chez la même personne sont le même événement : le second met la
-- ligne à jour, il n'en ajoute pas une.
--
ALTER TABLE ONLY public.notification_utilisateur
    ADD CONSTRAINT uk_notification_destinataire_cle UNIQUE (destinataire_id, cle_unicite);

-- La requête de tous les écrans : ma boîte, les non-lues d'abord.
CREATE INDEX idx_notification_destinataire_lue
    ON public.notification_utilisateur USING btree (destinataire_id, lue);

-- La reprise d'un dossier : retrouver ce qui a été dit à son sujet.
CREATE INDEX idx_notification_ressource
    ON public.notification_utilisateur USING btree (resource_id);
