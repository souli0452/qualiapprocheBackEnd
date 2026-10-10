package com.qualiapproche.common.serie;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Les deux gestes qu'exige une attribution sûre : créer une série sans course, la verrouiller. */
@NoRepositoryBean
public interface SerieRepository<E extends AbstractSerieEntity> extends JpaRepository<E, String> {

    /**
     * Crée la série si elle n'existe pas encore, sans erreur si une autre transaction vient de le
     * faire : deux premières attributions simultanées dans une portée neuve ne se heurtent pas.
     */
    @Modifying
    @Query(value = "INSERT INTO serie_numero (cle_serie, valeur_courante) VALUES (:cle, :valeur) "
            + "ON CONFLICT (cle_serie) DO NOTHING", nativeQuery = true)
    int creerSiAbsente(@Param("cle") String cle, @Param("valeur") long valeur);

    /** La série, verrouillée ({@code SELECT … FOR UPDATE}) jusqu'à la fin de la transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM #{#entityName} s WHERE s.cleSerie = :cle")
    Optional<E> verrouiller(@Param("cle") String cle);
}
