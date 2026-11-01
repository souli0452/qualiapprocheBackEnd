-- Une installation sert désormais plusieurs clients : chaque client est une direction racine, et
-- chaque ligne porte dans direction_id la direction à laquelle elle appartient (Cloisonnement).

-- 1. Les structures : direction_id = la direction racine de l'arbre. Seule une DIRECTION sans
--    parent est une racine — un client. Jusqu'ici la colonne recevait la structure de l'auteur,
--    quand elle recevait quelque chose.
WITH RECURSIVE arbre(id, racine) AS (
    SELECT s.id, s.id
    FROM public.structures s
    WHERE s.parent_direction_id IS NULL AND s.type_structure = 'DIRECTION'
    UNION ALL
    SELECT enfant.id, arbre.racine
    FROM public.structures enfant
    JOIN arbre ON enfant.parent_direction_id = arbre.id
)
UPDATE public.structures s
SET direction_id = arbre.racine
FROM arbre
WHERE s.id = arbre.id
  AND s.direction_id IS DISTINCT FROM arbre.racine;

--    Une structure sans parent qui n'est pas une direction (un service saisi sans rattachement)
--    n'est pas un client : elle revient à la direction quand l'installation n'en a qu'une. Avec
--    plusieurs, elle reste sans client — à rattacher depuis l'écran, puis à rendre par le
--    rattrapage ; l'attribuer au hasard la montrerait à qui ne doit pas la voir.
WITH RECURSIVE orphelines(id) AS (
    SELECT s.id FROM public.structures s
    WHERE s.parent_direction_id IS NULL AND s.type_structure <> 'DIRECTION'
    UNION ALL
    SELECT enfant.id FROM public.structures enfant JOIN orphelines o ON enfant.parent_direction_id = o.id
),
unique_direction AS (
    SELECT min(id::text)::uuid AS id FROM public.structures
    WHERE parent_direction_id IS NULL AND type_structure = 'DIRECTION'
    HAVING count(*) = 1
)
UPDATE public.structures s
SET direction_id = (SELECT id FROM unique_direction)
FROM orphelines o
WHERE s.id = o.id
  AND EXISTS (SELECT 1 FROM unique_direction);

-- 2. La licence est celle d'un client : chacun la sienne. Les licences existantes sont rendues à
--    leur client par le rattrapage du démarrage (RattrapageDesDirections).
ALTER TABLE public.licences_installees ADD COLUMN IF NOT EXISTS direction_id uuid;

-- 3. Les messages emportent le client pour lequel ils ont été déposés.
ALTER TABLE public.message_sortant ADD COLUMN IF NOT EXISTS direction_id uuid;

-- 4. Le paramétrage est propre à chaque client : ses libellés et ses clés ne sont uniques que chez lui.
ALTER TABLE public.domaines_application DROP CONSTRAINT IF EXISTS uk_domaine_libelle;
ALTER TABLE public.domaines_application ADD CONSTRAINT uk_domaine_libelle UNIQUE (direction_id, libelle);

ALTER TABLE public.niveaux_confidentialite DROP CONSTRAINT IF EXISTS uk_niveau_confidentialite_libelle;
ALTER TABLE public.niveaux_confidentialite ADD CONSTRAINT uk_niveau_confidentialite_libelle UNIQUE (direction_id, libelle);

ALTER TABLE public.parametres DROP CONSTRAINT IF EXISTS uk_parametre_cle;
ALTER TABLE public.parametres ADD CONSTRAINT uk_parametre_cle UNIQUE (direction_id, cle);

ALTER TABLE public.priorites_document DROP CONSTRAINT IF EXISTS uk_priorite_libelle;
ALTER TABLE public.priorites_document ADD CONSTRAINT uk_priorite_libelle UNIQUE (direction_id, libelle);

-- 5. Chaque lecture est filtrée sur la direction.
CREATE INDEX IF NOT EXISTS idx_structures_direction ON public.structures (direction_id);
CREATE INDEX IF NOT EXISTS idx_licences_installees_direction ON public.licences_installees (direction_id);
CREATE INDEX IF NOT EXISTS idx_parametre_direction ON public.parametres (direction_id);
