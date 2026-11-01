package com.qualiapproche.common.cloisonnement;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Exécute un geste pour le compte d'une direction, dans une transaction à lui.
 *
 * <p>La direction d'une session Hibernate se fixe à son ouverture ; dans une requête HTTP, la
 * session est ouverte dès l'entrée (open-in-view) avec la direction de l'appelant. Imposer une
 * autre direction ({@link Cloisonnement#pour}) ne vaut donc que pour une session neuve : la
 * transaction {@code REQUIRES_NEW} suspend celle de la requête et en ouvre une autre, sous la
 * direction voulue.</p>
 */
public class ExecutionCloisonnee {

    private final Supplier<PlatformTransactionManager> transactions;
    private volatile TransactionTemplate transactionPropre;

    /** Résolu à l'usage : voir CloisonnementAutoConfiguration. */
    public ExecutionCloisonnee(ObjectProvider<PlatformTransactionManager> transactions) {
        this.transactions = transactions::getObject;
    }

    /** Un gestionnaire donné (tests). */
    public ExecutionCloisonnee(PlatformTransactionManager transactions) {
        this.transactions = () -> transactions;
    }

    private TransactionTemplate transaction() {
        if (transactionPropre == null) {
            TransactionTemplate modele = new TransactionTemplate(transactions.get());
            modele.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionPropre = modele;
        }
        return transactionPropre;
    }

    public <T> T pour(UUID direction, Supplier<T> geste) {
        return Cloisonnement.pour(direction, () -> transaction().execute(statut -> geste.get()));
    }

    public void pour(UUID direction, Runnable geste) {
        pour(direction, () -> {
            geste.run();
            return null;
        });
    }

    /** Pour le système : toutes les directions sont visibles, rien n'est écrit sans en nommer une. */
    public <T> T enInstallation(Supplier<T> geste) {
        return pour(Cloisonnement.INSTALLATION, geste);
    }
}
