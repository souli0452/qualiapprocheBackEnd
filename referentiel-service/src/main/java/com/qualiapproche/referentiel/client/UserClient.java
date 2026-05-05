package com.qualiapproche.referentiel.client;

import com.qualiapproche.common.dto.auth.KcUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", path = "/api/v1")
public interface UserClient {

    @PostMapping("/users/create")
    ResponseEntity<KcUserDto> createUser(@RequestBody KcUserDto kcUserDto);
}
