package com.danveil.converter;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;

public class PptxToPdfConverter {

    private static final Logger log = LoggerFactory.getLogger(PptxToPdfConverter.class);

    // progressCallback(currentSlide, totalSlides)
    public void convert(String input, String output, BiConsumer<Integer, Integer> progressCallback)
            throws IOException {

        log.info("Starting conversion: {} -> {}", input, output);

        // --- Validate input ---
        File inputFile = new File(input);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file not found: " + input);
        }
        if (!inputFile.getName().toLowerCase().endsWith(".pptx")) {
            throw new IllegalArgumentException("Input file must be a .pptx file: " + input);
        }

        // --- Validate output directory ---
        File outputFile = new File(output);
        File outputDir = outputFile.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
            log.info("Created output directory: {}", outputDir.getAbsolutePath());
        }

        XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(inputFile));
        Dimension size = ppt.getPageSize();
        int totalSlides = ppt.getSlides().size();

        log.info("Loaded PPTX: {} slides, page size {}x{}", totalSlides, size.width, size.height);

        if (totalSlides == 0) {
            ppt.close();
            throw new IllegalStateException("The PPTX file contains no slides.");
        }

        List<BufferedImage> images = renderSlides(ppt, size, totalSlides, progressCallback);
        buildPdf(images, output);

        ppt.close();
        log.info("Conversion complete: {}", output);
    }

    // Overload with no progress callback (for CLI use)
    public void convert(String input, String output) throws IOException {
        convert(input, output, (current, total) ->
                log.info("Rendering slide {}/{}", current, total));
    }

    private List<BufferedImage> renderSlides(
            XMLSlideShow ppt,
            Dimension size,
            int totalSlides,
            BiConsumer<Integer, Integer> progressCallback) {

        List<BufferedImage> images = new ArrayList<>();

        int slideNumber = 0;
        for (XSLFSlide slide : ppt.getSlides()) {
            slideNumber++;

            try {
                BufferedImage img = new BufferedImage(
                        size.width,
                        size.height,
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, size.width, size.height);

                slide.draw(g);
                g.dispose();

                images.add(img);
                log.debug("Rendered slide {}/{}", slideNumber, totalSlides);

                if (progressCallback != null) {
                    progressCallback.accept(slideNumber, totalSlides);
                }

            } catch (Exception e) {
                log.warn("Failed to render slide {}, using blank slide. Reason: {}", slideNumber, e.getMessage());

                // Insert blank slide so PDF page count still matches
                BufferedImage blank = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = blank.createGraphics();
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, size.width, size.height);
                g.dispose();
                images.add(blank);

                if (progressCallback != null) {
                    progressCallback.accept(slideNumber, totalSlides);
                }
            }
        }

        return images;
    }

    private void buildPdf(List<BufferedImage> images, String outputPath) throws IOException {

        PDDocument pdf = new PDDocument();

        for (int i = 0; i < images.size(); i++) {
            BufferedImage img = images.get(i);

            try {
                PDImageXObject pdImage = LosslessFactory.createFromImage(pdf, img);
                PDRectangle pageSize = new PDRectangle(img.getWidth(), img.getHeight());
                PDPage page = new PDPage(pageSize);
                pdf.addPage(page);

                PDPageContentStream content = new PDPageContentStream(pdf, page);
                content.drawImage(pdImage, 0, 0, img.getWidth(), img.getHeight());
                content.close();

                log.debug("Added slide {} to PDF", i + 1);

            } catch (IOException e) {
                log.error("Failed to add slide {} to PDF: {}", i + 1, e.getMessage());
                throw e;
            }
        }

        pdf.save(outputPath);
        pdf.close();
        log.info("PDF saved: {} ({} pages)", outputPath, pdf.getNumberOfPages());
    }
}