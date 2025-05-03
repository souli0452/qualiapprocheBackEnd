package com.qualiapproche.service.impl;

import com.qualiapproche.controller.FichierController;
import com.qualiapproche.dto.FichierDto;
import com.qualiapproche.entities.CategorieFichier;
import com.qualiapproche.entities.Fichier;
import com.qualiapproche.repository.CategorieFichierRepository;
import com.qualiapproche.repository.FichierRepository;
import com.qualiapproche.service.FichierService;
import com.qualiapproche.utils.RootPath;
import jakarta.annotation.PostConstruct;

import java.util.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.qualiapproche.utils.RootPath.fichierRootPath;

@Service
public class FichierServiceImpl implements FichierService {

    private final CategorieFichierRepository categorieFichierRepository;
    private final FichierRepository fichierRepository;
    private final Path root = Paths.get("uploads");

    public FichierServiceImpl(CategorieFichierRepository categorieFichierRepository, FichierRepository fichierRepository) {
        this.categorieFichierRepository = categorieFichierRepository;
        this.fichierRepository = fichierRepository;
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Override
    public void saveFichier(FichierDto fichierDto, UUID categorieId) throws IOException {
        if (fichierDto == null) {
            throw new IllegalArgumentException("FichierDto ne peut pas être nul");
        }

        CategorieFichier categorieFichier = categorieFichierRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));

        // Vérifier si un fichier avec le même nom existe déjà dans la catégorie
        List<Fichier> existingFichiers = categorieFichier.getFichiers().stream()
                .filter(f -> f.getNomFichier() != null && f.getNomFichier().equals(fichierDto.getNomFichier()))
                .collect(Collectors.toList());

        Fichier fichier = (Fichier) convertBase64File(fichierDto);
        if (existingFichiers.isEmpty()) {
            // Si le fichier n'existe pas, créer un nouveau fichier avec la version 1
            fichier.setVersionFichier(1);
        } else {
            // Si le fichier existe, trouver la version maximale et incrémenter
            int maxVersion = existingFichiers.stream()
                    .mapToInt(Fichier::getVersionFichier)
                    .max()
                    .orElse(0);
            fichier.setVersionFichier(maxVersion + 1);
        }
        fichier = fichierRepository.save(fichier);
        categorieFichier.getFichiers().add(fichier);
        categorieFichierRepository.save(categorieFichier);
    }

    @Override
    public void deleteFichier(UUID fichierId) throws IOException {
        Fichier fichier = fichierRepository.findById(fichierId)
                .orElseThrow(() -> new RuntimeException("Fichier non trouvé"));

        // Supprimer les références dans toutes les catégories associées
        List<CategorieFichier> categories = categorieFichierRepository.findAllByFichiersId(fichierId);
        for (CategorieFichier categorie : categories) {
            categorie.getFichiers().remove(fichier);
            categorieFichierRepository.save(categorie);
        }

        // Extraire le chemin du fichier physique à partir de l'URL
        String urlFichier = fichier.getUrlFichier();
        String fileName = urlFichier.substring(urlFichier.lastIndexOf("/") + 1);
        Path filePath = RootPath.fichierRootPath.resolve(fileName);

        // Supprimer le fichier du système de fichiers
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IOException("Échec de la suppression du fichier : " + filePath, e);
        }

        // Supprimer le fichier de la base de données
        fichierRepository.delete(fichier);
    }



    @Override
    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(root.toFile());
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.root, 1).filter(path -> !path.equals(this.root)).map(this.root::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }


    public String saveBase64FichierAndReturnUrl(String fichierBase64, String fichierRootPath, String mimeType) throws IOException {
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(fichierBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Le contenu du fichier n'est pas valide en base64.");
        }

        String uuid = UUID.randomUUID().toString().replace(" ", "");

        Map<String, String> mimeToExtension = Map.ofEntries(
                Map.entry("application/pdf", "pdf"),
                Map.entry("image/png", "png"),
                Map.entry("image/jpg", "jpg"),
                Map.entry("image/jpeg", "jpeg"),
                Map.entry("application/msword", "doc"),
                Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
                Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
                Map.entry("text/plain", "txt")
        );

        String extension = mimeToExtension.get(mimeType.toLowerCase());

        if (extension == null) {
            throw new IllegalArgumentException("Type de fichier non supporté : " + mimeType);
        }

        File file = new File(fichierRootPath, uuid + "." + extension);
        file.createNewFile();

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(fileBytes);
        }

        String methodName = switch (extension) {
            case "pdf" -> "getFilePDF";
            case "doc" -> "getFileMsword";
            default -> "getFile";
        };

        return MvcUriComponentsBuilder
                .fromMethodName(FichierController.class, methodName, uuid + "." + extension)
                .build()
                .toString();
    }

    public List<Fichier> convertBase64File(FichierDto fichierDtos) throws IOException {
        Fichier fichier = new Fichier();
        try {
            String url = saveBase64FichierAndReturnUrl(fichierDtos.getFichierBase64(), String.valueOf(fichierRootPath), fichierDtos.getTypeFichier().replace(" ", ""));
            fichier.setUrlFichier(url);
            fichier.setNomFichier(fichierDtos.getNomFichier());
            fichier.setDescriptionFichier(fichierDtos.getDescriptionFichier());
            fichier.setVersionFichier(fichierDtos.getVersionFichier());
            return Collections.singletonList(fichierRepository.save(fichier));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public List<Fichier> convertBase64(List<Fichier> fichierDtos) throws IOException {
        List<Fichier> fichiersUrls = new ArrayList<>();
        // Vérifie si fichierDtos est null ou vide
        if (fichierDtos == null || fichierDtos.isEmpty()) {
            return fichiersUrls; // Retourne une liste vide sans erreur
            }
        fichierDtos.forEach(f -> {
            try {
                String url = saveBase64FichierAndReturnUrl(f.getFichierBase64(), String.valueOf(fichierRootPath), f.getTypeFichier().replace(" ", ""));
                Fichier fichier = new Fichier();
                fichier.setUrlFichier(url);
                fichiersUrls.add(fichierRepository.save(fichier));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return fichiersUrls;
    }




}
