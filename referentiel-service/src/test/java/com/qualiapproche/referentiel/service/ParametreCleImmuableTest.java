package com.qualiapproche.referentiel.service;

import com.qualiapproche.common.dto.ParametreDto;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.referentiel.entities.Parametre;
import com.qualiapproche.referentiel.entities.TypeParametre;
import com.qualiapproche.referentiel.repository.ParametreRepository;
import com.qualiapproche.referentiel.service.impl.ParametreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.assertj.core.api.Assertions;

/**
 * Réglages de l'organisation : la clé fait l'identité, et elle ne bouge pas.
 *
 * <p>C'est par la clé que le code désigne un réglage — le pied de page des courriels demande
 * {@code CONTACT_EMAIL}. La renommer romprait en silence tout ce qui la lit : l'ancien nom ne
 * rendrait plus rien, et personne ne verrait de message d'erreur. D'où un refus explicite, et une
 * normalisation à la création pour que trois orthographes ne fassent pas trois réglages.</p>
 */
class ParametreCleImmuableTest {

    private static final UUID ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    private ParametreRepository repository;
    private ParametreServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ParametreRepository.class);
        service = new ParametreServiceImpl(repository);
        when(repository.save(any(Parametre.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Parametre existant(String cle, String valeur) {
        Parametre parametre = Parametre.builder()
                .cle(cle).valeur(valeur).libelle("Courriel de contact")
                .type(TypeParametre.COURRIEL).lisibleSansHabilitation(true).build();
        parametre.setId(ID);
        return parametre;
    }

    private ParametreDto propose(String cle, String valeur) {
        return ParametreDto.builder()
                .cle(cle).valeur(valeur).libelle("Courriel de contact").type("COURRIEL").build();
    }

    // ------------------------------------------------------------------ création

    @Test
    @DisplayName("La clé est normalisée : trois orthographes ne font pas trois réglages")
    void cle_normalisee() {
        assertThat(service.create(propose("  contact email ", "qualite@exemple.fr")).getCle())
                .isEqualTo("CONTACT_EMAIL");
        assertThat(service.create(propose("Contact-Email", "qualite@exemple.fr")).getCle())
                .isEqualTo("CONTACT_EMAIL");
        assertThat(service.create(propose("créateur du dossier", "x")).getCle())
                .isEqualTo("CREATEUR_DU_DOSSIER");
    }

    @Test
    @DisplayName("Une clé déjà prise est refusée : le code ne saurait pas lequel des deux lire")
    void cleDejaPrise_refusee() {
        when(repository.existsByCle("CONTACT_EMAIL")).thenReturn(true);

        assertThatThrownBy(() -> service.create(propose("contact_email", "autre@exemple.fr")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un intitulé est exigé : la clé est technique, c'est l'intitulé que lit l'administrateur")
    void libelleObligatoire() {
        ParametreDto sansLibelle = ParametreDto.builder().cle("CONTACT_EMAIL").libelle("  ").build();

        assertThatThrownBy(() -> service.create(sansLibelle))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("intitulé");
    }

    @Test
    @DisplayName("Une clé vide est refusée plutôt que persistée")
    void cleVide_refusee() {
        assertThatThrownBy(() -> service.create(propose("   ", "x")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ modification

    @Test
    @DisplayName("Renommer la clé est refusé en 409, et non ignoré en silence")
    void renommerLaCle_refuse() {
        when(repository.findById(ID)).thenReturn(Optional.of(existant("CONTACT_EMAIL", "qualite@exemple.fr")));

        // Ignorer la clé soumise aurait laissé croire au renommage : l'utilisateur aurait cherché
        // longtemps pourquoi le pied de page de ses courriels ne changeait pas.
        assertThatThrownBy(() -> service.update(ID, propose("COURRIEL_CONTACT", "qualite@exemple.fr")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ne peut pas être renommée")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("La valeur se modifie, la clé reste")
    void valeur_modifiable() {
        Parametre parametre = existant("CONTACT_EMAIL", "ancien@exemple.fr");
        when(repository.findById(ID)).thenReturn(Optional.of(parametre));

        ParametreDto modifie = service.update(ID, propose("CONTACT_EMAIL", "nouveau@exemple.fr"));

        assertThat(modifie.getValeur()).isEqualTo("nouveau@exemple.fr");
        assertThat(modifie.getCle()).isEqualTo("CONTACT_EMAIL");
    }

    @Test
    @DisplayName("Une clé omise à la modification n'est pas prise pour un renommage")
    void cleOmise_toleree() {
        when(repository.findById(ID)).thenReturn(Optional.of(existant("CONTACT_EMAIL", "ancien@exemple.fr")));

        // L'écran peut n'envoyer que ce qui est modifiable : l'absence de clé n'est pas une demande
        // de renommage.
        assertThat(service.update(ID, propose(null, "nouveau@exemple.fr")).getCle())
                .isEqualTo("CONTACT_EMAIL");
    }

    @Test
    @DisplayName("La même clé écrite autrement n'est pas un renommage")
    void memeCleAutrementEcrite_acceptee() {
        when(repository.findById(ID)).thenReturn(Optional.of(existant("CONTACT_EMAIL", "ancien@exemple.fr")));

        assertThat(service.update(ID, propose(" contact email ", "nouveau@exemple.fr")).getValeur())
                .isEqualTo("nouveau@exemple.fr");
    }

    // ------------------------------------------------------------------ valeurs publiques

    @Test
    @DisplayName("Les valeurs publiques ne rendent que ce qui est renseigné")
    void valeursPubliques_ignorentLesReglagesVides() {
        Parametre courriel = existant("CONTACT_EMAIL", "qualite@exemple.fr");
        Parametre telephone = existant("CONTACT_TELEPHONE", "   ");
        Parametre logo = existant("LOGO_URL", null);
        when(repository.findByLisibleSansHabilitationTrue())
                .thenReturn(List.of(courriel, telephone, logo));

        // Un réglage vide transmis obligerait chaque consommateur à refaire ce tri, et un pied de
        // page afficherait « Téléphone : » suivi de rien.
        assertThat(service.valeursPubliques()).containsExactly(
                Assertions.entry("CONTACT_EMAIL", "qualite@exemple.fr"));
    }

    @Test
    @DisplayName("Une nature de réglage inconnue est refusée, en énonçant les valeurs admises")
    void typeInconnu_refuse() {
        ParametreDto dto = ParametreDto.builder()
                .cle("CONTACT_EMAIL").libelle("Courriel").type("IMAGE_ANIMEE").build();

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TEXTE");
    }

    // ------------------------------------------------------------------ réglages numériques

    @Test
    @DisplayName("Un réglage numérique refuse ce qui n'est pas un nombre, à la saisie")
    void reglageNumerique_valeurNonNumeriqueRefusee() {
        ParametreDto dto = ParametreDto.builder()
                .cle("RAPPEL_ECHEANCE_JOURS").libelle("Rappel").type("NOMBRE").valeur("deux jours").build();

        // Sans ce refus, l'erreur n'apparaîtrait qu'au moment du rappel, sur un fil de fond, loin de
        // qui a saisi la valeur.
        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nombre entier")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un réglage numérique non renseigné reste admis : il est simplement ignoré")
    void reglageNumerique_valeurVideAdmise() {
        ParametreDto dto = ParametreDto.builder()
                .cle("RAPPEL_ECHEANCE_JOURS").libelle("Rappel").type("NOMBRE").build();

        assertThat(service.create(dto).getValeur()).isNull();
    }

    @Test
    @DisplayName("La nature omise à la modification est conservée : la valeur reste vérifiée comme un nombre")
    void reglageNumerique_natureConservee() {
        Parametre existant = Parametre.builder()
                .cle("RAPPEL_ECHEANCE_JOURS").libelle("Rappel").type(TypeParametre.NOMBRE).build();
        existant.setId(ID);
        when(repository.findById(ID)).thenReturn(Optional.of(existant));

        ParametreDto sansNature = ParametreDto.builder()
                .cle("RAPPEL_ECHEANCE_JOURS").libelle("Rappel").valeur("bientôt").build();

        assertThatThrownBy(() -> service.update(ID, sansNature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nombre entier");
    }

    @Test
    @DisplayName("Aucune nature précisée vaut « texte », plutôt qu'un refus")
    void typeAbsent_vautTexte() {
        ParametreDto dto = ParametreDto.builder().cle("MENTION_LEGALE").libelle("Mention").build();

        assertThat(service.create(dto).getType()).isEqualTo("TEXTE");
    }
}
