package com.gymai.back.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class PdfService {

    // Colores del tema
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);      // Azul
    private static final Color SECONDARY_COLOR = new Color(59, 130, 246);   // Azul claro
    private static final Color ACCENT_COLOR = new Color(16, 185, 129);      // Verde
    private static final Color HEADER_BG = new Color(30, 41, 59);           // Slate oscuro
    private static final Color SECTION_BG = new Color(241, 245, 249);       // Slate claro
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);

    // Fuentes
    private Font getTitleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, Font.BOLD, Color.WHITE);
    }

    private Font getSubtitleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, new Color(148, 163, 184));
    }

    private Font getSectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.BOLD, PRIMARY_COLOR);
    }

    private Font getSubsectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Font.BOLD, TEXT_DARK);
    }

    private Font getBodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
    }

    private Font getBoldBodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, TEXT_DARK);
    }
    
    private Font getTableHeaderFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, Color.WHITE);
    }

    private Font getSmallFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, TEXT_MUTED);
    }

    public byte[] generatePlanPdf(String title, String content) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document, title);

            if (content != null) {
                parseAndAddContent(document, content);
            }

            addFooter(document);

        } catch (DocumentException e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private void addHeader(Document document, String title) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(HEADER_BG);
        headerCell.setPadding(20f);
        headerCell.setBorder(Rectangle.NO_BORDER);
        
        Paragraph titleP = new Paragraph();
        titleP.add(new Phrase("🏋️ GymAI", getTitleFont()));
        titleP.setAlignment(Element.ALIGN_CENTER);
        headerCell.addElement(titleP);
        
        String subtitle = title != null && title.toLowerCase().contains("dieta") 
            ? "Plan de Alimentación Personalizado" 
            : "Plan de Entrenamiento Personalizado";
        Paragraph subtitleP = new Paragraph(subtitle, getSubtitleFont());
        subtitleP.setAlignment(Element.ALIGN_CENTER);
        subtitleP.setSpacingBefore(8f);
        headerCell.addElement(subtitleP);
        
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy"));
        Paragraph fechaP = new Paragraph("Generado el " + fecha, getSmallFont());
        fechaP.setAlignment(Element.ALIGN_CENTER);
        fechaP.setSpacingBefore(10f);
        headerCell.addElement(fechaP);
        
        headerTable.addCell(headerCell);
        headerTable.setSpacingAfter(20f);
        document.add(headerTable);
    }

    private void parseAndAddContent(Document document, String content) throws DocumentException {
        String normalized = normalizeContent(content);
        String[] lines = normalized.split("\r?\n");
        
        List<String> tableBuffer = new ArrayList<>();
        boolean inTable = false;
        
        for (String rawLine : lines) {
            String line = rawLine.trim();
            
            if (line.isEmpty()) continue;
            
            // Manejo de tablas
            if (line.startsWith("|")) {
                inTable = true;
                tableBuffer.add(line);
                continue;
            } else if (inTable) {
                renderTable(document, tableBuffer);
                tableBuffer.clear();
                inTable = false;
            }
            
            // Procesar línea individual
            processLine(document, line);
        }
        
        // Tabla pendiente al final
        if (inTable && !tableBuffer.isEmpty()) {
            renderTable(document, tableBuffer);
        }
    }
    
    private String normalizeContent(String content) {
        return content
            .replace("```markdown", "")
            .replace("```", "")
            .replace("**", "")
            .replace("•", "\n- ");
    }
    
    private void processLine(Document document, String line) throws DocumentException {
        if (line.startsWith("## ")) {
            String headingText = line.substring(3).trim();
            addSectionHeader(document, headingText);
        } else if (line.startsWith("### ")) {
            String subheadingText = line.substring(4).trim();
            addSubsectionHeader(document, subheadingText);
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            String itemText = line.substring(2).trim();
            addListItem(document, itemText);
        } else if (line.matches("^\\d+\\.\\s.*")) {
            int dotIndex = line.indexOf(".");
            String itemText = line.substring(dotIndex + 1).trim();
            String number = line.substring(0, dotIndex);
            addNumberedItem(document, number, itemText);
        } else {
            addNormalText(document, line);
        }
    }
    
    private void addSubsectionHeader(Document document, String text) throws DocumentException {
        Paragraph subheading = new Paragraph(text, getSubsectionFont());
        subheading.setSpacingBefore(12f);
        subheading.setSpacingAfter(6f);
        document.add(subheading);
    }
    
    private void addNormalText(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, getBodyFont());
        p.setSpacingAfter(6f);
        document.add(p);
    }

    private void renderTable(Document document, List<String> markdownLines) throws DocumentException {
        if (markdownLines.isEmpty()) return;

        // Analizar cabecera para saber número de columnas
        String headerLine = markdownLines.get(0);
        // | Col 1 | Col 2 | -> ["", " Col 1 ", " Col 2 ", ""]
        String[] headers = Arrays.stream(headerLine.split("\\|"))
                                 .map(String::trim)
                                 .filter(s -> !s.isEmpty())
                                 .toArray(String[]::new);
        
        int numCols = headers.length;
        if (numCols == 0) return;

        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Renderizar Headers
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, getTableHeaderFont()));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(8f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        // Procesar filas (saltando la segunda línea si es separador |---|)
        for (int i = 1; i < markdownLines.size(); i++) {
            String rowLine = markdownLines.get(i);
            if (rowLine.replaceAll("[|\\-\\s:]", "").isEmpty()) {
                continue; // Es la línea separadora
            }

            String[] cells = Arrays.stream(rowLine.split("\\|"))
                                   // Nota: split("|") deja un string vacío al inicio si la línea empieza por |
                                   // así que filtramos con cuidado o usamos lógica manual si es complejo
                                   .map(String::trim)
                                   .toArray(String[]::new);
            
            // Reconstrucción manual simple para evitar líos con split
            List<String> cellValues = new ArrayList<>();
            String temp = rowLine;
            if (temp.startsWith("|")) temp = temp.substring(1);
            if (temp.endsWith("|")) temp = temp.substring(0, temp.length() - 1);
            
            String[] rawValues = temp.split("\\|");
            for (String val : rawValues) {
                cellValues.add(val.trim());
            }
            
            // Rellenar celdas
            for (int c = 0; c < numCols; c++) {
                String text = (c < cellValues.size()) ? cellValues.get(c) : "";
                PdfPCell cell = new PdfPCell(new Phrase(text, getBodyFont()));
                cell.setPadding(6f);
                if (i % 2 == 0) {
                    cell.setBackgroundColor(new Color(248, 250, 252));
                }
                table.addCell(cell);
            }
        }

        document.add(table);
    }

    private void addSectionHeader(Document document, String text) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(15f);
        sectionTable.setSpacingAfter(10f);
        
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(SECTION_BG);
        cell.setPadding(12f);
        cell.setBorderWidth(0);
        cell.setBorderWidthLeft(4f);
        cell.setBorderColorLeft(PRIMARY_COLOR);
        
        Paragraph p = new Paragraph(text, getSectionFont());
        cell.addElement(p);
        sectionTable.addCell(cell);
        
        document.add(sectionTable);
    }

    private void addListItem(Document document, String itemText) throws DocumentException {
        int colonIndex = itemText.indexOf(":");
        if (colonIndex > 0 && colonIndex < 40) {
            String label = itemText.substring(0, colonIndex).trim();
            String value = itemText.substring(colonIndex + 1).trim();
            
            PdfPTable table = new PdfPTable(new float[]{35f, 65f});
            table.setWidthPercentage(100);
            table.setSpacingAfter(4f);
            
            PdfPCell labelCell = new PdfPCell(new Phrase(label, getBoldBodyFont()));
            labelCell.setBackgroundColor(new Color(249, 250, 251));
            labelCell.setPadding(8f);
            labelCell.setBorderWidth(0.5f);
            labelCell.setBorderColor(new Color(229, 231, 235));
            
            PdfPCell valueCell = new PdfPCell(new Phrase(value, getBodyFont()));
            valueCell.setPadding(8f);
            valueCell.setBorderWidth(0.5f);
            valueCell.setBorderColor(new Color(229, 231, 235));
            
            table.addCell(labelCell);
            table.addCell(valueCell);
            document.add(table);
        } else {
            PdfPTable bulletTable = new PdfPTable(new float[]{5f, 95f});
            bulletTable.setWidthPercentage(100);
            bulletTable.setSpacingAfter(3f);
            
            PdfPCell bulletCell = new PdfPCell(new Phrase("●", FontFactory.getFont(FontFactory.HELVETICA, 8, ACCENT_COLOR)));
            bulletCell.setBorder(Rectangle.NO_BORDER);
            bulletCell.setPaddingTop(3f);
            bulletCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            PdfPCell textCell = new PdfPCell(new Phrase(itemText, getBodyFont()));
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setPaddingLeft(5f);
            
            bulletTable.addCell(bulletCell);
            bulletTable.addCell(textCell);
            document.add(bulletTable);
        }
    }

    private void addNumberedItem(Document document, String number, String itemText) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{8f, 92f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(4f);
        
        PdfPCell numCell = new PdfPCell();
        numCell.setBackgroundColor(PRIMARY_COLOR);
        numCell.setPadding(6f);
        numCell.setBorder(Rectangle.NO_BORDER);
        numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        numCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        Paragraph numP = new Paragraph(number, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE));
        numP.setAlignment(Element.ALIGN_CENTER);
        numCell.addElement(numP);
        
        PdfPCell textCell = new PdfPCell(new Phrase(itemText, getBodyFont()));
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPaddingLeft(10f);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        table.addCell(numCell);
        table.addCell(textCell);
        document.add(table);
    }

    private void addFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(30f);
        footer.setAlignment(Element.ALIGN_CENTER);
        
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(60);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorder(Rectangle.TOP);
        lineCell.setBorderColorTop(new Color(203, 213, 225));
        lineCell.setBorderWidthTop(1f);
        lineCell.setFixedHeight(1f);
        line.addCell(lineCell);
        line.setSpacingAfter(15f);
        document.add(line);
        
        Paragraph footerText = new Paragraph(
            "Generado por GymAI - Tu asistente de entrenamiento con IA\n" +
            "Este plan es orientativo. Consulta con un profesional antes de iniciar cualquier programa.",
            getSmallFont()
        );
        footerText.setAlignment(Element.ALIGN_CENTER);
        document.add(footerText);
    }
}
