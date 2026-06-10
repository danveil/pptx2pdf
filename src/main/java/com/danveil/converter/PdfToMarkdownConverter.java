package com.danveil.converter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class PdfToMarkdownConverter {

    private static final Logger log = LoggerFactory.getLogger(PdfToMarkdownConverter.class);

    public void convert(String input, String output) throws Exception {

        log.info("Converting PDF to Markdown: {} -> {}", input, output);

        File inputFile = new File(input);
        if (!inputFile.exists()) throw new IllegalArgumentException("File not found: " + input);

        File outputFile = new File(output);
        if (outputFile.getParentFile() != null) outputFile.getParentFile().mkdirs();

        PDDocument pdf = Loader.loadPDF(inputFile);

        // Custom stripper that tries to detect headings by font size
        PDFTextStripper stripper = new PDFTextStripper() {

            float lastFontSize = 0f;

            @Override
            protected void writeString(String text, List<TextPosition> positions) throws IOException {

                if (positions == null || positions.isEmpty()) {
                    super.writeString(text, positions);
                    return;
                }

                float fontSize = positions.get(0).getFontSizeInPt();

                // Heuristic: large text = heading, normal = paragraph
                if (fontSize >= 18f && !text.trim().isEmpty()) {
                    super.writeString("# " + text, positions);
                } else if (fontSize >= 14f && !text.trim().isEmpty()) {
                    super.writeString("## " + text, positions);
                } else if (fontSize >= 12f && !text.trim().isEmpty()) {
                    super.writeString("### " + text, positions);
                } else {
                    super.writeString(text, positions);
                }

                lastFontSize = fontSize;
            }
        };

        stripper.setSortByPosition(true);
        String rawText = stripper.getText(pdf);
        pdf.close();

        // Post-process: clean up and format as markdown
        StringBuilder md = new StringBuilder();
        String[] lines = rawText.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                // Collapse multiple blank lines into one
                if (md.length() > 0 && !md.toString().endsWith("\n\n")) {
                    md.append("\n");
                }
                continue;
            }

            // Detect bullet-like lines (starting with -, *, •, or a number+dot)
            if (line.matches("^[-*•]\\s+.*")) {
                md.append("- ").append(line.replaceFirst("^[-*•]\\s+", "")).append("\n");
                continue;
            }

            if (line.matches("^\\d+[.)].+")) {
                md.append(line).append("\n");
                continue;
            }

            md.append(line).append("\n");
        }

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(md.toString());
        }

        log.info("PDF to Markdown complete: {}", output);
    }
}