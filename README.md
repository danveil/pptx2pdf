# PPTX2PDF

A simple desktop app to convert PowerPoint (.pptx) files to PDF.

## Features
- Drag-and-drop file picker
- Slide-by-slide progress bar
- Logging to `logs/pptx2pdf.log`
- No cloud — runs fully offline

## Requirements
- Windows
- Java 21+ ([Download](https://adoptium.net))

## Usage
1. Download `PPTX2PDF.exe` from [Releases](../../releases)
2. Run it — no installation needed
3. Pick your `.pptx`, pick output location, click Convert

## Build from Source
\`\`\`
mvn clean package
java -jar target/pptx2pdf-1.0-SNAPSHOT.jar
\`\`\`

## Tech Stack
- Java 21 + Swing
- Apache POI (PPTX reading)
- Apache PDFBox (PDF writing)
- SLF4J + Logback (logging)