package com.qualiapproche.common.courriel;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * La file des courriels d'un service, conservée en base (patron « transactional outbox »).
 *
 * <p>Les courriels partaient d'une file en mémoire, ou directement depuis la requête : un
 * redémarrage perdait la file, une panne du serveur de messagerie perdait le courriel, et rien n'en
 * gardait trace. Désormais :</p>
 * <ul>
 *   <li>{@link #deposer} inscrit le courriel, déjà rendu, dans la table {@code courriel_sortant}.
 *       Appelé dans la transaction métier, il n'existe que si elle est validée : une décision
 *       annulée n'annonce rien ;</li>
 *   <li>{@link #tournee} — appelée par une tâche planifiée, sous verrou ShedLock — remet les
 *       courriels échus au serveur, un par un ; un échec reporte le courriel (de 30 s jusqu'à 1 h),
 *       et après {@value #TENTATIVES_MAX} tentatives il passe ABANDONNE, à examiner ;</li>
 *   <li>chaque courriel est d'abord <b>revendiqué</b> en une instruction : une tournée interrompue
 *       en plein envoi le rend reprenable cinq minutes plus tard, sans doublon.</li>
 * </ul>
 *
 * <p>Repris du registre de notifications de workflow-service, qui fonctionnait ainsi déjà.</p>
 *
 * @param <E> l'entité de courriel du service
 */
@Slf4j
public class FileDeCourriels<E extends AbstractCourrielSortant> {

    public static final int TENTATIVES_MAX = 8;
    private static final long REPORT_INITIAL_SECONDES = 30;
    private static final long REPORT_MAX_SECONDES = 3600;
    /** Au-delà, une tournée revendiquée et muette est tenue pour interrompue. */
    private static final Duration DELAI_DE_REPRISE = Duration.ofMinutes(5);
    private static final int TAILLE_MAX_ERREUR = 2000;

    private final CourrielSortantRepository<E> depot;
    private final Supplier<E> fabrique;
    private final ObjectProvider<JavaMailSender> messagerie;
    private final String expediteur;
    private final TransactionTemplate transactionPropre;
    private final Clock horloge;

    public FileDeCourriels(CourrielSortantRepository<E> depot, Supplier<E> fabrique,
                           ObjectProvider<JavaMailSender> messagerie, String expediteur,
                           PlatformTransactionManager transactions) {
        this(depot, fabrique, messagerie, expediteur, transactions, Clock.systemDefaultZone());
    }

    FileDeCourriels(CourrielSortantRepository<E> depot, Supplier<E> fabrique,
                    ObjectProvider<JavaMailSender> messagerie, String expediteur,
                    PlatformTransactionManager transactions, Clock horloge) {
        this.depot = depot;
        this.fabrique = fabrique;
        this.messagerie = messagerie;
        this.expediteur = expediteur;
        this.transactionPropre = new TransactionTemplate(transactions);
        this.transactionPropre.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.horloge = horloge;
    }

    /**
     * Inscrit un courriel à envoyer. Dans une transaction, il n'existera que si elle est validée.
     *
     * @return l'identifiant du courriel, ou {@code null} s'il n'y avait aucun destinataire
     */
    public UUID deposer(String destinataire, String copie, String sujet, String corpsHtml, String origine) {
        if (destinataire == null || destinataire.isBlank()) {
            log.warn("Courriel « {} » ({}) non déposé : aucun destinataire.", sujet, origine);
            return null;
        }
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        E courriel = fabrique.get();
        courriel.setId(UUID.randomUUID());
        courriel.setDestinataire(destinataire.trim());
        courriel.setCopie(copie == null || copie.isBlank() ? null : copie.trim());
        courriel.setSujet(sujet == null ? "" : sujet);
        courriel.setCorps(corpsHtml == null ? "" : corpsHtml);
        courriel.setOrigine(origine);
        courriel.setStatut(StatutCourriel.A_ENVOYER);
        courriel.setTentatives(0);
        courriel.setProchaineTentativeAt(maintenant);
        courriel.setCreeLe(maintenant);
        depot.save(courriel);
        return courriel.getId();
    }

    /**
     * Remet au serveur les courriels échus, au plus {@code lot}.
     *
     * @return le nombre de courriels remis
     */
    public int tournee(int lot) {
        JavaMailSender serveur = messagerie.getIfAvailable();
        if (serveur == null) {
            // Rien n'est consommé : les courriels attendent qu'une messagerie soit configurée.
            long enAttente = depot.countByStatut(StatutCourriel.A_ENVOYER);
            if (enAttente > 0) {
                log.warn("{} courriel(s) en attente : aucune messagerie configurée (spring.mail.host).", enAttente);
            }
            return 0;
        }
        int remis = 0;
        for (UUID id : depot.echus(LocalDateTime.now(horloge), lot)) {
            if (envoyer(id, serveur)) {
                remis++;
            }
        }
        return remis;
    }

    /** Efface les courriels remis depuis plus de {@code conservation}. */
    public int purger(Duration conservation) {
        return transactionPropre.execute(s ->
                depot.purgerLesEnvoyes(StatutCourriel.ENVOYE, LocalDateTime.now(horloge).minus(conservation)));
    }

    private boolean envoyer(UUID id, JavaMailSender serveur) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        E courriel = transactionPropre.execute(s -> {
            int acquis = depot.revendiquer(id, StatutCourriel.EN_COURS,
                    EnumSet.of(StatutCourriel.A_ENVOYER, StatutCourriel.EN_COURS),
                    maintenant, maintenant.plus(DELAI_DE_REPRISE));
            return acquis == 1 ? depot.findById(id).orElse(null) : null;
        });
        if (courriel == null) {
            return false;
        }
        try {
            // Hors transaction : un serveur lent ne retient ni connexion ni verrou en base.
            serveur.send(composer(serveur, courriel));
            transactionPropre.executeWithoutResult(s -> depot.findById(id).ifPresent(c -> {
                c.setStatut(StatutCourriel.ENVOYE);
                c.setEnvoyeLe(LocalDateTime.now(horloge));
                c.setDerniereErreur(null);
                depot.save(c);
            }));
            log.info("Courriel « {} » ({}) remis pour {}.", courriel.getSujet(), courriel.getOrigine(),
                    courriel.getDestinataire());
            return true;
        } catch (Exception e) {
            reporter(id, e);
            return false;
        }
    }

    private MimeMessage composer(JavaMailSender serveur, E courriel) throws jakarta.mail.MessagingException {
        MimeMessage mime = serveur.createMimeMessage();
        MimeMessageHelper aide = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
        if (expediteur != null && !expediteur.isBlank()) {
            aide.setFrom(expediteur);
        }
        aide.setTo(courriel.getDestinataire());
        if (courriel.getCopie() != null) {
            aide.setCc(courriel.getCopie());
        }
        aide.setSubject(courriel.getSujet());
        aide.setText(courriel.getCorps(), true);
        return mime;
    }

    private void reporter(UUID id, Exception cause) {
        String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        transactionPropre.executeWithoutResult(s -> depot.findById(id).ifPresent(c -> {
            c.setDerniereErreur(message.length() > TAILLE_MAX_ERREUR ? message.substring(0, TAILLE_MAX_ERREUR) : message);
            if (c.getTentatives() >= TENTATIVES_MAX) {
                c.setStatut(StatutCourriel.ABANDONNE);
                log.error("Courriel « {} » ({}) pour {} abandonné après {} tentatives : {}", c.getSujet(),
                        c.getOrigine(), c.getDestinataire(), c.getTentatives(), message);
            } else {
                long report = Math.min(REPORT_INITIAL_SECONDES * (1L << (c.getTentatives() - 1)), REPORT_MAX_SECONDES);
                c.setStatut(StatutCourriel.A_ENVOYER);
                c.setProchaineTentativeAt(LocalDateTime.now(horloge).plusSeconds(report));
                log.warn("Courriel « {} » ({}) pour {} : tentative {}/{} échouée, nouvel essai dans {} s : {}",
                        c.getSujet(), c.getOrigine(), c.getDestinataire(), c.getTentatives(), TENTATIVES_MAX,
                        report, message);
            }
            depot.save(c);
        }));
    }

    /** Pour un tableau de bord ou une sonde : ce qui attend, et ce qui a été abandonné. */
    public List<Long> etat() {
        return List.of(depot.countByStatut(StatutCourriel.A_ENVOYER), depot.countByStatut(StatutCourriel.ABANDONNE));
    }
}
