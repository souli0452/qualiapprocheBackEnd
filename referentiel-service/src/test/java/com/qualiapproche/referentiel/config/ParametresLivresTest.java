package com.qualiapproche.referentiel.config;

import com.qualiapproche.referentiel.entities.Parametre;
import com.qualiapproche.referentiel.repository.ParametreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Réglages semés au démarrage.
 *
 * <p>Ce sont les <b>clés</b> qui sont semées, et elles seules : ce sont elles que le code cite, et
 * l'administrateur ne peut pas les deviner. Il les trouve donc dans son écran, avec leur intitulé, et
 * n'a qu'à les renseigner — le responsable qualité compris, que le dialogue de lancement lui réclame.
 * </p>
 *
 * <p>Aucune valeur n'est inventée : un téléphone ou une adresse posés d'office figureraient faux au
 * bas de courriels envoyés à l'extérieur.</p>
 */
class ParametresLivresTest {

    private ParametreRepository repository;
    private ParametresLivres livres;

    @BeforeEach
    void setUp() {
        repository = mock(ParametreRepository.class);
        livres = new ParametresLivres(repository);
        when(repository.existsByCle(anyString())).thenReturn(false);
        when(repository.save(any(Parametre.class))).thenAnswer(i -> i.getArgument(0));
    }

    private List<Parametre> semes() {
        org.mockito.ArgumentCaptor<Parametre> aSeme =
                org.mockito.ArgumentCaptor.forClass(Parametre.class);
        verify(repository, org.mockito.Mockito.atLeastOnce()).save(aSeme.capture());
        return aSeme.getAllValues();
    }

    @Test
    @DisplayName("Les clés que le code lit sont toutes semées")
    void toutesLesClesSemees() {
        livres.semer();

        assertThat(semes()).extracting(Parametre::getCle).contains(
                "ORGANISATION_NOM", "CONTACT_EMAIL", "CONTACT_TELEPHONE", "ADRESSE_POSTALE",
                "SITE_WEB", "LOGO_URL", "RESPONSABLE_QUALITE_NOM", "RESPONSABLE_QUALITE_EMAIL",
                "RAPPEL_ECHEANCE_JOURS");
    }

    @Test
    @DisplayName("Aucune valeur n'est inventée : les réglages sont semés vides")
    void reglages_semesVides() {
        livres.semer();

        assertThat(semes()).allSatisfy(parametre -> assertThat(parametre.getValeur()).isNull());
    }

    @Test
    @DisplayName("Tout réglage porte un intitulé : la clé est technique, c'est l'intitulé qui se lit")
    void reglages_tousIntitules() {
        livres.semer();

        assertThat(semes()).allSatisfy(parametre ->
                assertThat(parametre.getLibelle()).isNotBlank());
    }

    @Test
    @DisplayName("Un réglage déjà présent n'est pas touché : un redémarrage n'écrase pas une saisie")
    void reglageExistant_intact() {
        when(repository.existsByCle(anyString())).thenReturn(true);

        livres.semer();

        verify(repository, never()).save(any());
    }
}
