package com.qualiapproche.support.config;

import com.qualiapproche.common.cloisonnement.Amorce;
import com.qualiapproche.support.service.QmsDocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Les types documentaires livrés, posés pour chaque client qui n'en a pas encore. */
@Component
@RequiredArgsConstructor
public class TypesDeDocumentParDefaut implements Amorce {

    private final QmsDocumentTypeService types;

    @Override
    public void amorcer() {
        types.initDefaultTypes();
    }
}
