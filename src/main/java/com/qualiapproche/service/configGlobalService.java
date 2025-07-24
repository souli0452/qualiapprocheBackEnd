package com.qualiapproche.service;

import com.qualiapproche.entities.ConfigGlobal;

import java.util.UUID;

public interface configGlobalService {
    ConfigGlobal getConfigGlobal();
    ConfigGlobal createConfigGlobal(ConfigGlobal configGlobal);
    ConfigGlobal updateConfigGlobal(ConfigGlobal c,UUID id);
}
