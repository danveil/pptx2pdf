package com.danveil.ui;

import com.danveil.converter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class ConverterPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(ConverterPanel.class);

    // Format definitions
    private static final String PPTX = "PPTX";
    private static final String DOCX = "DOCX";
    private static final String XLSX = "XLSX";
    private static final String PDF  = "PDF";
    private static final String JPG  = "JPG";
    private static final String PNG  = "PNG";

    // Valid conversions: from -> list of valid targets
    private static final Map<String, List<String>> VALID_TARGETS = new LinkedHashMap<>();
    static {
        VALID_TARGETS.put(PPTX, List.of(PDF));
        VALID_TARGETS.put(DOCX, List.of(PDF));
        VALID_TARGETS.put(XLSX, List.of(PDF));
        VALID_TARGETS.put(PDF,  List.of(DOCX, JPG, PNG, "MD"));
        VALID_TARGETS.put(JPG,  List.of(PDF));
        VALID_TARGETS.put(PNG,  List.of(PDF));
    }

    private static final Map<String, String[]> FILE_FILTERS = new HashMap<>();
    static {
        FILE_FILTERS.put(PPTX, new String[]{"PowerPoint Files (*.pptx)", "pptx"});
        FILE_FILTERS.put(DOCX, new String[]{"Word Files (*.docx)", "docx"});
        FILE_FILTERS.put(XLSX, new String[]{"Excel Files (*.xlsx)", "xlsx"});
        FILE_FILTERS.put(PDF,  new String[]{"PDF Files (*.pdf)", "pdf"});
        FILE_FILTERS.put(JPG,  new String[]{"JPEG Images (*.jpg)", "jpg", "jpeg"});
        FILE_FILTERS.put(PNG,  new String[]{"PNG Images (*.png)", "png"});
    }

    // UI components
    private final ButtonGroup fromGroup = new ButtonGroup();
    private final ButtonGroup toGroup   = new ButtonGroup();
    private final Map<String, JToggleButton> fromButtons = new LinkedHashMap<>();
    private final Map<String, JToggleButton> toButtons   = new LinkedHashMap<>();

    private DefaultListModel<String> fileListModel;
    private JList<String> fileList;
    private JTextField outputDirField;
    private JButton convertButton;
    private JProgressBar progressBar;
    private JLabel statusLabel;

    private String selectedFrom = PPTX;
    private String selectedTo   = PDF;

    public ConverterPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        initComponents();
        updateToButtons();
    }

    private void initComponents() {

        // ── Format selector at top ──
        JPanel formatPanel = new JPanel(new GridBagLayout());
        formatPanel.setBorder(BorderFactory.createTitledBorder("Conversion Format"));
        GridBagConstraints fc = new GridBagConstraints();
        fc.insets = new Insets(4, 4, 4, 4);

        // FROM label + buttons
        fc.gridx = 0; fc.gridy = 0;
        formatPanel.add(new JLabel("From:"), fc);

        JPanel fromPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (String fmt : VALID_TARGETS.keySet()) {
            JToggleButton btn = makeToggleButton(fmt);
            btn.addActionListener(e -> {
                selectedFrom = fmt;
                updateToButtons();
                clearFiles();
            });
            fromButtons.put(fmt, btn);
            fromGroup.add(btn);
            fromPanel.add(btn);
        }
        // Select first by default
        fromButtons.get(PPTX).setSelected(true);

        fc.gridx = 1; fc.weightx = 1; fc.fill = GridBagConstraints.HORIZONTAL;
        formatPanel.add(fromPanel, fc);

        // TO label + buttons
        fc.gridx = 0; fc.gridy = 1; fc.weightx = 0; fc.fill = GridBagConstraints.NONE;
        formatPanel.add(new JLabel("To:"), fc);

        JPanel toPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (String fmt : new String[]{PDF, DOCX, JPG, PNG, "MD"}) {
            JToggleButton btn = makeToggleButton(fmt);
            btn.addActionListener(e -> {
                selectedTo = fmt;
            });
            toButtons.put(fmt, btn);
            toGroup.add(btn);
            toPanel.add(btn);
        }

        fc.gridx = 1; fc.weightx = 1; fc.fill = GridBagConstraints.HORIZONTAL;
        formatPanel.add(toPanel, fc);

        add(formatPanel, BorderLayout.NORTH);

        // ── File list in center ──
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(BorderFactory.createTitledBorder("Files to Convert"));
        add(scroll, BorderLayout.CENTER);

        // ── Right: list buttons ──
        JPanel listButtons = new JPanel(new GridLayout(2, 1, 0, 6));
        listButtons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton addBtn    = new JButton("Add Files");
        JButton removeBtn = new JButton("Remove");

        addBtn.addActionListener(e -> addFiles());
        removeBtn.addActionListener(e -> {
            int idx = fileList.getSelectedIndex();
            if (idx != -1) { fileListModel.remove(idx); refreshConvertButton(); }
        });

        listButtons.add(addBtn);
        listButtons.add(removeBtn);
        add(listButtons, BorderLayout.EAST);

        // ── Bottom: output + progress + buttons ──
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        bottomPanel.add(new JLabel("Output Folder:"), c);

        outputDirField = new JTextField(28);
        outputDirField.setEditable(false);
        c.gridx = 1; c.weightx = 1;
        bottomPanel.add(outputDirField, c);

        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> browseOutputDir());
        c.gridx = 2; c.weightx = 0;
        bottomPanel.add(browseBtn, c);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3;
        bottomPanel.add(progressBar, c);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        convertButton = new JButton("Convert");
        convertButton.setEnabled(false);
        convertButton.addActionListener(e -> startConvert());

        JButton restartBtn = new JButton("Restart");
        restartBtn.addActionListener(e -> resetPanel());

        actionRow.add(convertButton);
        actionRow.add(restartBtn);

        c.gridy = 2; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.CENTER;
        bottomPanel.add(actionRow, c);

        statusLabel = new JLabel("Select a format and add files to begin.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        c.gridy = 3;
        bottomPanel.add(statusLabel, c);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JToggleButton makeToggleButton(String label) {
        JToggleButton btn = new JToggleButton(label);
        btn.setPreferredSize(new Dimension(60, 28));
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        return btn;
    }

    private void updateToButtons() {
        List<String> validTargets = VALID_TARGETS.get(selectedFrom);

        // Reset all TO buttons
        toGroup.clearSelection();
        for (Map.Entry<String, JToggleButton> entry : toButtons.entrySet()) {
            boolean valid = validTargets.contains(entry.getKey());
            entry.getValue().setEnabled(valid);
            entry.getValue().setSelected(false);
        }

        // Auto-select first valid target
        if (!validTargets.isEmpty()) {
            selectedTo = validTargets.get(0);
            JToggleButton first = toButtons.get(selectedTo);
            if (first != null) first.setSelected(true);
        }

        refreshConvertButton();
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        String[] filterInfo = FILE_FILTERS.get(selectedFrom);

        // Build extension array (skip index 0 which is description)
        String[] exts = Arrays.copyOfRange(filterInfo, 1, filterInfo.length);
        chooser.setFileFilter(new FileNameExtensionFilter(filterInfo[0], exts));
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select " + selectedFrom + " files");

        // Special case: images can be both JPG and PNG when converting images
        if (selectedFrom.equals(JPG) || selectedFrom.equals(PNG)) {
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Image Files (*.jpg, *.png)", "jpg", "jpeg", "png"));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                fileListModel.addElement(f.getAbsolutePath());
            }

            if (outputDirField.getText().isEmpty() && fileListModel.size() > 0) {
                File first = new File(fileListModel.get(0));
                outputDirField.setText(first.getParent());
            }

            refreshConvertButton();
        }
    }

    private void browseOutputDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select output folder");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            refreshConvertButton();
        }
    }

    private void refreshConvertButton() {
        boolean ready = fileListModel.size() >= 1 && !outputDirField.getText().isEmpty();
        convertButton.setEnabled(ready);
        if (fileListModel.size() == 0) {
            statusLabel.setText("Add files to begin.");
            statusLabel.setForeground(Color.GRAY);
        } else {
            statusLabel.setText(fileListModel.size() + " file(s) queued — " + selectedFrom + " → " + selectedTo);
            statusLabel.setForeground(Color.GRAY);
        }
    }

    private void clearFiles() {
        fileListModel.clear();
        outputDirField.setText("");
        refreshConvertButton();
    }

    private void startConvert() {
        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) inputs.add(fileListModel.get(i));

        String outputDir  = outputDirField.getText().trim();
        String from       = selectedFrom;
        String to         = selectedTo;
        int total         = inputs.size();

        convertButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting...");
        statusLabel.setText("Converting " + from + " → " + to + "...");
        statusLabel.setForeground(Color.BLUE);

        SwingWorker<Void, int[]> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {

                for (int i = 0; i < inputs.size(); i++) {
                    String input    = inputs.get(i);
                    String baseName = new File(input).getName();
                    baseName        = baseName.substring(0, baseName.lastIndexOf('.'));
                    final int idx   = i + 1;

                    switch (from + "_" + to) {

                        case "PPTX_PDF" -> {
                            String out = outputDir + File.separator + baseName + ".pdf";
                            new PptxToPdfConverter().convert(input, out, (cur, tot) ->
                                    publish(new int[]{idx, total,
                                            (int)(((idx - 1 + cur / (double) tot) / total) * 100)}));
                        }

                        case "DOCX_PDF" -> {
                            String out = outputDir + File.separator + baseName + ".pdf";
                            new DocxToPdfConverter().convert(input, out);
                            publish(new int[]{idx, total, (int)((idx / (double) total) * 100)});
                        }

                        case "XLSX_PDF" -> {
                            String out = outputDir + File.separator + baseName + ".pdf";
                            new ExcelToPdfConverter().convert(input, out);
                            publish(new int[]{idx, total, (int)((idx / (double) total) * 100)});
                        }

                        case "JPG_PDF", "PNG_PDF" -> {
                            // Collect all images into one PDF
                            if (idx == 1) {
                                String out = outputDir + File.separator + "images_combined.pdf";
                                new ImageToPdfConverter().convert(inputs, out);
                                publish(new int[]{total, total, 100});
                                return null; // handled all at once
                            }
                        }

                        case "PDF_JPG", "PDF_PNG" -> {
                            String format = to.toLowerCase();
                            String outDir = outputDir + File.separator + baseName + "_pages";
                            new PdfToImageConverter().convert(input, outDir, format, (cur, tot) ->
                                    publish(new int[]{idx, total,
                                            (int)(((idx - 1 + cur / (double) tot) / total) * 100)}));
                        }

                        case "PDF_DOCX" -> {
                            String out = outputDir + File.separator + baseName + ".docx";
                            new PdfToDocxConverter().convert(input, out);
                            publish(new int[]{idx, total, (int)((idx / (double) total) * 100)});
                        }

                        case "PDF_MD" -> {
                            String out = outputDir + File.separator + baseName + ".md";
                            new PdfToMarkdownConverter().convert(input, out);
                            publish(new int[]{idx, total, (int)((idx / (double) total) * 100)});
}

                        default -> throw new IllegalArgumentException(
                                "Unsupported conversion: " + from + " → " + to);
                    }
                }
                return null;
            }

            @Override
            protected void process(List<int[]> chunks) {
                int[] latest = chunks.get(chunks.size() - 1);
                int fileIdx  = latest[0];
                int fileTot  = latest[1];
                int percent  = latest[2];

                progressBar.setValue(percent);
                progressBar.setString("File " + fileIdx + "/" + fileTot);
                statusLabel.setText("Converting file " + fileIdx + " of " + fileTot + "...");
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressBar.setValue(100);
                    progressBar.setString("Complete!");
                    statusLabel.setText("Done! Files saved to: " + outputDir);
                    statusLabel.setForeground(new Color(0, 140, 0));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    progressBar.setString("Failed");
                    statusLabel.setText("Error: " + msg);
                    statusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(ConverterPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                    log.error("Conversion failed: {}", msg);
                } finally {
                    convertButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void resetPanel() {
        fileListModel.clear();
        outputDirField.setText("");
        progressBar.setValue(0);
        progressBar.setString("Idle");
        statusLabel.setText("Select a format and add files to begin.");
        statusLabel.setForeground(Color.GRAY);
        convertButton.setEnabled(false);

        // Reset toggles to default
        fromButtons.get(PPTX).setSelected(true);
        selectedFrom = PPTX;
        updateToButtons();
        log.info("Converter panel reset.");
    }
}