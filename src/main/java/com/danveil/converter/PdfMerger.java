package com.danveil.converter;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfMerger {

    private static final Logger log = LoggerFactory.getLogger(PdfMerger.class);

    public void merge(List<String> inputPaths, String outputPath) throws IOException {

        if (inputPaths == null || inputPaths.size() < 2) {
            throw new IllegalArgumentException("At least 2 PDF files are required to merge.");
        }

        // Validate all input files exist
        for (String path : inputPaths) {
            File f = new File(path);
            if (!f.exists()) {
                throw new IllegalArgumentException("File not found: " + path);
            }
            if (!f.getName().toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("Not a PDF file: " + path);
            }
        }

        // Validate output directory
        File outputFile = new File(outputPath);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
            log.info("Created output directory: {}", outputDir.getAbsolutePath());
        }

        log.info("Merging {} PDF files into: {}", inputPaths.size(), outputPath);

        PDFMergerUtility merger = new PDFMergerUtility();

        for (String path : inputPaths) {
            merger.addSource(new File(path));
            log.info("Added: {}", path);
        }

        merger.setDestinationFileName(outputPath);
        merger.mergeDocuments(null);

        log.info("Merge complete: {}", outputPath);
    }
}