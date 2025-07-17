/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
// File: SchedulerGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;



public class SchedulerGUI extends JFrame {
    private JTextArea outputArea;
    private JComboBox<String> algoComboBox;
    private JTextField quantumField;
    private JTable processTable;
    private DefaultListModel<Process> processList;

    public SchedulerGUI() {
        setTitle("CPU Scheduling Visualizer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(255, 253, 208)); // Cream background
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBackground(new Color(255, 253, 208));

        algoComboBox = new JComboBox<>(new String[]{"FIFO", "SJF", "SRTF", "RR", "MLFQ"});
        quantumField = new JTextField(5);
        JButton runBtn = new JButton("Run");
        JButton exportBtn = new JButton("Export");

        topPanel.add(new JLabel("Algorithm:"));
        topPanel.add(algoComboBox);
        topPanel.add(new JLabel("Quantum (if RR/MLFQ):"));
        topPanel.add(quantumField);
        topPanel.add(runBtn);
        topPanel.add(exportBtn);

        // Center Panel
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Dummy input process table (optional input UI)
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(255, 253, 208));
        inputPanel.add(new JLabel("Output:"), BorderLayout.NORTH);
        inputPanel.add(scrollPane, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.CENTER);

        runBtn.addActionListener((ActionEvent e) -> {
            runScheduler();
        });

        exportBtn.addActionListener((ActionEvent e) -> {
            Scheduler.exportToFile(outputArea.getText());
        });
    }

    private void runScheduler() {
        String selected = (String) algoComboBox.getSelectedItem();
        String quantumText = quantumField.getText();
        int quantum = 1;
        try {
            if (!quantumText.isEmpty()) {
                quantum = Integer.parseInt(quantumText);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantum value.");
            return;
        }

        List<Process> processes = Scheduler.defaultProcesses();

        String result = "";
        if (null != selected) switch (selected) {
            case "FIFO" -> result = Scheduler.fifo(processes);
            case "SJF" -> result = Scheduler.sjf(processes);
            case "SRTF" -> result = Scheduler.srtf(processes);
            case "RR" -> result = Scheduler.roundRobin(processes, quantum);
            case "MLFQ" -> result = Scheduler.mlfq(processes, quantum);
            default -> {
            }
        }

        outputArea.setText(result);
    }
}
