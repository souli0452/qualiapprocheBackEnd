package com.qualiapproche.referentiel.service;

import com.qualiapproche.referentiel.entities.ConfigGlobal;

import java.util.UUID;

public interface configGlobalService {
    ConfigGlobal getConfigGlobal();
    ConfigGlobal createConfigGlobal(ConfigGlobal configGlobal);
    ConfigGlobal updateConfigGlobal(ConfigGlobal c,String id);
}
