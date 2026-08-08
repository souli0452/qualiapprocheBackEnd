package com.qualiapproche.amelioration.common.utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.qualiapproche.common.dto.NonConformiteDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import com.google.zxing.WriterException;

public class QRCodeGenerator {

  public static String generateAndSaveQRCodeImage(NonConformiteDto demande) throws IOException, WriterException {
    String qrContent = generateQRContent(demande);
    String fileName = "qr_" + demande.getNumeroReference().replace("/", "_") + ".png";

    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 350, 350);

    // Chemin vers le dossier 'qrCode' sous 'src/main/resources'
    Path path = FileSystems.getDefault().getPath("qrCode/" + fileName);

    // Créer le dossier qrCode s'il n'existe pas
    File directory = new File("qrCode");
    if (!directory.exists()) {
      directory.mkdirs();
    }

    // Écrire l'image PNG au chemin spécifié
    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

    return "QR code saved to: " + path.toString();
  }

  private static String generateQRContent(NonConformiteDto demande) {
    StringBuilder qrContent = new StringBuilder();
    qrContent.append("Numero: ").append(demande.getNumeroReference()).append("\n")
      .append("Identité Démandeur: ").append(demande.getCurrentUserfullName()).append("\n")
      .append("Email Demandeur: ").append(demande.getCurrentUserEmail()).append("\n")
      .append("Date de création: ").append(demande.getCreatedAt()).append("\n");
    return qrContent.toString();
  }

}
