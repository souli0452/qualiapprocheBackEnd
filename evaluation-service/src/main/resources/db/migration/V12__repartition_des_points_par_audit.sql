--
-- evaluation-service — les points de contrôle se répartissent par audit.
--
-- L'auditeur d'un point était fixé une fois pour toutes sur le modèle de checklist : une équipe
-- différente ne voyait aucun point et saisissait tout « hors checklist » (recette du 17 oct. 2026,
-- observation 20). Le responsable d'équipe répartit désormais les points de son audit ; à défaut,
-- l'affectation du modèle vaut si l'auditeur est de l'équipe, sinon le point s'offre à toute l'équipe.
--
CREATE TABLE public.affectation_point_audit (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    update_at timestamp(6) without time zone,
    created_by_id character varying(255),
    update_by_id character varying(255),
    current_user_full_name character varying(255),
    current_user_email character varying(255),
    current_user_structure character varying(255),
    direction_id uuid,

    audit_id uuid NOT NULL,
    point_controle_id uuid NOT NULL,
    auditeur_id uuid NOT NULL,

    CONSTRAINT pk_affectation_point_audit PRIMARY KEY (id),
    CONSTRAINT uk_affectation_point_audit UNIQUE (audit_id, point_controle_id),
    CONSTRAINT fk_affectation_audit FOREIGN KEY (audit_id) REFERENCES public.audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_affectation_point FOREIGN KEY (point_controle_id) REFERENCES public.point_controle (id) ON DELETE CASCADE,
    CONSTRAINT fk_affectation_auditeur FOREIGN KEY (auditeur_id) REFERENCES public.auditeur (id)
);
CREATE INDEX idx_affectation_point_audit ON public.affectation_point_audit (audit_id);
