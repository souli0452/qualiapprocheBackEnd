package com.qualiapproche.support.controller;

import com.qualiapproche.support.service.AlfrescoDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/support/documents")
@RequiredArgsConstructor
@Tag(name = "Alfresco Documents", description = "Gestion documentaire via Alfresco dans le service Support")
public class AlfrescoController {

    private final AlfrescoDocumentService alfrescoDocumentService;

    @Operation(summary = "Obtenir ou créer un dossier", description = "Vérifie l'existence d'un dossier par nom et le crée si nécessaire")
    @PostMapping("/folder")
    public ResponseEntity<Map<String, String>> getOrCreateFolder(
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam("folderName") String folderName
    ) {
        String folderId = alfrescoDocumentService.getOrCreateFolder(parentId, folderName);
        return ResponseEntity.ok(Map.of("folderId", folderId));
    }

    @Operation(summary = "Uploader un document dans Alfresco", description = "Dépose un fichier binaire sous un dossier parent donné")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestPart("file") MultipartFile file
    ) {
        Map<String, Object> response = alfrescoDocumentService.uploadFile(parentId, file);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Télécharger un document depuis Alfresco", description = "Récupère le contenu binaire d'un document par son ID unique")
    @GetMapping("/download/{nodeId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable("nodeId") String nodeId) {
        return alfrescoDocumentService.downloadFile(nodeId);
    }

    @Operation(summary = "Supprimer un document ou dossier dans Alfresco", description = "Supprime définitivement un nœud Alfresco par son ID")
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable("nodeId") String nodeId) {
        alfrescoDocumentService.deleteNode(nodeId);
        return ResponseEntity.noContent().build();
    }
}
