package com.danveil.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiConsumer;

public class PdfToImageConverter {

    private static final Logger log = LoggerFactory.getLogger(PdfToImageConverter.class);

    public void convert(String input, String outputDir, String format,
                        BiConsumer<Integer, Integer> progress) throws Exception {

        log.info("Converting PDF to {}: {}", format.toUpperCase(), input);

        File inputFile = new File(input);
        if (!inputFile.exists()) throw new IllegalArgumentException("File not found: " + input);

        new File(outputDir).mkdirs();

        // PDFBox 3.x uses Loader.loadPDF()
        PDDocument pdf = Loader.loadPDF(inputFile);
        PDFRenderer renderer = new PDFRenderer(pdf);
        int total = pdf.getNumberOfPages();

        String baseName = inputFile.getName().replaceAll("(?i)\\.pdf$", "");

        for (int i = 0; i < total; i++) {
            BufferedImage img = renderer.renderImageWithDPI(i, 150);
            String outPath = outputDir + File.separator + baseName + "_page" + (i + 1) + "." + format.toLowerCase();
            ImageIO.write(img, format.toUpperCase(), new File(outPath));
            log.info("Saved page {}: {}", i + 1, outPath);

            if (progress != null) progress.accept(i + 1, total);
        }

        pdf.close();
        log.info("PDF to {} complete. {} pages saved to: {}", format.toUpperCase(), total, outputDir);
    }
}