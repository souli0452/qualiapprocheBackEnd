package com.qualiapproche.referentiel.client;

import com.qualiapproche.common.dto.DestinataireDto;
import com.qualiapproche.common.dto.auth.KcUserDto;
import com.qualiapproche.referentiel.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "user-service", path = "/api/v1", configuration = FeignConfig.class)
public interface UserClient {

    @PostMapping("/users/create") 
    KcUserDto createUser(@RequestBody KcUserDto kcUserDto);

    /**
     * Porteurs d'un rôle applicatif, avec leur adresse.
     *
     * <p>Sert à prévenir les administrateurs de l'échéance de la licence. L'enveloppe
     * {@code ApiResponse} est déballée par {@code ApiResponseFeignDecoder}.</p>
     */
    @GetMapping("/roles/{role}/users")
    List<DestinataireDto> getUsersByRole(@PathVariable("role") String role);
}
