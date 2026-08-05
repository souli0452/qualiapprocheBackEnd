package com.qualiapproche.referentiel.service;

import com.qualiapproche.referentiel.entities.ConfigGlobal;


public interface ConfigGlobalService {
    ConfigGlobal getConfigGlobal();
    ConfigGlobal createConfigGlobal(ConfigGlobal configGlobal);
    ConfigGlobal updateConfigGlobal(ConfigGlobal c, String id);
}
