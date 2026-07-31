sed -i '' -e 's/return ResponseEntity.ok(documentMapper.toDto(documentService.getDocumentById(id)));/DocumentQms doc = documentService.getDocumentById(id);\
        DocumentQmsDto dto = documentMapper.toDto(doc);\
        try {\
            java.util.Map<String, Object> state = workflowClient.getWorkflowState(id);\
            dto.setWorkflowState(state);\
        } catch(Exception e) {\
            log.warn("Could not fetch workflow state for document {}: {}", id, e.getMessage());\
        }\
        return ResponseEntity.ok(dto);/g' ./support-service/src/main/java/com/qualiapproche/support/controller/QmsDocumentController.java
