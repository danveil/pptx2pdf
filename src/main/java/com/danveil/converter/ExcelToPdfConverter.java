package com.danveil.converter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ExcelToPdfConverter {

    private static final Logger log = LoggerFactory.getLogger(ExcelToPdfConverter.class);

    public void convert(String input, String output) throws Exception {

        log.info("Converting Excel to PDF: {} -> {}", input, output);

        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("File not found: " + input);
        }

        new File(output).getParentFile().mkdirs();

        Workbook workbook = new XSSFWorkbook(new FileInputStream(inputFile));
        PDDocument pdf = new PDDocument();

        PDType1Font font      = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontBold  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        float fontSize = 9f;
        float margin   = 40f;
        float rowHeight = 16f;

        for (int si = 0; si < workbook.getNumberOfSheets(); si++) {

            Sheet sheet = workbook.getSheetAt(si);
            log.info("Processing sheet: {}", sheet.getSheetName());

            // Calculate column widths
            int maxCols = 0;
            for (Row row : sheet) {
                if (row.getLastCellNum() > maxCols) maxCols = row.getLastCellNum();
            }
            if (maxCols == 0) continue;

            float colWidth = (PDRectangle.A4.getWidth() - margin * 2) / maxCols;
            float pageHeight = PDRectangle.A4.getHeight();

            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            float y = pageHeight - margin;

            // Sheet name header
            cs.beginText();
            cs.setFont(fontBold, fontSize + 2);
            cs.newLineAtOffset(margin, y);
            cs.showText("Sheet: " + sheet.getSheetName());
            cs.endText();
            y -= rowHeight * 1.5f;

            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {

                if (y < margin + rowHeight) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    pdf.addPage(page);
                    cs = new PDPageContentStream(pdf, page);
                    y = pageHeight - margin;
                }

                for (int ci = 0; ci < maxCols; ci++) {
                    Cell cell = row.getCell(ci, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String text = formatter.formatCellValue(cell);

                    if (text != null && !text.isEmpty()) {
                        // Truncate long text to fit cell
                        if (text.length() > 20) text = text.substring(0, 18) + "..";

                        boolean isHeader = (row.getRowNum() == 0);
                        cs.beginText();
                        cs.setFont(isHeader ? fontBold : font, fontSize);
                        cs.newLineAtOffset(margin + ci * colWidth, y);
                        cs.showText(text);
                        cs.endText();
                    }
                }

                y -= rowHeight;
            }

            cs.close();
        }

        pdf.save(output);
        pdf.close();
        workbook.close();

        log.info("Excel to PDF complete: {}", output);
    }
}