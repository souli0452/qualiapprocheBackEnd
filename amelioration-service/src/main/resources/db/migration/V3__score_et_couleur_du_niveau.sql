--
-- amelioration-service — le niveau de non-conformité porte son poids et sa couleur.
--
-- Le niveau n'avait qu'un libellé et une description. Deux conséquences, l'une et l'autre visibles
-- à l'écran. Trier des non-conformités par gravité retombait sur l'ordre alphabétique du libellé,
-- qui range « Critique » avant « Majeure » et « Majeure » avant « Mineure » : l'inverse de ce que
-- l'oeil attend une fois sur deux. Et l'étiquette de gravité était peinte côté client, d'après une
-- correspondance écrite en dur entre un libellé et une couleur — une organisation qui nommait ses
-- niveaux autrement les voyait tous de la même teinte.
--
-- Le score porte l'échelle, la couleur porte l'affichage. Les deux appartiennent à l'organisation,
-- qui crée ses propres niveaux : ni l'étendue de l'échelle ni la palette ne sont imposées ici.
--
-- Les deux colonnes sont facultatives. Les niveaux déjà saisis restent exploitables sans score ni
-- couleur, et c'est le paramétrage qui les renseignera ; un niveau sans score se range simplement
-- après ceux qui en ont un.
--

ALTER TABLE public.niveau_non_conformite
    ADD COLUMN IF NOT EXISTS score integer;

ALTER TABLE public.niveau_non_conformite
    ADD COLUMN IF NOT EXISTS couleur character varying(20);

COMMENT ON COLUMN public.niveau_non_conformite.score IS
    'Poids du niveau, du moins grave au plus grave. Sert au tri et a la comparaison.';

COMMENT ON COLUMN public.niveau_non_conformite.couleur IS
    'Couleur d''affichage du niveau, au format hexadecimal (ex. #f59e0b).';
