--
-- user-service — schéma initial, relevé du schéma en service au 7 août 2026.
--
-- Comptes, rôles applicatifs et permissions.
--
-- Ce script est le POINT DE DÉPART de l'historique de migration : il décrit le schéma tel que
-- « ddl-auto: update » l'avait construit. Une base déjà en service est marquée « v1 » sans que ce
-- script y soit rejoué (baseline-on-migrate) ; une base vide le reçoit intégralement. Les deux
-- suivent ensuite la même suite de migrations, ce qui est tout l'objet de Flyway.
--
-- NE JAMAIS MODIFIER un script déjà appliqué : Flyway en vérifie l'empreinte et refuserait de
-- démarrer. Toute correction du schéma passe par un nouveau V<n+1>__....sql.
--

--
--

SET default_table_access_method = heap;

--
-- Name: app_role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_role_permissions (
    role_id uuid NOT NULL,
    permission_value character varying(255)
);

--
-- Name: app_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.app_roles (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    description character varying(255),
    name character varying(255) NOT NULL
);

--
-- Name: user_role_assignments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_role_assignments (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    current_user_full_name character varying(255),
    direction_id uuid,
    update_at timestamp(6) without time zone,
    update_by_id character varying(255),
    user_id character varying(255) NOT NULL,
    role_id uuid
);

--
-- Name: app_roles app_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_roles
    ADD CONSTRAINT app_roles_pkey PRIMARY KEY (id);

--
-- Name: user_role_assignments user_role_assignments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT user_role_assignments_pkey PRIMARY KEY (id);

--
-- Name: app_role_permissions fksj8wgtocscsk3cv3d7pngtv1s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.app_role_permissions
    ADD CONSTRAINT fksj8wgtocscsk3cv3d7pngtv1s FOREIGN KEY (role_id) REFERENCES public.app_roles(id);

--
-- Name: user_role_assignments fktjxt96o556lj9uxx9thfk4gjd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_role_assignments
    ADD CONSTRAINT fktjxt96o556lj9uxx9thfk4gjd FOREIGN KEY (role_id) REFERENCES public.app_roles(id);

--
--
