package com.qualiapproche.support.numerotation;

import com.qualiapproche.common.cloisonnement.Cloisonnement;
import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.common.serie.AttributionDeNumero;
import com.qualiapproche.common.serie.SerieRepository;
import com.qualiapproche.support.model.SerieNumero;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.function.Function;

/**
 * Les séries de numérotation du client de la session.
 *
 * <p>La table des séries n'est pas cloisonnée par Hibernate (sa clé est le nom de la série) : la
 * direction est donc inscrite dans la clé — {@code <direction>|DOC|PRO-DQ-2026-} — et la liste
 * comme le calage s'en tiennent aux clés du client. Les séries d'avant le cloisonnement, sans
 * direction, ne servent plus : la première attribution d'un client reprend sur le plus grand
 * numéro de ses propres documents.</p>
 */
public class SeriesDuClient extends AttributionDeNumero<SerieNumero> {

    public SeriesDuClient(SerieRepository<SerieNumero> series) {
        super(series);
    }

    /** La clé d'une série pour le client de la session. */
    public static String cle(String serie) {
        return prefixeDuClient() + serie;
    }

    @Override
    public List<SerieNumero> lister() {
        String prefixe = prefixeDuClient();
        return super.lister().stream().filter(s -> s.getCleSerie().startsWith(prefixe)).toList();
    }

    @Override
    public SerieNumero caler(String cle, long valeur, Function<String, SerieNumero> fabrique) {
        if (cle == null || !cle.startsWith(prefixeDuClient())) {
            throw new BusinessException("Aucune série ne porte cette clé : " + cle, HttpStatus.NOT_FOUND);
        }
        return super.caler(cle, valeur, fabrique);
    }

    private static String prefixeDuClient() {
        return Cloisonnement.courante() + "|";
    }
}
