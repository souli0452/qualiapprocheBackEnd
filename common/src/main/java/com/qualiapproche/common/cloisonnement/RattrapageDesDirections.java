package com.qualiapproche.common.cloisonnement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Rend chaque ligne existante à son client.
 *
 * <p>Avant le cloisonnement, {@code direction_id} recevait la <b>structure</b> de l'auteur (faute
 * d'un claim de direction dans le jeton), ou rien du tout pour ce qu'écrivait le système. Seul
 * referentiel-service connaît l'arbre des structures : il transmet la correspondance
 * structure → direction racine, et chaque service la reporte sur ses tables.</p>
 *
 * <p>Une ligne sans direction, ou d'une direction inconnue, revient à la direction par défaut
 * quand referentiel en désigne une — c'est le cas d'une installation qui n'a qu'un client : tout ce
 * qui existait avant lui appartient. Avec plusieurs clients, elle reste où elle est : l'attribuer
 * au hasard serait la montrer à qui ne doit pas la voir.</p>
 *
 * <p>Idempotent : une fois les lignes rendues, les mises à jour ne trouvent plus rien.</p>
 */
@Slf4j
public class RattrapageDesDirections {

    private final ObjectProvider<EntityManagerFactory> fabriques;
    private final ExecutionCloisonnee execution;

    public RattrapageDesDirections(ObjectProvider<EntityManagerFactory> fabriques, ExecutionCloisonnee execution) {
        this.fabriques = fabriques;
        this.execution = execution;
    }

    /**
     * @param structures chaque structure connue et la direction racine dont elle relève (une
     *                   direction y figure face à elle-même)
     * @param parDefaut  la direction qui reçoit les lignes orphelines, ou {@code null}
     * @return par table, le nombre de lignes rendues, ou l'erreur rencontrée
     */
    public Map<String, Object> rattraper(Map<UUID, UUID> structures, UUID parDefaut) {
        Map<String, Object> rapport = new LinkedHashMap<>();
        Set<UUID> directions = new TreeSet<>(structures.values());
        if (parDefaut != null) {
            directions.add(parDefaut);
        }
        for (String table : tablesCloisonnees()) {
            try {
                Integer rendues = execution.enInstallation(() -> rattraperLaTable(table, structures, directions, parDefaut));
                if (rendues != null && rendues > 0) {
                    log.info("Cloisonnement : {} ligne(s) de {} rendues à leur client.", rendues, table);
                    rapport.put(table, rendues);
                }
            } catch (Exception e) {
                log.error("Cloisonnement : rattrapage de {} impossible : {}", table, e.getMessage());
                rapport.put(table, "échec : " + e.getMessage());
            }
        }
        return rapport;
    }

    private int rattraperLaTable(String table, Map<UUID, UUID> structures, Set<UUID> directions, UUID parDefaut) {
        EntityManager em = fabriques.getObject().createEntityManager();
        try {
            em.joinTransaction();
            int rendues = 0;
            List<Map.Entry<UUID, UUID>> deplacees = structures.entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getKey().equals(e.getValue()))
                    .toList();
            if (!deplacees.isEmpty()) {
                StringBuilder valeurs = new StringBuilder();
                for (int i = 0; i < deplacees.size(); i++) {
                    valeurs.append(i == 0 ? "" : ", ").append("(cast(?").append(2 * i + 1)
                            .append(" as uuid), cast(?").append(2 * i + 2).append(" as uuid))");
                }
                Query maj = em.createNativeQuery("update " + table + " t set direction_id = m.d from (values "
                        + valeurs + ") as m(s, d) where t.direction_id = m.s");
                for (int i = 0; i < deplacees.size(); i++) {
                    maj.setParameter(2 * i + 1, deplacees.get(i).getKey().toString());
                    maj.setParameter(2 * i + 2, deplacees.get(i).getValue().toString());
                }
                rendues += maj.executeUpdate();
            }
            if (parDefaut != null) {
                List<String> connues = new ArrayList<>(directions.stream().map(UUID::toString).toList());
                rendues += em.createNativeQuery("update " + table + " set direction_id = cast(?1 as uuid)"
                                + " where direction_id is null or cast(direction_id as text) not in (?2)")
                        .setParameter(1, parDefaut.toString())
                        .setParameter(2, connues)
                        .executeUpdate();
            }
            return rendues;
        } finally {
            em.close();
        }
    }

    /** Les tables racines des entités cloisonnées : une hiérarchie n'a sa direction qu'à la racine. */
    private Set<String> tablesCloisonnees() {
        EntityManagerFactory fabrique = fabriques.getIfAvailable();
        if (fabrique == null) {
            return Set.of();
        }
        SessionFactoryImplementor sessions = fabrique.unwrap(SessionFactoryImplementor.class);
        Set<String> tables = new TreeSet<>();
        for (EntityType<?> type : fabrique.getMetamodel().getEntities()) {
            if (!Cloisonnable.class.isAssignableFrom(type.getJavaType())) {
                continue;
            }
            EntityPersister persister = sessions.getMappingMetamodel().getEntityDescriptor(type.getJavaType());
            if (persister instanceof AbstractEntityPersister abstrait) {
                tables.add(abstrait.getRootTableName());
            }
        }
        return tables;
    }
}
