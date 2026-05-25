package com.qualiapproche.support.controller;

import com.qualiapproche.support.model.QmsDocumentType;
import com.qualiapproche.support.service.QmsDocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.qualiapproche.common.utils.ApiUrls.DOCUMENT_TYPE;


@RestController
@RequestMapping(DOCUMENT_TYPE)
@RequiredArgsConstructor
public class QmsDocumentTypeController {

    private final QmsDocumentTypeService typeService;

    @GetMapping
    public ResponseEntity<List<QmsDocumentType>> getAllTypes() {
        return ResponseEntity.ok(typeService.getAllTypes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QmsDocumentType> getTypeById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(typeService.getTypeById(id));
    }

    @PostMapping
    public ResponseEntity<QmsDocumentType> createType(@RequestBody QmsDocumentType type) {
        return ResponseEntity.ok(typeService.createType(type));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QmsDocumentType> updateType(@PathVariable("id") UUID id, @RequestBody QmsDocumentType type) {
        return ResponseEntity.ok(typeService.updateType(id, type));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable("id") UUID id) {
        typeService.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}
