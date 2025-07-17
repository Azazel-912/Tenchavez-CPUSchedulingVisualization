/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class SchedulerGUI extends JFrame {
    private final JTable processTable;
    private DefaultTableModel tableModel;
    private JTextArea outputArea;
    private JPanel ganttPanel;

    public SchedulerGUI() {
        setTitle("CPU Scheduling Visualizer");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Cream-colored theme
        Color cream = new Color(255, 253, 208);
        getContentPane().setBackground(cream);

        // Top Panel - Buttons
        JPanel topPanel = new JPanel();
        topPanel.setBackground(cream);
        JButton addBtn = new JButton("Add Process");
        JButton generateBtn = new JButton("Generate Sample");
        JButton runBtn = new JButton("Run Scheduler");
        topPanel.add(addBtn);
        topPanel.add(generateBtn);
        topPanel.add(runBtn);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel - Table + Output
        tableModel = new DefaultTableModel(new Object[]{"PID", "Arrival", "Burst"}, 0);
        processTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(processTable);

        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputArea);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.add(tableScroll);
        centerPanel.add(outputScroll);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel - Gantt
        ganttPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGanttChart(g);
            }
        };
        ganttPanel.setPreferredSize(new Dimension(800, 80));
        ganttPanel.setBackground(Color.WHITE);
        add(ganttPanel, BorderLayout.SOUTH);

        // Add Process
        addBtn.addActionListener(e -> {
            tableModel.addRow(new Object[]{"P" + (tableModel.getRowCount() + 1), 0, 1});
        });

        // Generate Sample
        generateBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"P1", 0, 5});
            tableModel.addRow(new Object[]{"P2", 1, 3});
            tableModel.addRow(new Object[]{"P3", 2, 8});
            tableModel.addRow(new Object[]{"P4", 3, 6});
        });

        // Run Scheduler
        runBtn.addActionListener(e -> {
            List<Process> processes = new ArrayList<>();  // Correct instantiation
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String pid = tableModel.getValueAt(i, 0).toString();
                int arrival = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                int burst = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                processes.add(new Process(pid, arrival, burst));
            }
            StringBuilder output = new StringBuilder();
            output.append("=== FIFO ===\n").append(Scheduler.fifoOutput(processes)).append("\n\n");
            output.append("=== SJF ===\n").append(Scheduler.sjfOutput(processes)).append("\n\n");
            output.append("=== SRTF ===\n").append(Scheduler.srtfOutput(processes)).append("\n\n");
            output.append("=== RR ===\n").append(Scheduler.rrOutput(processes, 4)).append("\n\n");
            output.append("=== MLFQ ===\n").append(Scheduler.mlfqOutput(processes)).append("\n\n");

            outputArea.setText(output.toString());
            ganttPanel.repaint();
        });
    }

    private void drawGanttChart(Graphics g) {
        // Simple placeholder chart - expand if needed
        g.setColor(Color.BLUE);
        g.fillRect(10, 20, 100, 30);
        g.setColor(Color.BLACK);
        g.drawString("Sample", 15, 40);
    }

}