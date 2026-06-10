# PPTX2PDF

A desktop app to convert, merge, and transform documents — fully offline, no cloud required.

## Features

- **PPTX to PDF** — batch convert multiple PowerPoint files at once, with slide-by-slide progress
- **DOCX to PDF** — convert Word documents to PDF
- **XLSX to PDF** — convert Excel spreadsheets to PDF
- **Images to PDF** — combine JPG and PNG images into a single PDF
- **PDF to DOCX** — extract text from PDF into a Word document
- **PDF to JPG / PNG** — render each PDF page as an image
- **Merge PDFs** — combine multiple PDF files with drag-to-reorder support
- Progress bar on all operations
- Restart button to reset and run again without relaunching
- Logging to `logs/pptx2pdf.log`
- No cloud — runs fully offline

## Requirements

- Windows
- Java 21+ ([Download Temurin JDK 21](https://adoptium.net))

> If you use the installer built with `jpackage`, Java is bundled — no separate install needed.

## Usage

1. Download `PPTX2PDF-1.2.0.exe` from [Releases](../../releases)
2. Run the installer and follow the prompts
3. Open **PPTX2PDF** from the Start Menu
4. Choose a tab:
   - **PPTX to PDF** — add one or more `.pptx` files, pick an output folder, click Convert
   - **Merge PDFs** — add PDFs, reorder with Move Up / Move Down, click Merge
   - **All Converters** — toggle your source and target format, add files, click Convert

## Supported Conversions

| From | To |
|------|----|
| PPTX | PDF |
| DOCX | PDF |
| XLSX | PDF |
| JPG / PNG | PDF |
| PDF | DOCX |
| PDF | JPG |
| PDF | PNG |

## Build from Source

```
git clone https://github.com/danveil/pptx2pdf.git
cd pptx2pdf
mvn clean package
java -jar target/pptx2pdf-1.0-SNAPSHOT.jar
```

To build a Windows installer:

```
jpackage --input target --name PPTX2PDF --main-jar pptx2pdf-1.0-SNAPSHOT.jar --main-class com.danveil.App --type exe --win-shortcut --win-menu --app-version 1.2.0 --vendor "danveil" --dest release
```

## Tech Stack

- Java 21 + Swing
- Apache POI 5.4.1 (PPTX, DOCX, XLSX reading and writing)
- Apache PDFBox 3.0.5 (PDF creation, rendering, text extraction)
- docx4j 11.4.9 (DOCX to PDF conversion)
- SLF4J + Logback (logging)

## Changelog

### v1.2.0
- Added All Converters tab with format toggle (DOCX, XLSX, Images, PDF conversions)
- Batch convert multiple files at once
- PDF to DOCX, JPG, PNG support

### v1.1.0
- Added Merge PDFs tab with reorder support
- Multi-file selection for merging

### v1.0.0
- Initial release: PPTX to PDF conversion
- Swing GUI with file picker and progress bar
- Logging to file