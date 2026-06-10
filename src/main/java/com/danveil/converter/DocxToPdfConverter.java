package com.danveil.converter;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

public class DocxToPdfConverter {

    private static final Logger log = LoggerFactory.getLogger(DocxToPdfConverter.class);

    public void convert(String input, String output) throws Exception {

        log.info("Converting DOCX to PDF: {} -> {}", input, output);

        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("File not found: " + input);
        }

        File outputFile = new File(output);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        WordprocessingMLPackage wordPackage =
                WordprocessingMLPackage.load(inputFile);

        FileOutputStream os = new FileOutputStream(outputFile);
        Docx4J.toPDF(wordPackage, os);
        os.close();

        log.info("DOCX to PDF complete: {}", output);
    }
}