package com.qualiapproche.support.client;

import com.qualiapproche.support.config.AlfrescoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@FeignClient(
        name = "alfresco-client",
        url = "${alfresco.url}/api/-default-/public/alfresco/versions/1",
        configuration = AlfrescoFeignConfig.class
)
public interface AlfrescoClient {

    @PostMapping(value = "/nodes/{nodeId}/children", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> createNode(
            @PathVariable("nodeId") String nodeId,
            @RequestBody Map<String, Object> nodeBody
    );

    @PostMapping(value = "/nodes/{nodeId}/children", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> uploadFile(
            @PathVariable("nodeId") String nodeId,
            @RequestPart("filedata") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "nodeType", required = false, defaultValue = "cm:content") String nodeType
    );

    @GetMapping("/nodes/{nodeId}/content")
    ResponseEntity<byte[]> downloadFile(@PathVariable("nodeId") String nodeId);

    @GetMapping("/nodes/{nodeId}/children")
    Map<String, Object> getChildren(@PathVariable("nodeId") String nodeId);

    @DeleteMapping("/nodes/{nodeId}")
    void deleteNode(@PathVariable("nodeId") String nodeId);
    
    @GetMapping("/nodes/{nodeId}")
    Map<String, Object> getNodeInfo(
            @PathVariable("nodeId") String nodeId,
            @RequestParam(value = "include", required = false) String include
    );

    @PostMapping(value = "/nodes/{nodeId}/copy", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> copyNode(
            @PathVariable("nodeId") String nodeId,
            @RequestBody Map<String, Object> copyBody
    );

    @PostMapping(value = "/nodes/{nodeId}/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> moveNode(
            @PathVariable("nodeId") String nodeId,
            @RequestBody Map<String, Object> moveBody
    );

    @GetMapping("/sites/{siteId}")
    Map<String, Object> getSite(@PathVariable("siteId") String siteId);

    @PostMapping(value = "/sites", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> createSite(@RequestBody Map<String, Object> siteBody);

    @GetMapping("/sites/{siteId}/containers/{containerId}")
    Map<String, Object> getSiteContainer(
            @PathVariable("siteId") String siteId,
            @PathVariable("containerId") String containerId
    );

    @PostMapping(value = "/people", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> createPerson(@RequestBody Map<String, Object> personBody);

    @GetMapping("/people")
    Map<String, Object> getPeople(
            @RequestParam(value = "skipCount", required = false) Integer skipCount,
            @RequestParam(value = "maxItems", required = false) Integer maxItems
    );

    @GetMapping("/people/{personId}")
    Map<String, Object> getPerson(@PathVariable("personId") String personId);

    @PutMapping(value = "/nodes/{nodeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> updateNode(
            @PathVariable("nodeId") String nodeId,
            @RequestBody Map<String, Object> nodeBody
    );

    @PostMapping(value = "/shared-links", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> createSharedLink(@RequestBody Map<String, Object> sharedLinkBody);

    @GetMapping("/shared-links")
    Map<String, Object> getSharedLinks(@RequestParam(value = "where", required = false) String where);
}
