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
