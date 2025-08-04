package com.qualiapproche.controller;

import com.qualiapproche.dto.FichierDto;
import com.qualiapproche.service.FichierService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.qualiapproche.utils.ApiUrls.*;

@RestController
@RequestMapping(FICHIER_ROOT_URL)
@RequiredArgsConstructor
public class FichierController {

 private final FichierService fichierService;

    /*----------------------------------------------------------------------------/
    /                   Méthode de création d'un document                         /
    /----------------------------------------------------------------------------*/
    @PostMapping(CREATE_FICHIER_ROOT_URL)
    public ResponseEntity<?> saveFichier(@RequestBody FichierDto fichierDto, @RequestParam UUID categorieId) {
        if (fichierDto == null || categorieId == null) {
            return ResponseEntity.status(HttpStatus.OK).body(" Le fichier ou categorie du fichier ne peut pas être nul");
        }
        try {
            fichierService.saveFichier(fichierDto, categorieId);
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Fichier sauvegarde avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }


    /*----------------------------------------------------------------------------/
    /                Méthode de suppression d'un document                         /
    /----------------------------------------------------------------------------*/

    @DeleteMapping(DELETE_FICHIER_ROOT_URL)
    public ResponseEntity<?> deleteFichier(@PathVariable UUID fichierId) {
        try {
            fichierService.deleteFichier(fichierId);
            return ResponseEntity.ok("Fichier supprimé avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression du fichier : " + e.getMessage());
        }
    }

    @GetMapping("/files/image/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = fichierService.load(filename);
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.valueOf("image/png")).body(file);
    }

    @GetMapping("/files/pdf/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFilePDF(@PathVariable String filename) {
        Resource file = fichierService.load(filename);
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.valueOf("application/pdf")).body(file);
    }

    @GetMapping("/files/msword/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFileMsword(@PathVariable String filename) {
        Resource file = fichierService.load(filename);
        return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.valueOf("application/msword")).body(file);
    }
}
