package com.danveil.ui;

import com.danveil.converter.PdfMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MergePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(MergePanel.class);

    private DefaultListModel<String> listModel;
    private JList<String> fileList;
    private JTextField outputField;
    private JButton mergeButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public MergePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        initComponents();
    }

    private void initComponents() {

        // --- Top: file list ---
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setToolTipText("Add PDF files to merge. Use Move Up/Down to reorder.");

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("PDF Files to Merge (in order)"));
        scrollPane.setPreferredSize(new Dimension(0, 180));
        add(scrollPane, BorderLayout.CENTER);

        // --- Right: list control buttons ---
        JPanel listButtons = new JPanel(new GridLayout(4, 1, 0, 6));
        listButtons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton addButton = new JButton("Add PDF");
        JButton removeButton = new JButton("Remove");
        JButton moveUpButton = new JButton("Move Up");
        JButton moveDownButton = new JButton("Move Down");

        addButton.addActionListener(e -> addFiles());
        removeButton.addActionListener(e -> removeSelected());
        moveUpButton.addActionListener(e -> moveUp());
        moveDownButton.addActionListener(e -> moveDown());

        listButtons.add(addButton);
        listButtons.add(removeButton);
        listButtons.add(moveUpButton);
        listButtons.add(moveDownButton);

        add(listButtons, BorderLayout.EAST);

        // --- Bottom: output + merge button + status ---
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Output row
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        bottomPanel.add(new JLabel("Output PDF:"), c);

        outputField = new JTextField(28);
        outputField.setEditable(false);
        c.gridx = 1; c.weightx = 1;
        bottomPanel.add(outputField, c);

        JButton browseOutput = new JButton("Browse");
        browseOutput.addActionListener(e -> browseOutput());
        c.gridx = 2; c.weightx = 0;
        bottomPanel.add(browseOutput, c);

        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setString("Idle");
        progressBar.setStringPainted(true);
        c.gridx = 0; c.gridy = 1;
        c.gridwidth = 3;
        bottomPanel.add(progressBar, c);

        // Merge button
        mergeButton = new JButton("Merge PDFs");
        mergeButton.setEnabled(false);
        mergeButton.addActionListener(e -> startMerge());
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        bottomPanel.add(mergeButton, c);

        // Status label
        statusLabel = new JLabel("Add at least 2 PDF files to begin.", SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        c.gridy = 3;
        bottomPanel.add(statusLabel, c);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select PDF files to merge");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                listModel.addElement(f.getAbsolutePath());
                log.info("Added to merge list: {}", f.getAbsolutePath());
            }
            refreshMergeButton();

            // Auto-fill output path based on first file's folder
            if (outputField.getText().isEmpty() && listModel.size() > 0) {
                File first = new File(listModel.get(0));
                outputField.setText(first.getParent() + File.separator + "merged.pdf");
            }
        }
    }

    private void removeSelected() {
        int index = fileList.getSelectedIndex();
        if (index != -1) {
            listModel.remove(index);
            refreshMergeButton();
        }
    }

    private void moveUp() {
        int index = fileList.getSelectedIndex();
        if (index > 0) {
            String item = listModel.remove(index);
            listModel.add(index - 1, item);
            fileList.setSelectedIndex(index - 1);
        }
    }

    private void moveDown() {
        int index = fileList.getSelectedIndex();
        if (index != -1 && index < listModel.size() - 1) {
            String item = listModel.remove(index);
            listModel.add(index + 1, item);
            fileList.setSelectedIndex(index + 1);
        }
    }

    private void browseOutput() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));
        chooser.setDialogTitle("Save merged PDF as...");

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.toLowerCase().endsWith(".pdf")) {
                path += ".pdf";
            }
            outputField.setText(path);
        }
    }

    private void refreshMergeButton() {
        mergeButton.setEnabled(listModel.size() >= 2 && !outputField.getText().isEmpty());
        if (listModel.size() < 2) {
            statusLabel.setText("Add at least 2 PDF files to begin.");
            statusLabel.setForeground(Color.GRAY);
        } else {
            statusLabel.setText("Ready. " + listModel.size() + " files queued.");
            statusLabel.setForeground(Color.GRAY);
        }
    }

    private void startMerge() {
        String output = outputField.getText().trim();

        if (listModel.size() < 2) {
            showError("Add at least 2 PDF files.");
            return;
        }
        if (output.isEmpty()) {
            showError("Please select an output file.");
            return;
        }

        List<String> paths = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            paths.add(listModel.get(i));
        }

        mergeButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Merging...");
        statusLabel.setText("Merging " + paths.size() + " files...");
        statusLabel.setForeground(Color.BLUE);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                PdfMerger merger = new PdfMerger();
                merger.merge(paths, output);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("Complete!");
                    statusLabel.setText("Done! Saved to: " + output);
                    statusLabel.setForeground(new Color(0, 140, 0));
                    log.info("Merge finished: {}", output);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    progressBar.setIndeterminate(false);
                    progressBar.setString("Failed");
                    showError(msg);
                    log.error("Merge failed: {}", msg);
                } finally {
                    mergeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void showError(String message) {
        statusLabel.setText("Error: " + message);
        statusLabel.setForeground(Color.RED);
        JOptionPane.showMessageDialog(this, message, "Merge Error", JOptionPane.ERROR_MESSAGE);
    }
}