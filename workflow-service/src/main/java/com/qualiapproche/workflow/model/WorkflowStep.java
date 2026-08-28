package com.qualiapproche.workflow.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "workflow_step",
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_step_code",
                columnNames = {"workflow_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnoreProperties("steps")
    private Workflow workflow;

    /**
     * Identifiant fonctionnel de l'étape, propre au circuit et <b>immuable</b>.
     *
     * <p>C'est la clé stable sur laquelle s'appuient le rattachement des transitions et le
     * rapprochement des étapes lors d'une modification. Le nom de l'étape est un libellé
     * d'affichage : le prendre pour identité rompait toutes les destinations dès qu'on le
     * corrigeait.</p>
     *
     * <p>Obligatoire et unique au sein d'un circuit, garanti par la base autant que par
     * {@code WorkflowService}. L'unicité est portée par le couple (circuit, code) et non par le
     * code seul : deux circuits distincts peuvent légitimement comporter chacun une étape
     * {@code VALIDATION}.</p>
     */
    @Column(name = "code", nullable = false)
    private String code;

    @Column(nullable = false)
    private String nomEtape;

    @Column(nullable = false)
    private int stepOrder;

    private String responsableRole;
    private String description;

    @Column(name = "etat_traitement")
    private String etatTraitement;

    @Column(name = "email_template_code")
    private String emailTemplateCode;

    /**
     * Destinataire du courriel d'étape, quand ce n'est pas celui qui doit y agir.
     *
     * <p>Écrit sous la forme {@code RÔLE@PORTÉE} — voir {@link DestinataireCourriel}. Vide, le
     * courriel part vers les porteurs du rôle responsable de l'étape, dans la structure du
     * dossier ; c'est le cas de presque toutes. La clôture d'une non-conformité fait exception :
     * elle s'annonce au pilote du processus qui a signalé l'écart, lequel n'est ni le rôle de
     * cette étape ni, à ce stade, la structure du dossier.</p>
     *
     * <p>Accepte aussi les deux désignations <b>personnelles</b> — {@code @CREATEUR},
     * {@code @TITULAIRE} — pour une étape qui doit s'annoncer à quelqu'un sans pour autant lui
     * être réservée : la rédaction d'un document reste ouverte à tout agent du processus, mais
     * c'est à son auteur qu'on écrit quand le document lui revient.</p>
     */
    @Column(name = "destinataire_courriel", length = 80)
    private String destinataireCourriel;

    /**
     * Champ de cette étape dont la valeur désigne la personne à qui le dossier est confié.
     *
     * <p>L'imputation d'une non-conformité y inscrit {@code userImputId} : la décision prise à
     * cette étape ne fait pas qu'avancer le dossier, elle en nomme le titulaire. Les étapes
     * suivantes peuvent alors se réserver à cette personne plutôt qu'à un rôle.</p>
     */
    @Column(name = "champ_titulaire")
    private String champTitulaire;

    /**
     * Personnes qui co-signent cette étape, séparées par des virgules, ou vide.
     *
     * <p>Le rôle responsable dit qui <b>peut</b> décider ; celle-ci nomme qui, parmi eux, engage
     * effectivement sa signature ici. Elle n'ouvre ni ne ferme l'étape à personne : elle porte une
     * seule règle, la <b>séparation des signatures</b> — celui de ces signataires qui a soumis le
     * dossier ne le décide pas à cette étape. Le pilote qui rédige un document ne le vérifie donc
     * pas lui-même, tandis que ses co-signataires le vérifient comme avant.</p>
     *
     * <p>Des <b>personnes</b> et non des rôles : le rôle est déjà dit par {@link #responsableRole},
     * et le redire n'apprendrait rien. La séparation, elle, porte sur une identité — celui-là, sur
     * ce dossier-là — et deux porteurs d'un même rôle ne sont pas interchangeables au regard d'une
     * signature.</p>
     *
     * <p>Vide — le cas de toutes les étapes livrées —, la règle est inactive et l'étape se décide
     * comme avant, à l'habilitation seule. C'est une décision d'organisation, pas une règle du
     * moteur : une structure où une seule personne peut signer ne peut pas se l'offrir sans
     * immobiliser ses dossiers, et c'est à elle d'en juger.</p>
     *
     * <p>Identifiants d'utilisateur, tels que user-service les connaît. Voir {@link Cosignataires}
     * pour la lecture, l'écriture et la comparaison de la liste.</p>
     */
    @Column(name = "cosignataires", length = 1000)
    private String cosignataires;

    /**
     * Modèle d'étape du catalogue ayant servi à pré-remplir cette étape.
     *
     * <p>Simple référence, sans association JPA : le catalogue est administré par support-service,
     * dans une autre base. L'association {@code @ManyToOne} précédente pointait vers une table
     * homonyme propre à ce service, que rien n'alimentait — l'identifiant choisi par l'utilisateur
     * n'était donc jamais conservé.</p>
     */
    @Column(name = "step_template_id")
    private UUID stepTemplateId;

    @OneToMany(mappedBy = "fromStep", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("fromStep")
    private List<WorkflowTransition> transitions = new ArrayList<>();

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("step")
    private List<WorkflowStepField> fields = new ArrayList<>();
}
