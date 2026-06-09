package com.danveil.ui;

import com.danveil.converter.PptxToPdfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class MainFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);

    private JTextField inputField;
    private JTextField outputField;
    private JButton convertButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public MainFrame() {
        setTitle("PPTX2PDF Converter");
        setSize(580, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("PPTX to PDF", buildConvertPanel());
        tabs.addTab("Merge PDFs", new MergePanel());

        add(tabs);
    }

    private JPanel buildConvertPanel() {

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        panel.add(new JLabel("Input PPTX:"), c);

        inputField = new JTextField(28);
        inputField.setEditable(false);
        c.gridx = 1; c.weightx = 1;
        panel.add(inputField, c);

        JButton browseInput = new JButton("Browse");
        browseInput.addActionListener(e -> browseInput());
        c.gridx = 2; c.weightx = 0;
        panel.add(browseInput, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        panel.add(new JLabel("Output PDF:"), c);

        outputField = new JTextField(28);
        outputField.setEditable(false);
        c.gridx = 1; c.weightx = 1;
        panel.add(outputField, c);

        JButton browseOutput = new JButton("Browse");
        browseOutput.addActionListener(e -> browseOutput());
        c.gridx = 2; c.weightx = 0;
        panel.add(browseOutput, c);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        c.gridx = 0; c.gridy = 2;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(progressBar, c);

        convertButton = new JButton("Convert to PDF");
        convertButton.setEnabled(false);
        convertButton.addActionListener(e -> startConvert());
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(convertButton, c);

        statusLabel = new JLabel("Select a PPTX file to begin.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        c.gridy = 4;
        panel.add(statusLabel, c);

        return panel;
    }

    private void browseInput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PowerPoint Files (*.pptx)", "pptx"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            inputField.setText(selected.getAbsolutePath());

            String autoOutput = selected.getAbsolutePath()
                    .replaceAll("(?i)\\.pptx$", ".pdf");
            outputField.setText(autoOutput);

            convertButton.setEnabled(true);
            statusLabel.setText("Ready to convert.");
            statusLabel.setForeground(Color.GRAY);
            progressBar.setValue(0);
            progressBar.setString("Idle");
            log.info("Input selected: {}", selected.getAbsolutePath());
        }
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        chooser.setDialogTitle("Save PDF as...");

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.toLowerCase().endsWith(".pdf")) {
                path += ".pdf";
            }
            outputField.setText(path);
            log.info("Output selected: {}", path);
        }
    }

    private void startConvert() {
        String input  = inputField.getText().trim();
        String output = outputField.getText().trim();

        if (input.isEmpty() || output.isEmpty()) {
            showError("Please select input and output files.");
            return;
        }

        convertButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Starting...");
        statusLabel.setText("Converting...");
        statusLabel.setForeground(Color.BLUE);

        log.info("Conversion started: {} -> {}", input, output);

        SwingWorker<Void, int[]> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                PptxToPdfConverter converter = new PptxToPdfConverter();
                converter.convert(input, output, (current, total) -> {
                    publish(new int[]{current, total});
                });
                return null;
            }

            @Override
            protected void process(java.util.List<int[]> chunks) {
                int[] latest = chunks.get(chunks.size() - 1);
                int current = latest[0];
                int total   = latest[1];
                int percent = (int) ((current / (double) total) * 100);

                progressBar.setValue(percent);
                progressBar.setString("Slide " + current + " / " + total);
                statusLabel.setText("Rendering slide " + current + " of " + total + "...");
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressBar.setValue(100);
                    progressBar.setString("Complete!");
                    statusLabel.setText("Done! Saved to: " + output);
                    statusLabel.setForeground(new Color(0, 140, 0));
                    log.info("Conversion finished successfully.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    showError(msg);
                    progressBar.setValue(0);
                    progressBar.setString("Failed");
                    log.error("Conversion failed: {}", msg);
                } finally {
                    convertButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setForeground(Color.RED);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}