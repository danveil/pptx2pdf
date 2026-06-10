package com.danveil.ui;

import com.danveil.converter.PptxToPdfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainFrame.class);

    private DefaultListModel<String> convertListModel;
    private JList<String> convertFileList;
    private JTextField convertOutputDirField;
    private JButton convertButton;
    private JLabel convertStatusLabel;
    private JProgressBar convertProgressBar;

    public MainFrame() {
        setTitle("PPTX2PDF Converter");
        setSize(640, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("PPTX to PDF", buildConvertPanel());
        tabs.addTab("Merge PDFs", new MergePanel());
        tabs.addTab("All Converters", new ConverterPanel());

        add(tabs);
    }

    private JPanel buildConvertPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        convertListModel = new DefaultListModel<>();
        convertFileList  = new JList<>(convertListModel);
        convertFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(convertFileList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("PPTX Files to Convert"));
        scrollPane.setPreferredSize(new Dimension(0, 160));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel listButtons = new JPanel(new GridLayout(2, 1, 0, 6));
        listButtons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JButton addButton    = new JButton("Add PPTX");
        JButton removeButton = new JButton("Remove");

        addButton.addActionListener(e -> addPptxFiles());
        removeButton.addActionListener(e -> {
            int index = convertFileList.getSelectedIndex();
            if (index != -1) { convertListModel.remove(index); refreshConvertButton(); }
        });

        listButtons.add(addButton);
        listButtons.add(removeButton);
        panel.add(listButtons, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill   = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        bottomPanel.add(new JLabel("Output Folder:"), c);

        convertOutputDirField = new JTextField(28);
        convertOutputDirField.setEditable(false);
        c.gridx = 1; c.weightx = 1;
        bottomPanel.add(convertOutputDirField, c);

        JButton browseOutput = new JButton("Browse");
        browseOutput.addActionListener(e -> browseOutputDir());
        c.gridx = 2; c.weightx = 0;
        bottomPanel.add(browseOutput, c);

        convertProgressBar = new JProgressBar(0, 100);
        convertProgressBar.setStringPainted(true);
        convertProgressBar.setString("Idle");
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3;
        bottomPanel.add(convertProgressBar, c);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        convertButton = new JButton("Convert All to PDF");
        convertButton.setEnabled(false);
        convertButton.addActionListener(e -> startBatchConvert());

        JButton restartButton = new JButton("Restart");
        restartButton.addActionListener(e -> resetConvertTab());

        actionButtons.add(convertButton);
        actionButtons.add(restartButton);

        c.gridy = 2; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.CENTER;
        bottomPanel.add(actionButtons, c);

        convertStatusLabel = new JLabel("Add PPTX files to begin.", SwingConstants.CENTER);
        convertStatusLabel.setForeground(Color.GRAY);
        c.gridy = 3;
        bottomPanel.add(convertStatusLabel, c);

        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void addPptxFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PowerPoint Files (*.pptx)", "pptx"));
        chooser.setMultiSelectionEnabled(true);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) convertListModel.addElement(f.getAbsolutePath());

            if (convertOutputDirField.getText().isEmpty()) {
                convertOutputDirField.setText(new File(convertListModel.get(0)).getParent());
            }
            refreshConvertButton();
        }
    }

    private void browseOutputDir() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            convertOutputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            refreshConvertButton();
        }
    }

    private void refreshConvertButton() {
        boolean ready = convertListModel.size() >= 1 && !convertOutputDirField.getText().isEmpty();
        convertButton.setEnabled(ready);
        convertStatusLabel.setText(convertListModel.size() + " file(s) queued.");
        convertStatusLabel.setForeground(Color.GRAY);
    }

    private void startBatchConvert() {
        java.util.List<String> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < convertListModel.size(); i++) inputs.add(convertListModel.get(i));

        String outputDir = convertOutputDirField.getText().trim();
        int total        = inputs.size();

        convertButton.setEnabled(false);
        convertProgressBar.setValue(0);
        convertProgressBar.setString("Starting...");
        convertStatusLabel.setText("Converting 0 of " + total + "...");
        convertStatusLabel.setForeground(Color.BLUE);

        SwingWorker<Void, int[]> worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() throws Exception {
                PptxToPdfConverter converter = new PptxToPdfConverter();
                for (int i = 0; i < inputs.size(); i++) {
                    String input    = inputs.get(i);
                    String fileName = new File(input).getName().replaceAll("(?i)\\.pptx$", ".pdf");
                    String output   = outputDir + File.separator + fileName;
                    final int fi    = i + 1;

                    converter.convert(input, output, (cur, tot) -> {
                        int pct = (int)(((fi - 1 + cur / (double) tot) / total) * 100);
                        publish(new int[]{fi, total, pct, cur, tot});
                    });
                }
                return null;
            }

            @Override
            protected void process(List<int[]> chunks) {
                int[] l = chunks.get(chunks.size() - 1);
                convertProgressBar.setValue(l[2]);
                convertProgressBar.setString("File " + l[0] + "/" + l[1] + " — Slide " + l[3] + "/" + l[4]);
                convertStatusLabel.setText("Converting file " + l[0] + " of " + l[1] + "...");
            }

            @Override
            protected void done() {
                try {
                    get();
                    convertProgressBar.setValue(100);
                    convertProgressBar.setString("Complete!");
                    convertStatusLabel.setText("Done! " + total + " file(s) saved to: " + outputDir);
                    convertStatusLabel.setForeground(new Color(0, 140, 0));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                    convertProgressBar.setString("Failed");
                    convertStatusLabel.setText("Error: " + msg);
                    convertStatusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(MainFrame.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    convertButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    private void resetConvertTab() {
        convertListModel.clear();
        convertOutputDirField.setText("");
        convertProgressBar.setValue(0);
        convertProgressBar.setString("Idle");
        convertStatusLabel.setText("Add PPTX files to begin.");
        convertStatusLabel.setForeground(Color.GRAY);
        convertButton.setEnabled(false);
    }
}