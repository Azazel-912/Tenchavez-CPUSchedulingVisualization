/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SchedulerGUI extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private final JComboBox<String> algoBox;
    private final JTextField quantumField;
    private final JTextField[] mlfqQuantums;
    private final JTextField[] mlfqAllotments;
    private JTextArea outputArea;

    public SchedulerGUI() {
        setTitle("CPU Scheduling Visualization");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Color cream = new Color(255, 253, 208);

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setBackground(cream);
        algoBox = new JComboBox(new String[] {"FIFO", "SJF", "SRTF", "Round Robin", "MLFQ"});
        quantumField = new JTextField("2", 3);

        topPanel.add(new JLabel("Algorithm:"));
        topPanel.add(algoBox);
        topPanel.add(new JLabel("Quantum:"));
        topPanel.add(quantumField);

        // MLFQ Inputs
        mlfqQuantums = new JTextField[4];
        mlfqAllotments = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            mlfqQuantums[i] = new JTextField("2", 2);
            mlfqAllotments[i] = new JTextField("4", 2);
            topPanel.add(new JLabel("Q" + i + " Q:"));
            topPanel.add(mlfqQuantums[i]);
            topPanel.add(new JLabel("A:"));
            topPanel.add(mlfqAllotments[i]);
        }

        JButton runBtn = new JButton("Run");
        JButton exportBtn = new JButton("Export");
        topPanel.add(runBtn);
        topPanel.add(exportBtn);

        // Middle Panel (Table)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(cream);
        model = new DefaultTableModel(new String[]{"PID", "Arrival Time", "Burst Time"}, 0);
        table = new JTable(model);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Process");
        JButton delBtn = new JButton("Delete Selected");
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(cream);
        btnPanel.add(addBtn);
        btnPanel.add(delBtn);
        centerPanel.add(btnPanel, BorderLayout.SOUTH);

        // Bottom Panel (Output)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(cream);
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        bottomPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // Layout Panels
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Actions
        addBtn.addActionListener((ActionEvent e) -> {
            model.addRow(new Object[]{"P" + (model.getRowCount() + 1), "0", "1"});
        });

        delBtn.addActionListener((ActionEvent e) -> {
            int row = table.getSelectedRow();
            if (row != -1) model.removeRow(row);
        });

        runBtn.addActionListener(this::runScheduler);

        exportBtn.addActionListener((ActionEvent e) -> {
            Scheduler.exportToFile(outputArea.getText());
        });

        setVisible(true);
    }

    private void runScheduler(ActionEvent e) {
        outputArea.setText("");
        List<Process> processes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            try {
                String pid = model.getValueAt(i, 0).toString().trim();
                int at = Integer.parseInt(model.getValueAt(i, 1).toString().trim());
                int bt = Integer.parseInt(model.getValueAt(i, 2).toString().trim());
                processes.add(new Process(pid, at, bt));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input on row " + (i + 1));
                return;
            }
        }

        StringBuilder out = new StringBuilder();
        String algo = algoBox.getSelectedItem().toString();

        try {
            if (null != algo) switch (algo) {
                case "FIFO" -> Scheduler.fifo(processes, out);
                case "SJF" -> Scheduler.sjf(processes, out);
                case "SRTF" -> Scheduler.srtf(processes, out);
                case "Round Robin" -> {
                    int q = Integer.parseInt(quantumField.getText().trim());
                    Scheduler.rr(processes, q, out);
                }
                case "MLFQ" -> {
                    int[] tq = new int[4];
                    int[] at = new int[4];
                    for (int i = 0; i < 4; i++) {
                        tq[i] = Integer.parseInt(mlfqQuantums[i].getText().trim());
                        at[i] = Integer.parseInt(mlfqAllotments[i].getText().trim());
                    }   Scheduler.mlfq(processes, tq, at, out);
                }
                default -> {
                }
            }
        } catch (NumberFormatException ex) {
            out.append("\n\nERROR: ").append(ex.getMessage());
        }

        outputArea.setText(out.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        });
    }
}
