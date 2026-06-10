package com.danveil.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class PdfToDocxConverter {

    private static final Logger log = LoggerFactory.getLogger(PdfToDocxConverter.class);

    public void convert(String input, String output) throws Exception {

        log.info("Converting PDF to DOCX: {} -> {}", input, output);

        File inputFile = new File(input);
        if (!inputFile.exists()) throw new IllegalArgumentException("File not found: " + input);

        File outputFile = new File(output);
        if (outputFile.getParentFile() != null) outputFile.getParentFile().mkdirs();

        // PDFBox 3.x uses Loader.loadPDF() instead of PDDocument.load()
        PDDocument pdf = Loader.loadPDF(inputFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdf);
        pdf.close();

        XWPFDocument doc = new XWPFDocument();

        for (String line : text.split("\\r?\\n")) {
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setFontFamily("Calibri");
            run.setFontSize(11);
            run.setText(line);
        }

        FileOutputStream fos = new FileOutputStream(output);
        doc.write(fos);
        fos.close();
        doc.close();

        log.info("PDF to DOCX complete: {}", output);
    }
}