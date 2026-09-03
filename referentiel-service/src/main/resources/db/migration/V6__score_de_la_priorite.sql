--
-- referentiel-service — le rang de la priorité devient son score.
--
-- La colonne s'appelait « ordre ». Le nom disait où la priorité se plaçait dans une liste, non ce
-- qu'elle pesait, et deux notions s'y confondaient : le rang d'affichage et l'échelle d'urgence.
-- La gravité d'une non-conformité, qui répond au même besoin, porte désormais un « score ». Les
-- deux paramétrages se lisent maintenant de la même façon, ce qui évite d'avoir à se rappeler
-- lequel des deux mots vaut pour quelle table.
--
-- Renommage et non ajout : il n'y a qu'une seule notion, et laisser cohabiter « ordre » et
-- « score » aurait obligé chaque écran, chaque tri et chaque relecteur à trancher lequel fait foi.
-- Les valeurs déjà saisies sont conservées telles quelles.
--
-- Le tri du service suit ce nom : les priorités sans score se rangent après celles qui en ont un.
--

ALTER TABLE public.priorites_document
    RENAME COLUMN ordre TO score;

COMMENT ON COLUMN public.priorites_document.score IS
    'Poids de la priorite, du plus urgent au moins urgent. Sert au tri et a la comparaison.';
