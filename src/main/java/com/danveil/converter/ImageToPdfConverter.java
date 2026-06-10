package com.danveil.converter;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ImageToPdfConverter {

    private static final Logger log = LoggerFactory.getLogger(ImageToPdfConverter.class);

    // Convert multiple images into one PDF (one image per page)
    public void convert(List<String> imagePaths, String output) throws Exception {

        log.info("Converting {} image(s) to PDF: {}", imagePaths.size(), output);

        if (imagePaths == null || imagePaths.isEmpty()) {
            throw new IllegalArgumentException("No image files provided.");
        }

        new File(output).getParentFile().mkdirs();

        PDDocument pdf = new PDDocument();

        for (String imgPath : imagePaths) {
            File imgFile = new File(imgPath);
            if (!imgFile.exists()) {
                log.warn("Image not found, skipping: {}", imgPath);
                continue;
            }

            BufferedImage bImg = ImageIO.read(imgFile);
            if (bImg == null) {
                log.warn("Could not read image, skipping: {}", imgPath);
                continue;
            }

            PDImageXObject pdImage = PDImageXObject.createFromFile(imgPath, pdf);
            PDRectangle pageSize   = new PDRectangle(bImg.getWidth(), bImg.getHeight());
            PDPage page            = new PDPage(pageSize);
            pdf.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(pdf, page);
            cs.drawImage(pdImage, 0, 0, bImg.getWidth(), bImg.getHeight());
            cs.close();

            log.info("Added image: {}", imgPath);
        }

        pdf.save(output);
        pdf.close();

        log.info("Image to PDF complete: {}", output);
    }
}