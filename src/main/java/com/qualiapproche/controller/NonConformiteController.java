package com.qualiapproche.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.qualiapproche.dto.FichierDto;
import com.qualiapproche.enumeration.Etat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.qualiapproche.dto.NonConformiteDto;
import com.qualiapproche.service.NonConformiteService;

import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(NON_CONFORMITE_ROOT_URL)
@RequiredArgsConstructor
public class NonConformiteController {
    private final   NonConformiteService nonConformiteService;


    /**
     * Endpoint pour créer une non-conformité
     * @param dto Objet contenant les informations de la non-conformité
     * @return NonConformiteDto contenant les informations de la non-conformité créée
     */
    @PostMapping(CREATE_NON_CONFORMITE_RPOCESSUS)
    public ResponseEntity<NonConformiteDto> createNonConformite(@RequestBody NonConformiteDto dto) throws IOException {
        NonConformiteDto createdNonConformite = nonConformiteService.createNonConformite(dto);
        return ResponseEntity.ok(createdNonConformite);
    }

    /*-----------------------------------------------------------------------/
    /               Méthode de modification d'une non conformité             /
    /-----------------------------------------------------------------------*/

    @PutMapping(UPDATE_NON_CONFORMITE_RPOCESSUS)
    public ResponseEntity<NonConformiteDto> updateNonConformite(@PathVariable UUID id, @RequestBody NonConformiteDto dto) throws IOException {
        return ResponseEntity.ok(nonConformiteService.updateNonConformite(id, dto));
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de modification d'une liste de non conformité             /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_LISTE_NON_CONFORMITE_RPOCESSUS)
    public ResponseEntity<List<NonConformiteDto>> updateNonConformites(@RequestBody List<NonConformiteDto> dtos) throws IOException {
        List<NonConformiteDto> updatedNonConformites = nonConformiteService.updateNonConformites(dtos);
        return ResponseEntity.ok(updatedNonConformites);
    }



    /*-----------------------------------------------------------------------/
    /               Méthode de création d'une NonConformité                  /
    /-----------------------------------------------------------------------*/
    @PostMapping(CREATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> create(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.create(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /              Méthode de mise à jour d'une NonConformite                /
    /-----------------------------------------------------------------------*/
    @PutMapping(UPDATE_NON_CONFORMITE)
    public ResponseEntity<NonConformiteDto> update(@RequestBody NonConformiteDto nonConformiteDto) {
        NonConformiteDto nonConformite = nonConformiteService.update(nonConformiteDto);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de toutes les NonConformités              /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_ALL_NON_CONFORMITE)
    public ResponseEntity<List<NonConformiteDto>> allNonConformites() {
        List<NonConformiteDto> nonConformite = nonConformiteService.allNonConformites();
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }
    /*-----------------------------------------------------------------------/
    /      Méthode de récupération de NonConformités par Etat                /
    /-----------------------------------------------------------------------*/

    @GetMapping(GET_ETAT_BAY_NON_CONFORMITE)
    public List<NonConformiteDto> getNonConformitesByEtat(@PathVariable Etat etapeTraitement) {
        return nonConformiteService.getNonConformitesByEtatNonConformite(etapeTraitement);
    }

    /*-----------------------------------------------------------------------/
    /       Méthode de récupération d'une NonConformité par son ID           /
    /-----------------------------------------------------------------------*/
    @GetMapping(GET_NON_CONFORMITE_BY_ID)
    public ResponseEntity<NonConformiteDto> getNonConformiteById(@PathVariable UUID id) {
        NonConformiteDto nonConformite = nonConformiteService.getNonConformiteById(id);
        return new ResponseEntity<>(nonConformite, HttpStatus.OK);
    }

    /*-----------------------------------------------------------------------/
    /                Méthode de suppression d'une NonConformité              /
    /-----------------------------------------------------------------------*/
    @DeleteMapping(DELETE_NON_CONFORMITE)
    public void deleteById(@PathVariable UUID id) {
        nonConformiteService.delete(id);
    }

     /*-----------------------------------------------------------------------------------------------------/
    /                   Méthode de recupération des non conformité lie à utilisateur                       /
    /----------------------------------------------------------------------------------------------------*/

    @GetMapping(GET_NON_CONFORMITE_BAY_USER)
    public ResponseEntity<List<NonConformiteDto>> getMyNonConformites(Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            Map<String, Object> claims = jwtToken.getToken().getClaims();
            String email = (String) claims.get("email"); // ✅ Récupération de l'email depuis le token

            if (email == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.emptyList()); // 🛑 Si l'email n'est pas présent
            }

            List<NonConformiteDto> nonConformites = nonConformiteService.getNonConformitesByEmail(email);
            return ResponseEntity.ok(nonConformites);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


}
