package com.gymai.back.service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PdfService {

    public byte[] generatePlanPdf(String title, String content) {
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            // Título principal
            if (title != null && !title.isBlank()) {
                Paragraph titleP = new Paragraph(title, titleFont);
                titleP.setSpacingAfter(12f);
                document.add(titleP);
            }

            if (content != null) {
                String normalized = content;
                // quitamos marcas de negrita markdown
                normalized = normalized.replace("**", "");
                // cada punto "•" lo convertimos en un salto de línea con viñeta markdown simple
                normalized = normalized.replace("•", "\n* ");
                String[] lines = normalized.split("\r?\n");
                for (String rawLine : lines) {
                    String line = rawLine.trim();
                    if (line.isEmpty()) {
                        // línea en blanco para espaciado
                        Paragraph spacer = new Paragraph(" ", bodyFont);
                        spacer.setSpacingAfter(4f);
                        document.add(spacer);
                        continue;
                    }

                    if (line.startsWith("## ")) {
                        // cabecera de sección estilo Markdown
                        String headingText = line.substring(3).trim();
                        Paragraph heading = new Paragraph(headingText, headingFont);
                        heading.setSpacingBefore(8f);
                        heading.setSpacingAfter(4f);
                        document.add(heading);
                    } else if (line.startsWith("* ")) {
                        // elemento de lista simple; si tiene forma "Etiqueta: valor" lo
                        // mostramos como tabla de 2 columnas para que quede más visual
                        String itemText = line.substring(2).trim();
                        int colonIndex = itemText.indexOf(":");
                        if (colonIndex > 0) {
                            String label = itemText.substring(0, colonIndex).trim();
                            String value = itemText.substring(colonIndex + 1).trim();
                            PdfPTable table = new PdfPTable(2);
                            table.setWidthPercentage(100f);
                            PdfPCell labelCell = new PdfPCell(new Phrase(label, bodyFont));
                            labelCell.setBackgroundColor(new Color(240, 240, 240));
                            labelCell.setPadding(4f);
                            PdfPCell valueCell = new PdfPCell(new Phrase(value, bodyFont));
                            valueCell.setPadding(4f);
                            table.addCell(labelCell);
                            table.addCell(valueCell);
                            table.setSpacingAfter(3f);
                            document.add(table);
                        } else {
                            Paragraph bullet = new Paragraph("• " + itemText, bodyFont);
                            bullet.setFirstLineIndent(10f);
                            bullet.setSpacingAfter(2f);
                            document.add(bullet);
                        }
                    } else {
                        Paragraph p = new Paragraph(line, bodyFont);
                        p.setSpacingAfter(2f);
                        document.add(p);
                    }
                }
            }
        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }
}
