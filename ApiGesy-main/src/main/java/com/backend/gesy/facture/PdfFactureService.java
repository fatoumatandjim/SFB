package com.backend.gesy.facture;

import com.backend.gesy.client.Client;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.LigneFactureDTO;
import com.backend.gesy.voyage.Voyage;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfFactureService {
    private final FactureRepository factureRepository;

    public byte[] generateFacturesPdf(Client client, List<Facture> factures) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate()); // Paysage pour le tableau large
            
            // En-tête
            Paragraph title = new Paragraph("RELEVÉ DES FACTURES")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(title);
            
            // Informations client
            Paragraph clientInfo = new Paragraph()
                    .add("Client: " + client.getNom())
                    .setFontSize(12)
                    .setMarginBottom(5);
            if (client.getCodeClient() != null) {
                clientInfo.add("\nCode: " + client.getCodeClient());
            }
            if (client.getEmail() != null) {
                clientInfo.add("\nEmail: " + client.getEmail());
            }
            if (client.getTelephone() != null) {
                clientInfo.add("\nTéléphone: " + client.getTelephone());
            }
            document.add(clientInfo);
            
            document.add(new Paragraph().setMarginBottom(15));
            
            // Tableau des factures
            float[] columnWidths = {60f, 50f, 80f, 50f, 50f, 60f, 50f, 70f, 70f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);
            
            // En-tête du tableau
            String[] headers = {"DATE", "NAT.", "PRODUIT", "QTÉ", "TAXES", "CAMION", "PU", "MONTANT", "RÈGLEMENT"};
            for (String header : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).setBold())
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(5);
                table.addHeaderCell(cell);
            }
            
            // Variables pour les totaux
            BigDecimal totalQuantite = BigDecimal.ZERO;
            BigDecimal totalTaxes = BigDecimal.ZERO;
            BigDecimal totalMontant = BigDecimal.ZERO;
            BigDecimal totalReglement = BigDecimal.ZERO;
            
            // Lignes de données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (Facture facture : factures) {
                // Récupérer le camion depuis le voyage associé (maintenant ManyToOne)
                String camionImmatriculation = "-";
                if (facture.getVoyage() != null) {
                    Voyage voyage = facture.getVoyage();
                    if (voyage.getCamion() != null) {
                        camionImmatriculation = voyage.getCamion().getImmatriculation();
                    }
                }
                
                if (facture.getLignes() != null && !facture.getLignes().isEmpty()) {
                    for (com.backend.gesy.facture.LigneFacture ligne : facture.getLignes()) {
                        // DATE
                        String dateStr = facture.getDate() != null ? 
                            facture.getDate().format(dateFormatter) : "-";
                        table.addCell(createCell(dateStr, TextAlignment.CENTER));
                        
                        // NAT. (Nature - type de produit)
                        String nature = "-";
                        if (ligne.getProduit() != null && ligne.getProduit().getTypeProduit() != null) {
                            nature = ligne.getProduit().getTypeProduit().name();
                        }
                        table.addCell(createCell(nature, TextAlignment.CENTER));
                        
                        // PRODUIT
                        String produit = ligne.getProduit() != null ? ligne.getProduit().getNom() : "-";
                        table.addCell(createCell(produit, TextAlignment.LEFT));
                        
                        // QTÉ
                        String quantite = ligne.getQuantite() != null ? 
                            String.format("%.2f", ligne.getQuantite()) : "0.00";
                        table.addCell(createCell(quantite, TextAlignment.RIGHT));
                        totalQuantite = totalQuantite.add(BigDecimal.valueOf(ligne.getQuantite() != null ? ligne.getQuantite() : 0));
                        
                        // TAXES - Calculer les taxes proportionnelles à la ligne
                        BigDecimal taxes = BigDecimal.ZERO;
                        if (facture.getTauxTVA() != null && facture.getMontantHT() != null && facture.getMontantHT().compareTo(BigDecimal.ZERO) > 0) {
                            // Calculer les taxes proportionnelles à la ligne basées sur le montant HT
                            BigDecimal montantHTLigne = ligne.getTotal() != null ? ligne.getTotal() : BigDecimal.ZERO;
                            BigDecimal montantHTTotal = facture.getMontantHT();
                            BigDecimal tauxTVA = facture.getTauxTVA();
                            // Calculer la proportion de la ligne dans le total HT
                            BigDecimal proportion = montantHTLigne.divide(montantHTTotal, 4, java.math.RoundingMode.HALF_UP);
                            // Calculer les taxes totales de la facture
                            BigDecimal taxesTotales = montantHTTotal.multiply(tauxTVA).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                            // Répartir proportionnellement
                            taxes = taxesTotales.multiply(proportion).setScale(2, java.math.RoundingMode.HALF_UP);
                        }
                        String taxesStr = formatMontant(taxes);
                        table.addCell(createCell(taxesStr, TextAlignment.RIGHT));
                        totalTaxes = totalTaxes.add(taxes);
                        
                        // CAMION
                        table.addCell(createCell(camionImmatriculation, TextAlignment.CENTER));
                        
                        // PU (Prix Unitaire)
                        String pu = ligne.getPrixUnitaire() != null ? 
                            formatMontant(ligne.getPrixUnitaire()) : "0";
                        table.addCell(createCell(pu, TextAlignment.RIGHT));
                        
                        // MONTANT
                        String montant = ligne.getTotal() != null ? 
                            formatMontant(ligne.getTotal()) : "0";
                        table.addCell(createCell(montant, TextAlignment.RIGHT));
                        totalMontant = totalMontant.add(ligne.getTotal() != null ? ligne.getTotal() : BigDecimal.ZERO);
                        
                        // RÈGLEMENT
                        String reglement = "-";
                        if (facture.getMontantPaye() != null && facture.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
                            // Calculer le règlement proportionnel à la ligne
                            BigDecimal montantTotal = facture.getMontant() != null ? facture.getMontant() : BigDecimal.ONE;
                            BigDecimal montantLigne = ligne.getTotal() != null ? ligne.getTotal() : BigDecimal.ZERO;
                            BigDecimal reglementLigne = facture.getMontantPaye()
                                .multiply(montantLigne)
                                .divide(montantTotal, 2, java.math.RoundingMode.HALF_UP);
                            reglement = formatMontant(reglementLigne);
                            totalReglement = totalReglement.add(reglementLigne);
                        }
                        table.addCell(createCell(reglement, TextAlignment.RIGHT));
                    }
                } else {
                    // Si pas de lignes, afficher juste la facture
                    String dateStr = facture.getDate() != null ? 
                        facture.getDate().format(dateFormatter) : "-";
                    table.addCell(createCell(dateStr, TextAlignment.CENTER));
                    table.addCell(createCell(facture.getStatut() != null ? facture.getStatut().name() : "-", TextAlignment.CENTER));
                    table.addCell(createCell("-", TextAlignment.LEFT));
                    table.addCell(createCell("0.00", TextAlignment.RIGHT));
                    
                    BigDecimal taxes = BigDecimal.ZERO;
                    if (facture.getTauxTVA() != null && facture.getMontantHT() != null) {
                        taxes = facture.getMontantHT()
                            .multiply(facture.getTauxTVA())
                            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    }
                    table.addCell(createCell(formatMontant(taxes), TextAlignment.RIGHT));
                    totalTaxes = totalTaxes.add(taxes);
                    
                    table.addCell(createCell(camionImmatriculation, TextAlignment.CENTER));
                    table.addCell(createCell("-", TextAlignment.RIGHT));
                    table.addCell(createCell(formatMontant(facture.getMontant() != null ? facture.getMontant() : BigDecimal.ZERO), TextAlignment.RIGHT));
                    totalMontant = totalMontant.add(facture.getMontant() != null ? facture.getMontant() : BigDecimal.ZERO);
                    
                    String reglement = "-";
                    if (facture.getMontantPaye() != null && facture.getMontantPaye().compareTo(BigDecimal.ZERO) > 0) {
                        reglement = formatMontant(facture.getMontantPaye());
                        totalReglement = totalReglement.add(facture.getMontantPaye());
                    }
                    table.addCell(createCell(reglement, TextAlignment.RIGHT));
                }
            }
            
            document.add(table);
            
            // Ligne de séparation
            document.add(new Paragraph().setMarginBottom(10));
            
            // Tableau des totaux
            float[] totalColumnWidths = {200f, 50f, 50f, 70f, 70f};
            Table totalTable = new Table(UnitValue.createPercentArray(totalColumnWidths))
                    .useAllAvailableWidth();
            
            // Ligne TOTAL
            totalTable.addCell(createCell("TOTAL", TextAlignment.LEFT).setBold());
            totalTable.addCell(createCell(String.format("%.2f", totalQuantite), TextAlignment.RIGHT).setBold());
            totalTable.addCell(createCell(formatMontant(totalTaxes), TextAlignment.RIGHT).setBold());
            totalTable.addCell(createCell(formatMontant(totalMontant), TextAlignment.RIGHT).setBold());
            totalTable.addCell(createCell(formatMontant(totalReglement), TextAlignment.RIGHT).setBold());
            
            document.add(totalTable);
            
            // Ligne NET À PAYER
            BigDecimal netAPayer = totalMontant.subtract(totalReglement);
            document.add(new Paragraph().setMarginBottom(5));
            
            Table netTable = new Table(UnitValue.createPercentArray(new float[]{100f}))
                    .useAllAvailableWidth();
            
            Cell netCell = new Cell()
                    .add(new Paragraph("NET À PAYER: " + formatMontant(netAPayer)).setBold().setFontSize(14))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(10)
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY);
            netTable.addCell(netCell);
            
            document.add(netTable);
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }
    
    private Cell createCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-"))
                .setTextAlignment(alignment)
                .setBorder(Border.NO_BORDER)
                .setPadding(5);
    }
    
    private String formatMontant(BigDecimal montant) {
        if (montant == null) {
            return "0";
        }
        return String.format("%,.0f", montant.doubleValue()).replace(",", " ");
    }
}

