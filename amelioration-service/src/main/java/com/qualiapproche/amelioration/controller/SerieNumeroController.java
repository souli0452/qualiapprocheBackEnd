package com.qualiapproche.amelioration.controller;

import com.qualiapproche.amelioration.entities.SerieNumero;
import com.qualiapproche.amelioration.repository.SerieNumeroRepository;
import com.qualiapproche.common.annotation.RequirePermissions;
import com.qualiapproche.common.serie.AttributionDeNumero;
import com.qualiapproche.common.serie.SerieController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Les séries de numérotation des non-conformités : lecture et calage, réservés à l'administration. */
@RestController
@RequestMapping("/api/v1/series-numerotation")
@RequirePermissions(
        read = {"CONFIG_READ", "CONFIG_GLOBAL_MANAGE", "config-global-read", "config-global-write"},
        update = {"CONFIG_GLOBAL_MANAGE", "config-global-write"}
)
public class SerieNumeroController extends SerieController<SerieNumero> {

    private final AttributionDeNumero<SerieNumero> attribution;

    public SerieNumeroController(SerieNumeroRepository series) {
        this.attribution = new AttributionDeNumero<>(series);
    }

    @Override
    protected AttributionDeNumero<SerieNumero> attribution() {
        return attribution;
    }

    @Override
    protected SerieNumero nouvelle() {
        return new SerieNumero();
    }
}
