package com.qualiapproche.evaluation.service;

import com.qualiapproche.common.exception.BusinessException;
import com.qualiapproche.evaluation.dto.PointControleDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Les points de contrôle d'une grille tenue sous Excel ou en CSV, lus pour être relus.
 *
 * <p>Les grilles d'audit existent souvent déjà sous ce format (recette du 17 oct. 2026,
 * observation 33). Rien n'est enregistré ici : les points lus reviennent à l'écran, s'ajoutent à
 * la checklist en brouillon, et l'utilisateur les relit avant de l'enregistrer.</p>
 *
 * <p>Colonnes, dans l'ordre du modèle : chapitre ISO, libellé (obligatoire), question, preuve
 * attendue. La première ligne est un en-tête ; une ligne sans libellé est ignorée.</p>
 */
@Component
public class ImportDePoints {

    static final List<String> COLONNES = List.of("Chapitre ISO", "Libellé du point *", "Question à poser", "Preuve attendue");
    static final int LIGNES_MAX = 1000;
    private static final long TAILLE_MAX = 5L * 1024 * 1024;

    public List<PointControleDto> lire(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Aucun fichier reçu.", HttpStatus.BAD_REQUEST);
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new BusinessException("Le fichier dépasse 5 Mo.", HttpStatus.BAD_REQUEST);
        }
        String nom = fichier.getOriginalFilename() == null ? "" : fichier.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<List<String>> lignes;
        try (InputStream flux = fichier.getInputStream()) {
            if (nom.endsWith(".xlsx") || nom.endsWith(".xls")) {
                lignes = lignesExcel(flux);
            } else if (nom.endsWith(".csv") || nom.endsWith(".txt")) {
                lignes = lignesCsv(flux);
            } else {
                throw new BusinessException("Format non reconnu : déposez un fichier Excel (.xlsx) ou CSV.", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new BusinessException("Le fichier n'a pas pu être lu : " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        List<PointControleDto> points = new ArrayList<>();
        for (int i = 1; i < lignes.size(); i++) {
            List<String> l = lignes.get(i);
            String libelle = colonne(l, 1);
            if (libelle == null) {
                continue;
            }
            if (points.size() >= LIGNES_MAX) {
                throw new BusinessException("Plus de " + LIGNES_MAX + " points : découpez la grille.", HttpStatus.BAD_REQUEST);
            }
            points.add(PointControleDto.builder()
                    .chapitreISO(colonne(l, 0))
                    .libelle(libelle)
                    .question(colonne(l, 2))
                    .preuveAttendue(colonne(l, 3))
                    .build());
        }
        if (points.isEmpty()) {
            throw new BusinessException("Aucun point lu : la colonne « Libellé du point » est vide sur toutes les lignes.",
                    HttpStatus.BAD_REQUEST);
        }
        return points;
    }

    /** Le modèle à remplir : l'en-tête, et une ligne d'exemple. */
    public byte[] modele() {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet("Points de contrôle");
            CellStyle gras = classeur.createCellStyle();
            Font police = classeur.createFont();
            police.setBold(true);
            gras.setFont(police);
            Row entete = feuille.createRow(0);
            for (int i = 0; i < COLONNES.size(); i++) {
                Cell c = entete.createCell(i);
                c.setCellValue(COLONNES.get(i));
                c.setCellStyle(gras);
                feuille.setColumnWidth(i, i == 0 ? 14 * 256 : 48 * 256);
            }
            Row exemple = feuille.createRow(1);
            exemple.createCell(0).setCellValue("7.5.3");
            exemple.createCell(1).setCellValue("Maîtrise des informations documentées");
            exemple.createCell(2).setCellValue("Comment les documents sont-ils approuvés et diffusés ?");
            exemple.createCell(3).setCellValue("Liste des documents en vigueur, circuit d'approbation");
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Modèle d'import illisible", e);
        }
    }

    private static List<List<String>> lignesExcel(InputStream flux) throws IOException {
        List<List<String>> lignes = new ArrayList<>();
        DataFormatter format = new DataFormatter(Locale.FRANCE);
        try (Workbook classeur = WorkbookFactory.create(flux)) {
            Sheet feuille = classeur.getSheetAt(0);
            for (Row r : feuille) {
                List<String> cellules = new ArrayList<>();
                for (int i = 0; i < COLONNES.size(); i++) {
                    Cell c = r.getCell(i);
                    cellules.add(c == null ? "" : format.formatCellValue(c));
                }
                lignes.add(cellules);
                if (lignes.size() > LIGNES_MAX + 1) {
                    break;
                }
            }
        }
        return lignes;
    }

    /** Point-virgule (Excel en français) ou virgule ; guillemets doubles pour un texte qui en contient. */
    private static List<List<String>> lignesCsv(InputStream flux) throws IOException {
        List<List<String>> lignes = new ArrayList<>();
        try (BufferedReader lecteur = new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8))) {
            String ligne;
            Character separateur = null;
            while ((ligne = lecteur.readLine()) != null && lignes.size() <= LIGNES_MAX + 1) {
                if (lignes.isEmpty() && ligne.startsWith("﻿")) {
                    ligne = ligne.substring(1);
                }
                if (separateur == null) {
                    separateur = ligne.indexOf(';') >= 0 ? ';' : ',';
                }
                lignes.add(decouper(ligne, separateur));
            }
        }
        return lignes;
    }

    static List<String> decouper(String ligne, char separateur) {
        List<String> cellules = new ArrayList<>();
        StringBuilder courante = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (c == '"') {
                if (entreGuillemets && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    courante.append('"');
                    i++;
                } else {
                    entreGuillemets = !entreGuillemets;
                }
            } else if (c == separateur && !entreGuillemets) {
                cellules.add(courante.toString());
                courante.setLength(0);
            } else {
                courante.append(c);
            }
        }
        cellules.add(courante.toString());
        return cellules;
    }

    private static String colonne(List<String> ligne, int i) {
        if (i >= ligne.size() || ligne.get(i) == null) {
            return null;
        }
        String v = ligne.get(i).strip();
        return v.isEmpty() ? null : v;
    }
}
