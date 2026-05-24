package com.qualiapproche.support.service;

import com.qualiapproche.support.client.AlfrescoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.thymeleaf.context.Context;
import com.qualiapproche.common.config.ThymeleafConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlfrescoDocumentService {

    private final AlfrescoClient alfrescoClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final JavaMailSender javaMailSender;

    @Value("${alfresco.root-folder-id:-root-}")
    private String rootFolderId;

    @Value("${alfresco.url:http://localhost:8999/alfresco}")
    private String alfrescoUrl;

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadSiteConfig() {
        try {
            // 1. Try to load from external file in working directory
            File extFile = new File("site-config.json");
            if (extFile.exists() && extFile.isFile()) {
                log.info("Loading Alfresco site config from external file: {}", extFile.getAbsolutePath());
                return objectMapper.readValue(extFile, new TypeReference<Map<String, Object>>() {});
            }
            
            // 2. Fall back to classpath resource
            Resource resource = resourceLoader.getResource("classpath:site-config.json");
            if (resource.exists()) {
                log.info("Loading Alfresco site config from classpath resource");
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                }
            }
        } catch (Exception e) {
            log.error("Failed to load site-config.json", e);
        }
        
        // Default fallback if loading fails
        return Map.of(
                "id", "qms-site",
                "title", "Gestion Documentaire QMS",
                "description", "Site collaboratif pour la gestion documentaire QMS",
                "visibility", "PUBLIC"
        );
    }

    @SuppressWarnings("unchecked")
    private String resolveSiteDocumentLibrary() {
        Map<String, Object> siteConfig = loadSiteConfig();
        String siteId = (String) siteConfig.getOrDefault("id", "qms-site");
        try {
            // First try to check if the site exists and get its documentLibrary container
            try {
                Map<String, Object> containerResponse = alfrescoClient.getSiteContainer(siteId, "documentLibrary");
                if (containerResponse != null && containerResponse.containsKey("entry")) {
                    Map<String, Object> entry = (Map<String, Object>) containerResponse.get("entry");
                    if (entry != null && entry.containsKey("id")) {
                        String docLibId = (String) entry.get("id");
                        log.info("Successfully fetched documentLibrary for site '{}': {}", siteId, docLibId);
                        return docLibId;
                    }
                }
            } catch (Exception e) {
                log.info("Site '{}' not found or failed to fetch documentLibrary, attempting to create site dynamically...", siteId);
                try {
                    alfrescoClient.createSite(siteConfig);
                    log.info("Successfully created site '{}'", siteId);
                    
                    // Retrieve documentLibrary again
                    Map<String, Object> containerResponse = alfrescoClient.getSiteContainer(siteId, "documentLibrary");
                    if (containerResponse != null && containerResponse.containsKey("entry")) {
                        Map<String, Object> entry = (Map<String, Object>) containerResponse.get("entry");
                        if (entry != null && entry.containsKey("id")) {
                            return (String) entry.get("id");
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to create site '{}' dynamically: {}", siteId, ex.getMessage());
                }
            }

            // Fallback: search all available sites (original logic)
            String sitesFolderId = null;
            Map<String, Object> rootChildren = alfrescoClient.getChildren(rootFolderId);
            if (rootChildren != null && rootChildren.containsKey("list")) {
                Map<String, Object> listMap = (Map<String, Object>) rootChildren.get("list");
                if (listMap != null && listMap.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                    if (entries != null) {
                        for (Map<String, Object> entryWrapper : entries) {
                            Map<String, Object> entry = (Map<String, Object>) entryWrapper.get("entry");
                            if (entry != null && ("st:sites".equals(entry.get("nodeType")) || "Sites".equalsIgnoreCase((String) entry.get("name")))) {
                                sitesFolderId = (String) entry.get("id");
                                break;
                            }
                        }
                    }
                }
            }

            if (sitesFolderId == null) {
                log.info("Sites folder not found under root folder ID '{}'", rootFolderId);
                return null;
            }

            String siteFolderId = null;
            Map<String, Object> sitesChildren = alfrescoClient.getChildren(sitesFolderId);
            if (sitesChildren != null && sitesChildren.containsKey("list")) {
                Map<String, Object> listMap = (Map<String, Object>) sitesChildren.get("list");
                if (listMap != null && listMap.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                    if (entries != null && !entries.isEmpty()) {
                        Map<String, Object> firstEntryWrapper = entries.get(0);
                        Map<String, Object> entry = (Map<String, Object>) firstEntryWrapper.get("entry");
                        if (entry != null) {
                            siteFolderId = (String) entry.get("id");
                            log.info("Found site folder '{}' with ID: {}", entry.get("name"), siteFolderId);
                        }
                    }
                }
            }

            if (siteFolderId == null) {
                log.info("No site folders found under Sites folder '{}'", sitesFolderId);
                return null;
            }

            Map<String, Object> siteChildren = alfrescoClient.getChildren(siteFolderId);
            if (siteChildren != null && siteChildren.containsKey("list")) {
                Map<String, Object> listMap = (Map<String, Object>) siteChildren.get("list");
                if (listMap != null && listMap.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                    if (entries != null) {
                        for (Map<String, Object> entryWrapper : entries) {
                            Map<String, Object> entry = (Map<String, Object>) entryWrapper.get("entry");
                            if (entry != null && "documentLibrary".equalsIgnoreCase((String) entry.get("name"))) {
                                String docLibId = (String) entry.get("id");
                                log.info("Resolved site's documentLibrary folder ID: {}", docLibId);
                                return docLibId;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve site documentLibrary dynamically: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Finds or creates a folder with the specified name under the parent folder.
     *
     * @param parentId   the parent folder node ID
     * @param folderName the folder name to check/create
     * @return the folder's nodeId
     */
    @SuppressWarnings("unchecked")
    public String getOrCreateFolder(String parentId, String folderName) {
        String parent = (parentId == null || parentId.isBlank()) ? rootFolderId : parentId;

        if (rootFolderId.equals(parent) && "documentLibrary".equalsIgnoreCase(folderName)) {
            String siteDocLibId = resolveSiteDocumentLibrary();
            if (siteDocLibId != null) {
                return siteDocLibId;
            }
        }

        log.info("Checking or creating folder '{}' under parent '{}'", folderName, parent);

        try {
            Map<String, Object> childrenResponse = alfrescoClient.getChildren(parent);
            if (childrenResponse != null && childrenResponse.containsKey("list")) {
                Map<String, Object> listMap = (Map<String, Object>) childrenResponse.get("list");
                if (listMap != null && listMap.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                    if (entries != null) {
                        for (Map<String, Object> entryWrapper : entries) {
                            Map<String, Object> entry = (Map<String, Object>) entryWrapper.get("entry");
                            if (entry != null && (Boolean.TRUE.equals(entry.get("isFolder")) || "cm:folder".equals(entry.get("nodeType"))) && folderName.equalsIgnoreCase((String) entry.get("name"))) {
                                log.info("Found existing folder '{}' with ID: {}", folderName, entry.get("id"));
                                return (String) entry.get("id");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error listing children of parent '{}', attempting to create folder directly: {}", parent, e.getMessage());
        }

        // Folder not found, let's create it
        try {
            Map<String, Object> folderBody = Map.of(
                    "name", folderName,
                    "nodeType", "cm:folder"
            );
            Map<String, Object> response = alfrescoClient.createNode(parent, folderBody);
            if (response != null && response.containsKey("entry")) {
                Map<String, Object> entry = (Map<String, Object>) response.get("entry");
                if (entry != null && entry.containsKey("id")) {
                    String createdId = (String) entry.get("id");
                    log.info("Created folder '{}' under parent '{}' with ID: {}", folderName, parent, createdId);
                    return createdId;
                }
            }
        } catch (Exception e) {
            log.error("Failed to create folder '{}' under parent '{}'", folderName, parent, e);
            throw new RuntimeException("Erreur lors de la création du dossier Alfresco: " + e.getMessage(), e);
        }

        throw new RuntimeException("Impossible de créer ou récupérer le dossier Alfresco : " + folderName);
    }

    /**
     * Uploads a file under a parent node ID in Alfresco.
     *
     * @param parentId the parent folder ID (nodeId)
     * @param file     the file payload
     * @return the created document node metadata map
     */
    public Map<String, Object> uploadFile(String parentId, MultipartFile file) {
        String parent = (parentId == null || parentId.isBlank()) ? rootFolderId : parentId;
        log.info("Uploading file '{}' under parent folder '{}'", file.getOriginalFilename(), parent);
        try {
            return alfrescoClient.uploadFile(parent, file, file.getOriginalFilename(), "cm:content");
        } catch (Exception e) {
            log.error("Failed to upload file to Alfresco under parent '{}'", parent, e);
            throw new RuntimeException("Erreur lors du dépôt du document dans Alfresco: " + e.getMessage(), e);
        }
    }

    /**
     * Downloads file contents from Alfresco by nodeId.
     *
     * @param nodeId the node ID of the document
     * @return response entity with byte array payload
     */
    public ResponseEntity<byte[]> downloadFile(String nodeId) {
        log.info("Downloading file with ID '{}' from Alfresco", nodeId);
        try {
            return alfrescoClient.downloadFile(nodeId);
        } catch (Exception e) {
            log.error("Failed to download document with ID '{}' from Alfresco", nodeId, e);
            throw new RuntimeException("Erreur de téléchargement du document depuis Alfresco: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a node (folder or file) from Alfresco by nodeId.
     *
     * @param nodeId the node ID to delete
     */
    public void deleteNode(String nodeId) {
        log.info("Deleting node with ID '{}' from Alfresco", nodeId);
        try {
            alfrescoClient.deleteNode(nodeId);
        } catch (Exception e) {
            log.error("Failed to delete node with ID '{}' from Alfresco", nodeId, e);
            throw new RuntimeException("Erreur de suppression du document dans Alfresco: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAlfrescoUsers() {
        log.info("Fetching list of users from Alfresco");
        try {
            Map<String, Object> response = alfrescoClient.getPeople(0, 100);
            if (response != null && response.containsKey("list")) {
                Map<String, Object> listMap = (Map<String, Object>) response.get("list");
                if (listMap != null && listMap.containsKey("entries")) {
                    List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                    return entries.stream()
                            .map(entry -> (Map<String, Object>) entry.get("entry"))
                            .toList();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Alfresco users", e);
        }
        return List.of();
    }

    public void createAlfrescoUser(String username, String firstName, String lastName, String email, String password) {
        log.info("Creating Alfresco user '{}' ({})", username, email);
        try {
            Map<String, Object> personBody = Map.of(
                    "id", username,
                    "firstName", firstName,
                    "lastName", lastName,
                    "email", email,
                    "password", password,
                    "enabled", true
            );
            alfrescoClient.createPerson(personBody);
            
            // Send email to user with their credentials
            sendAccessEmail(email, username, firstName, lastName, password);
        } catch (Exception e) {
            log.error("Failed to create Alfresco user '{}'", username, e);
            throw new RuntimeException("Erreur lors de la création de l'utilisateur Alfresco: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public void setNodePermissions(String nodeId, String username, String role, String docNumber) {
        log.info("Setting permission '{}' for user '{}' on node '{}'", role, username, nodeId);
        try {
            String alfrescoPermission;
            if ("WRITE".equalsIgnoreCase(role)) {
                alfrescoPermission = "Collaborator";
            } else {
                alfrescoPermission = "Consumer";
            }

            Map<String, Object> permissionEntry = Map.of(
                    "authorityId", username,
                    "name", alfrescoPermission,
                    "accessStatus", "ALLOWED"
            );

            Map<String, Object> permissions = Map.of(
                    "isInheritanceEnabled", true,
                    "locallySet", List.of(permissionEntry)
            );

            Map<String, Object> nodeBody = Map.of(
                    "permissions", permissions
            );

            alfrescoClient.updateNode(nodeId, nodeBody);

            // Fetch target user's email from Alfresco to send them the email notification
            String recipientEmail = null;
            try {
                Map<String, Object> person = alfrescoClient.getPerson(username);
                if (person != null && person.containsKey("entry")) {
                    Map<String, Object> entry = (Map<String, Object>) person.get("entry");
                    if (entry != null && entry.containsKey("email")) {
                        recipientEmail = (String) entry.get("email");
                    }
                }
            } catch (Exception e) {
                log.warn("Could not retrieve email for user '{}' from Alfresco: {}", username, e.getMessage());
            }

            if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                String sharedId = getOrCreateShareLink(nodeId);
                String shareUrl = "http://localhost:8999/share/s/" + sharedId;
                sendDocumentAssignmentEmail(recipientEmail, username, docNumber, shareUrl);
            }
        } catch (Exception e) {
            log.error("Failed to set node permissions on node '{}' for user '{}'", nodeId, username, e);
            throw new RuntimeException("Erreur de configuration des rôles Alfresco: " + e.getMessage(), e);
        }
    }

    private void sendAccessEmail(String recipientEmail, String username, String firstName, String lastName, String password) {
        log.info("Sending Alfresco credentials email to {}", recipientEmail);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject("Vos accès à la plateforme documentaire Alfresco");

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("lastName", lastName);
            context.setVariable("username", username);
            context.setVariable("password", password);
            String htmlMessage = ThymeleafConfig.getTemplateEngine().process("alfrescoUserCreation", context);

            helper.setText(htmlMessage, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send credentials email to {}", recipientEmail, e);
        }
    }

    private void sendDocumentAssignmentEmail(String recipientEmail, String username, String docNumber, String shareUrl) {
        log.info("Sending document assignment email to {}", recipientEmail);
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(String.format("Nouveau document partagé avec vous : %s", docNumber));

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("docNumber", docNumber);
            context.setVariable("shareUrl", shareUrl);
            String htmlMessage = ThymeleafConfig.getTemplateEngine().process("alfrescoDocumentAssignment", context);

            helper.setText(htmlMessage, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Failed to send document assignment email to {}", recipientEmail, e);
        }
    }

    @SuppressWarnings("unchecked")
    public String getOrCreateShareLink(String nodeId) {
        log.info("Getting or creating share link for node '{}'", nodeId);

        // 1. Try to create a new shared link
        try {
            Map<String, Object> response = alfrescoClient.createSharedLink(Map.of("nodeId", nodeId));
            if (response != null && response.containsKey("entry")) {
                Map<String, Object> entry = (Map<String, Object>) response.get("entry");
                if (entry != null && entry.containsKey("id")) {
                    String sharedId = (String) entry.get("id");
                    log.info("Generated new shared ID: {}", sharedId);
                    return sharedId;
                }
            }
        } catch (feign.FeignException.Conflict conflict) {
            // 2. Shared link already exists — extract the sharedId from the error message
            log.info("Shared link already exists for node '{}', retrieving existing link...", nodeId);
            String errorBody = conflict.contentUTF8();

            // The error message contains the sharedId in brackets: [dvJQt871TmuyULfO9d5ryw]
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[([A-Za-z0-9_-]{20,})\\]").matcher(errorBody);
            if (matcher.find()) {
                String existingSharedId = matcher.group(1);
                log.info("Retrieved existing shared ID from error: {}", existingSharedId);
                return existingSharedId;
            }

            // 3. Fallback: query all shared links to find the one for this nodeId
            try {
                Map<String, Object> listResponse = alfrescoClient.getSharedLinks(null);
                if (listResponse != null && listResponse.containsKey("list")) {
                    Map<String, Object> listMap = (Map<String, Object>) listResponse.get("list");
                    if (listMap != null && listMap.containsKey("entries")) {
                        List<Map<String, Object>> entries = (List<Map<String, Object>>) listMap.get("entries");
                        for (Map<String, Object> e : entries) {
                            Map<String, Object> entry = (Map<String, Object>) e.get("entry");
                            if (entry != null && nodeId.equals(entry.get("nodeId"))) {
                                String sharedId = (String) entry.get("id");
                                log.info("Found existing shared ID via listing: {}", sharedId);
                                return sharedId;
                            }
                        }
                    }
                }
            } catch (Exception listEx) {
                log.warn("Failed to list shared links: {}", listEx.getMessage());
            }

            throw new RuntimeException("Le lien de partage existe déjà mais impossible de récupérer son identifiant.");
        } catch (Exception e) {
            log.error("Failed to generate shared link for node '{}': {}", nodeId, e.getMessage());
            throw new RuntimeException("Impossible de générer le lien de partage Alfresco: " + e.getMessage(), e);
        }
        throw new RuntimeException("Aucune réponse valide d'Alfresco lors de la création du lien de partage.");
    }

    @SuppressWarnings("unchecked")
    public String getAosUrl(String nodeId) {
        log.info("Generating AOS URL for node '{}'", nodeId);
        try {
            Map<String, Object> nodeInfo = alfrescoClient.getNodeInfo(nodeId, "path");
            if (nodeInfo != null && nodeInfo.containsKey("entry")) {
                Map<String, Object> entry = (Map<String, Object>) nodeInfo.get("entry");
                String fileName = (String) entry.get("name");
                
                if (entry.containsKey("path")) {
                    Map<String, Object> pathObj = (Map<String, Object>) entry.get("path");
                    String pathName = (String) pathObj.get("name");
                    
                    if (pathName != null && pathName.startsWith("/Company Home/")) {
                        pathName = pathName.substring("/Company Home/".length());
                    } else if (pathName != null && pathName.startsWith("/Company Home")) {
                        pathName = pathName.substring("/Company Home".length());
                    }
                    
                    // URL Encode each path segment
                    String encodedPath = java.util.Arrays.stream(pathName.split("/"))
                            .map(segment -> java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                            .collect(java.util.stream.Collectors.joining("/"));
                            
                    String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
                    
                    // Base AOS URL e.g. http://localhost:8999/alfresco/aos
                    String baseAosUrl = alfrescoUrl + "/aos";
                    if (!baseAosUrl.endsWith("/")) {
                        baseAosUrl += "/";
                    }
                    
                    String fullAosUrl = baseAosUrl + (encodedPath.isEmpty() ? "" : encodedPath + "/") + encodedFileName;
                    
                    // Determine protocol prefix based on file extension
                    String protocolPrefix = "";
                    String lowerFileName = fileName.toLowerCase();
                    if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
                        protocolPrefix = "ms-word:ofe|u|";
                    } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
                        protocolPrefix = "ms-excel:ofe|u|";
                    } else if (lowerFileName.endsWith(".ppt") || lowerFileName.endsWith(".pptx")) {
                        protocolPrefix = "ms-powerpoint:ofe|u|";
                    } else {
                        // Not an office document, return null or just the WebDAV URL
                        log.warn("Node '{}' is not an Office document, cannot generate AOS protocol link.", fileName);
                        return null;
                    }
                    
                    return protocolPrefix + fullAosUrl;
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate AOS URL for node '{}'", nodeId, e);
        }
        return null;
    }
}
