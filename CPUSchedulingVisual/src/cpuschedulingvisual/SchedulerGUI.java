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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter; // For file chooser

public class SchedulerGUI extends JFrame {

    // --- GUI Components ---
    // Left Panels - Process Definition and Input
    private JTextField pidField, arrivalTimeField, execTimeField, priorityField, extensionField;
    private JComboBox<String> processNameDropdown;
    private JTextField randomLengthField;
    private JButton enqueueButton, dequeueButton, updateButton, generateRandomBtn;

    // Left Panels - Process Definition Table
    private JTable processDefinitionTable;
    private DefaultTableModel processDefinitionModel;

    // Left Panels - Algorithm Selection & Action Message
    private JComboBox<String> algorithmComboBox;
    private JTextField timeQuantumField; // For Round Robin
    private JLabel actionMessageLabel;

    // MLFQ Configuration Fields
    private JTextField q0QuantumField, q1QuantumField, q2QuantumField, q3QuantumField;
    private JTextField q0AllotmentField, q1AllotmentField, q2AllotmentField, q3AllotmentField;

    // Left Panels - Gantt Chart
    private JPanel ganttChartPanel;

    // Right Panels - Live Status Table
    private JTable liveStatusTable;
    private DefaultTableModel liveStatusModel;

    // Right Panels - Metrics Display
    private JLabel avgWaitingTimeLabel, avgExecutionTimeLabel, avgTurnaroundTimeLabel, avgResponseTimeLabel;
    private JLabel totalExecutionTimeLabel;
    private JLabel currentCPULabel, nextQueueLabel;
    private JProgressBar overallProgressBar;

    // Right Panels - Simulation Controls
    private JSlider simulationSpeedSlider;
    private JButton simulateButton, resetButton, exportResultsBtn;

    // --- Simulation Data ---
    private List<Process> definedProcesses; // Stores processes defined by the user
    private List<Process> currentSimulationProcesses; // Processes for the current simulation run
    private int currentTime = 0; // Current simulation time
    private Timer simulationTimer; // Timer for animation
    private int simulationSpeed = 200; // Milliseconds per tick (e.g., 200ms = 5 ticks/sec)
    private String currentRunningProcessPID = "None"; // For CPU display
    private String nextInQueuePID = "None"; // For Next Queue display

    // Gantt Chart specific
    private List<String> ganttChartSequence = new ArrayList<>(); // Stores PIDs for Gantt chart blocks
    private Map<String, Color> processColors = new HashMap<>(); // To keep consistent colors for processes

    // --- Gantt Chart specific constants ---
    private static final int GANTT_BLOCK_WIDTH = 30; // Width of each time unit block
    private static final int GANTT_ROW_HEIGHT = 20; // Height for the drawing area of each process
    private static final int GANTT_TIME_AXIS_HEIGHT = 20; // Height for the time axis
    private static final int GANTT_PANEL_FIXED_HEIGHT = GANTT_TIME_AXIS_HEIGHT + GANTT_ROW_HEIGHT + 20; // Total fixed height for the Gantt Chart Panel (e.g., 1 row for blocks + axis + padding)

    // --- Colors ---
    private final Color CREAM = new Color(255, 253, 208);
    private final Color DARK_BLUE_BACKGROUND = new Color(30, 30, 60); // For action message and possibly other dark elements
    private final Color LIGHT_GRAY_BORDER = new Color(200, 200, 200);

    // Algorithm-specific data structures
    private Queue<Process> Queue;
    private Process crentCPUProcess = null;
    // For Round Robin
    private int currentProcessQuantumRemaining; 
    private List<Queue<Process>> mlfqQueues; // For MLFQ
    private int[] mlfqQuantums = new int[4]; // Configurable quantums for Q0, Q1, Q2, Q3
    private int[] mlfqAllotments = new int[4]; // Configurable allotment times for Q0, Q1, Q2, Q3

    public SchedulerGUI() {
        this.currentProcessQuantumRemaining = 0;
        setTitle("CPU Scheduling Visualization - DEVELOPED BY : TENCHAVEZ RIEZNICK MCCAIN G.");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Add some gap between main panels
        getContentPane().setBackground(CREAM); // Overall background

        definedProcesses = new ArrayList<>();
        initializeComponents();
        setupLayout();
        addListeners();
        updateProcessDefinitionDropdown(); // Initialize dropdown with any initial processes if applicable
        updateMetricsDisplay(); // Initialize metrics to 0 or default
        actionMessageLabel.setText("Welcome! Define processes and click Simulate.");

        // Set frame to maximized state
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1000, 700)); // Minimum size if user somehow un-maximizes

        setLocationRelativeTo(null); // Center the frame on the screen (effective if not maximized)
        setVisible(true);
    }

    private void initializeComponents() {
        // --- Left Panel Components (Process Definition, Algorithm, Gantt) ---
        // Process Input Fields
        processNameDropdown = new JComboBox<>();
        pidField = new JTextField(5);
        extensionField = new JTextField(".script", 4); // Default extension
        arrivalTimeField = new JTextField("0", 3);
        execTimeField = new JTextField("10", 3);
        priorityField = new JTextField("5", 3);
        randomLengthField = new JTextField("5", 3); // Field for generating random processes

        // Process Action Buttons
        generateRandomBtn = new JButton("Generate Random");
        enqueueButton = new JButton("Enqueue");
        dequeueButton = new JButton("Dequeue");
        updateButton = new JButton("Update");

        // Process Definition Table
        processDefinitionModel = new DefaultTableModel(
            new Object[]{"#", "Process", "Extension", "Arrival Time", "Exec. Time", "Priority"}, 0 // Added Extension column
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; // Allow editing all columns except '#'
            }
        };
        processDefinitionTable = new JTable(processDefinitionModel);
        processDefinitionTable.setFillsViewportHeight(true);
        processDefinitionTable.getColumnModel().getColumn(0).setMaxWidth(30); // Small width for #
        processDefinitionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Only one row selectable

        // Algorithm Selection
        algorithmComboBox = new JComboBox<>(new String[]{"First Come First Serve", "SJF", "SRTF", "Round Robin", "MLFQ"});
        timeQuantumField = new JTextField("2", 3); // Default quantum for RR

        // MLFQ Configuration Fields
        q0QuantumField = new JTextField("2", 3);
        q1QuantumField = new JTextField("4", 3);
        q2QuantumField = new JTextField("8", 3);
        q3QuantumField = new JTextField("16", 3); // Default quantums

        q0AllotmentField = new JTextField("4", 3);
        q1AllotmentField = new JTextField("8", 3);
        q2AllotmentField = new JTextField("12", 3);
        q3AllotmentField = new JTextField("16", 3); // Default allotments

        // Action Message
        actionMessageLabel = new JLabel("Status Message Here", SwingConstants.CENTER);
        actionMessageLabel.setOpaque(true);
        actionMessageLabel.setBackground(DARK_BLUE_BACKGROUND);
        actionMessageLabel.setForeground(Color.WHITE);
        actionMessageLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        actionMessageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Gantt Chart Panel
        ganttChartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE); // Background of Gantt chart area
                g.fillRect(0, 0, getWidth(), getHeight()); // Fill the entire visible area
                drawGanttChart(g);
            }

            @Override
            public Dimension getPreferredSize() {
                // Calculate the preferred width based on the total simulation time.
                // Each time unit needs GANTT_BLOCK_WIDTH. Add some padding for axis labels.
                // Ensure a minimum width (e.g., 800) if no simulation has run yet.
                int calculatedWidth = Math.max(800, (currentTime + 2) * GANTT_BLOCK_WIDTH); // +2 to give space for last label
                return new Dimension(calculatedWidth, GANTT_PANEL_FIXED_HEIGHT);
            }
        };
        ganttChartPanel.setBackground(Color.WHITE);
        ganttChartPanel.setBorder(BorderFactory.createLineBorder(LIGHT_GRAY_BORDER));

        // --- Right Panel Components (Live Status, Metrics, Controls) ---
        // Live Status Table
        liveStatusModel = new DefaultTableModel(
            new Object[]{"Process", "Status", "Completion %", "Rem. Time", "Wait Time", "Comp. Time", "TAT", "Resp. Time"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Live status table is not editable
            }
        };
        liveStatusTable = new JTable(liveStatusModel);
        liveStatusTable.setFillsViewportHeight(true);
        // Set custom renderers for progress bars
        liveStatusTable.getColumnModel().getColumn(2).setCellRenderer(new ProgressBarRenderer()); // "Completion %"
        // Set renderer for "Status" column
        liveStatusTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("Completed".equals(value)) {
                    c.setForeground(new Color(0, 100, 0)); // Dark green for completed
                } else if ("Running".equals(value)) {
                    c.setForeground(new Color(0, 0, 150)); // Dark blue for running
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });


        // Metrics Display
        avgWaitingTimeLabel = new JLabel("Average Waiting Time : 0.00");
        avgExecutionTimeLabel = new JLabel("Average Burst Time : 0.00"); // Renamed for clarity, often "Average Execution Time" refers to burst time
        avgTurnaroundTimeLabel = new JLabel("Average Turnaround Time : 0.00"); // New
        avgResponseTimeLabel = new JLabel("Average Response Time : 0.00"); // New
        totalExecutionTimeLabel = new JLabel("Total Simulation Time : 0");

        currentCPULabel = new JLabel("CPU : None");
        nextQueueLabel = new JLabel("Next Queue : None");

        overallProgressBar = new JProgressBar(0, 100);
        overallProgressBar.setStringPainted(true);
        overallProgressBar.setString("0.00%");

        // Simulation Controls
        simulationSpeedSlider = new JSlider(JSlider.HORIZONTAL, 10, 1000, 200); // Min: fast (10ms/tick), Max: slow (1000ms/tick)
        simulationSpeedSlider.setMajorTickSpacing(990); // Spacing between 10 and 1000 for proper label rendering
        simulationSpeedSlider.setMinorTickSpacing(25);
        simulationSpeedSlider.setPaintTicks(true);
        simulationSpeedSlider.setPaintLabels(true);
        // Custom labels for speed (inverted: lower ms = faster)
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(10, new JLabel("x100 (Fast)"));
        labelTable.put(100, new JLabel("x10"));
        labelTable.put(200, new JLabel("x5"));
        labelTable.put(500, new JLabel("x2"));
        labelTable.put(1000, new JLabel("x1 (Slow)"));
        simulationSpeedSlider.setLabelTable(labelTable);


        simulateButton = new JButton("Simulate");
        resetButton = new JButton("Reset All");
        exportResultsBtn = new JButton("Export Results"); // New button

        // Initialize simulation timer (not started yet)
        simulationTimer = new Timer(simulationSpeed, this::simulationTick);
    }

    private void setupLayout() {
        // --- Left Panel (Process Definition, Algorithm, Gantt) ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(CREAM);

        // Fixed width for the left panel to control overall layout
        int fixedLeftPanelWidth = 550; // Adjust this value as needed based on your screen/preference
        leftPanel.setPreferredSize(new Dimension(fixedLeftPanelWidth, Integer.MAX_VALUE));
        leftPanel.setMinimumSize(new Dimension(fixedLeftPanelWidth, 500)); // Min height

        // Top-Left: Process Input & Controls
        JPanel processInputSection = new JPanel(new BorderLayout()); // Use BorderLayout for sub-sections
        processInputSection.setBackground(CREAM);
        processInputSection.setBorder(BorderFactory.createTitledBorder("Process Definition"));

        // Row 1: Generate Random
        JPanel generateRandomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        generateRandomPanel.setBackground(CREAM);
        generateRandomPanel.add(new JLabel("Length:"));
        generateRandomPanel.add(randomLengthField);
        generateRandomPanel.add(generateRandomBtn);
        processInputSection.add(generateRandomPanel, BorderLayout.NORTH);

        // Row 2: Individual Process Input Fields
        JPanel individualProcessInputPanel = new JPanel(new GridLayout(2, 5, 5, 5)); // 2 rows, 5 columns (labels + fields)
        individualProcessInputPanel.setBackground(CREAM);
        individualProcessInputPanel.add(new JLabel("Process:"));
        individualProcessInputPanel.add(new JLabel("Extension:"));
        individualProcessInputPanel.add(new JLabel("Arrival Time:"));
        individualProcessInputPanel.add(new JLabel("Exec. Time:"));
        individualProcessInputPanel.add(new JLabel("Priority:"));

        individualProcessInputPanel.add(pidField);
        individualProcessInputPanel.add(extensionField);
        individualProcessInputPanel.add(arrivalTimeField);
        individualProcessInputPanel.add(execTimeField);
        individualProcessInputPanel.add(priorityField);
        processInputSection.add(individualProcessInputPanel, BorderLayout.CENTER);

        // Row 3: Action Buttons (Enqueue, Dequeue, Update)
        JPanel processActionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        processActionButtonsPanel.setBackground(CREAM);
        processActionButtonsPanel.add(enqueueButton);
        processActionButtonsPanel.add(dequeueButton);
        processActionButtonsPanel.add(updateButton);
        processInputSection.add(processActionButtonsPanel, BorderLayout.SOUTH);

        leftPanel.add(processInputSection, BorderLayout.NORTH);


        // Middle-Left: Process Definition Table
        JPanel processTablePanel = new JPanel(new BorderLayout());
        processTablePanel.setBackground(CREAM);
        processTablePanel.setBorder(BorderFactory.createTitledBorder("Processes Defined"));
        processTablePanel.add(new JScrollPane(processDefinitionTable), BorderLayout.CENTER);
        leftPanel.add(processTablePanel, BorderLayout.CENTER);

        // Bottom-Left: Algorithm, Action Message, MLFQ Config, Gantt Chart
        JPanel bottomSectionLeft = new JPanel(new BorderLayout(5, 5));
        bottomSectionLeft.setBackground(CREAM);

        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        algorithmPanel.setBackground(CREAM);
        algorithmPanel.add(new JLabel("Algorithm:"));
        algorithmPanel.add(algorithmComboBox);
        algorithmPanel.add(new JLabel("Time Quantum for RR:"));
        algorithmPanel.add(timeQuantumField);
        bottomSectionLeft.add(algorithmPanel, BorderLayout.NORTH);

        // MLFQ Configuration Panel (New)
        JPanel mlfqConfigPanel = new JPanel(new GridBagLayout());
        mlfqConfigPanel.setBackground(CREAM);
        mlfqConfigPanel.setBorder(BorderFactory.createTitledBorder("MLFQ Quantums & Allotments"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5); // Padding
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Labels
        gbc.gridx = 1; gbc.gridy = 0; mlfqConfigPanel.add(new JLabel("Quantum"), gbc);
        gbc.gridx = 2; gbc.gridy = 0; mlfqConfigPanel.add(new JLabel("Allotment"), gbc);

        // Q0
        gbc.gridx = 0; gbc.gridy = 1; mlfqConfigPanel.add(new JLabel("Q0:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; mlfqConfigPanel.add(q0QuantumField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; mlfqConfigPanel.add(q0AllotmentField, gbc);

        // Q1
        gbc.gridx = 0; gbc.gridy = 2; mlfqConfigPanel.add(new JLabel("Q1:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; mlfqConfigPanel.add(q1QuantumField, gbc);
        gbc.gridx = 2; gbc.gridy = 2; mlfqConfigPanel.add(q1AllotmentField, gbc);

        // Q2
        gbc.gridx = 0; gbc.gridy = 3; mlfqConfigPanel.add(new JLabel("Q2:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; mlfqConfigPanel.add(q2QuantumField, gbc);
        gbc.gridx = 2; gbc.gridy = 3; mlfqConfigPanel.add(q2AllotmentField, gbc);

        // Q3
        gbc.gridx = 0; gbc.gridy = 4; mlfqConfigPanel.add(new JLabel("Q3:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; mlfqConfigPanel.add(q3QuantumField, gbc);
        gbc.gridx = 2; gbc.gridy = 4; mlfqConfigPanel.add(q3AllotmentField, gbc);

        bottomSectionLeft.add(mlfqConfigPanel, BorderLayout.CENTER); // MLFQ Config

        JPanel actionAndGanttPanel = new JPanel(new BorderLayout(5,5));
        actionAndGanttPanel.setBackground(CREAM);
        actionAndGanttPanel.add(actionMessageLabel, BorderLayout.NORTH); // Action Message

        JPanel ganttChartWrapper = new JPanel(new BorderLayout());
        ganttChartWrapper.setBackground(CREAM);
        ganttChartWrapper.setBorder(BorderFactory.createTitledBorder("Gantt Chart (Each box represents a second)"));

        JScrollPane ganttScrollPane = new JScrollPane(ganttChartPanel);
        ganttScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        // Explicitly set preferred viewport size for the JScrollPane
        ganttScrollPane.setPreferredSize(new Dimension(fixedLeftPanelWidth - 20, GANTT_PANEL_FIXED_HEIGHT + 5));
        ganttScrollPane.setMinimumSize(new Dimension(fixedLeftPanelWidth - 20, GANTT_PANEL_FIXED_HEIGHT + 5));

        ganttChartWrapper.add(ganttScrollPane, BorderLayout.CENTER);

        actionAndGanttPanel.add(ganttChartWrapper, BorderLayout.CENTER);

        bottomSectionLeft.add(actionAndGanttPanel, BorderLayout.SOUTH);

        leftPanel.add(bottomSectionLeft, BorderLayout.SOUTH);
        add(leftPanel, BorderLayout.WEST); // Add the whole left panel to the main frame


        // --- Right Panel (Live Status, Metrics, Controls) ---
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(CREAM);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding

        // Top-Right: Live Status Table
        JPanel liveStatusPanel = new JPanel(new BorderLayout());
        liveStatusPanel.setBackground(CREAM);
        liveStatusPanel.setBorder(BorderFactory.createTitledBorder("Process Status"));
        liveStatusPanel.add(new JScrollPane(liveStatusTable), BorderLayout.CENTER);

        JLabel sortedLabel = new JLabel("Process is sorted according to Arrival Time.", SwingConstants.CENTER);
        sortedLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        liveStatusPanel.add(sortedLabel, BorderLayout.SOUTH); // Add the sorting label
        rightPanel.add(liveStatusPanel, BorderLayout.NORTH);


        // Middle-Right: Metrics and CPU/Next Queue
        JPanel metricsPanel = new JPanel();
        metricsPanel.setBackground(CREAM);
        metricsPanel.setLayout(new GridLayout(8, 1, 5, 2)); // 8 rows now for new metrics
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Simulation Metrics"));
        metricsPanel.add(avgWaitingTimeLabel);
        metricsPanel.add(avgExecutionTimeLabel);
        metricsPanel.add(avgTurnaroundTimeLabel); // New
        metricsPanel.add(avgResponseTimeLabel);   // New
        metricsPanel.add(totalExecutionTimeLabel);
        metricsPanel.add(currentCPULabel);
        metricsPanel.add(nextQueueLabel);
        metricsPanel.add(overallProgressBar);
        rightPanel.add(metricsPanel, BorderLayout.CENTER);

        // Bottom-Right: Simulation Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(CREAM);
        controlPanel.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));
        controlPanel.add(new JLabel("Simulation Speed:"));
        controlPanel.add(simulationSpeedSlider);
        controlPanel.add(simulateButton);
        controlPanel.add(resetButton);
        controlPanel.add(exportResultsBtn); // New Export button
        rightPanel.add(controlPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER); // Add the whole right panel to the main frame

        // Initialize MLFQ fields visibility
        toggleMLFQConfigFields(false); // Hide by default
    }

    private void addListeners() {
        // Process Definition Buttons
        enqueueButton.addActionListener(e -> addProcessFromInput());
        dequeueButton.addActionListener(e -> deleteSelectedProcess());
        updateButton.addActionListener(e -> updateSelectedProcess());
        generateRandomBtn.addActionListener(e -> generateRandomProcesses());

        // Simulation Control Buttons
        simulateButton.addActionListener(e -> toggleSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        exportResultsBtn.addActionListener(e -> exportResultsToFile()); // Listener for new button

        // Simulation Speed Slider Listener
        simulationSpeedSlider.addChangeListener(e -> {
            simulationSpeed = simulationSpeedSlider.getValue();
            if (simulationTimer.isRunning()) {
                simulationTimer.setDelay(simulationSpeed);
            }
        });

        // Algorithm ComboBox Listener (for enabling/disabling quantum/MLFQ fields)
        algorithmComboBox.addActionListener(e -> {
            String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
            boolean isRR = "Round Robin".equals(selectedAlgo);
            boolean isMLFQ = "MLFQ".equals(selectedAlgo);

            timeQuantumField.setEnabled(isRR);
            toggleMLFQConfigFields(isMLFQ);
        });
        // Initial state of quantum/MLFQ fields
        String initialAlgo = (String) algorithmComboBox.getSelectedItem();
        timeQuantumField.setEnabled("Round Robin".equals(initialAlgo));
        toggleMLFQConfigFields("MLFQ".equals(initialAlgo));
    }

    // Helper method to toggle MLFQ config fields visibility
    private void toggleMLFQConfigFields(boolean enable) {
        q0QuantumField.setEnabled(enable);
        q1QuantumField.setEnabled(enable);
        q2QuantumField.setEnabled(enable);
        q3QuantumField.setEnabled(enable);
        q0AllotmentField.setEnabled(enable);
        q1AllotmentField.setEnabled(enable);
        q2AllotmentField.setEnabled(enable);
        q3AllotmentField.setEnabled(enable);
    }

    private void addProcessFromInput() {
        try {
            String pid = pidField.getText().trim();
            String ext = extensionField.getText().trim();
            int at = Integer.parseInt(arrivalTimeField.getText().trim());
            int bt = Integer.parseInt(execTimeField.getText().trim());
            int prio = Integer.parseInt(priorityField.getText().trim());

            if (pid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Process ID cannot be empty.");
                return;
            }
            if (bt <= 0) {
                 JOptionPane.showMessageDialog(this, "Burst Time must be greater than 0.");
                 return;
            }

            // Check for duplicate PID (regardless of extension for simplicity, or modify logic)
            if (definedProcesses.stream().anyMatch(p -> p.pid.equals(pid) && p.extension.equals(ext))) {
                JOptionPane.showMessageDialog(this, "Process with PID '" + pid + ext + "' already exists. Use Update to modify.");
                return;
            }

            Process newProcess = new Process(pid, ext, at, bt, prio);
            definedProcesses.add(newProcess);
            updateProcessDefinitionTable();
            updateProcessDefinitionDropdown();
            actionMessageLabel.setText("Process " + newProcess.getFullPid() + " enqueued.");

            // Clear input fields after adding
            pidField.setText("");
            extensionField.setText(".script");
            arrivalTimeField.setText("0");
            execTimeField.setText("10");
            priorityField.setText("5");

            // Assign a color for the new process for Gantt chart
            getColorForProcess(newProcess.getFullPid());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format for Arrival Time, Exec. Time, or Priority.");
        }
    }

    private void deleteSelectedProcess() {
        int selectedRow = processDefinitionTable.getSelectedRow();
        if (selectedRow != -1) {
            String fullPidToDelete = (String) processDefinitionModel.getValueAt(selectedRow, 1) + (String) processDefinitionModel.getValueAt(selectedRow, 2);
            definedProcesses.removeIf(p -> p.getFullPid().equals(fullPidToDelete));
            updateProcessDefinitionTable();
            updateProcessDefinitionDropdown();
            actionMessageLabel.setText("Process " + fullPidToDelete + " dequeued.");
        } else {
            JOptionPane.showMessageDialog(this, "Please select a process to delete.");
        }
    }

    private void updateSelectedProcess() {
        int selectedRow = processDefinitionTable.getSelectedRow();
        if (selectedRow != -1) {
            try {
                String oldPidName = (String) processDefinitionModel.getValueAt(selectedRow, 1);
                String oldExt = (String) processDefinitionModel.getValueAt(selectedRow, 2);
                String oldFullPid = oldPidName + oldExt;

                String newPidName = (String) processDefinitionModel.getValueAt(selectedRow, 1);
                String newExt = (String) processDefinitionModel.getValueAt(selectedRow, 2);
                int newAt = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 3).toString());
                int newBt = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 4).toString());
                int newPrio = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 5).toString());

                if (newBt <= 0) {
                    JOptionPane.showMessageDialog(this, "Burst Time must be greater than 0.");
                    updateProcessDefinitionTable(); // Revert display to old values
                    return;
                }

                String newFullPid = newPidName + newExt;
                // Check for duplicate PID if PID/Ext changed to an existing one
                if (!oldFullPid.equals(newFullPid) && definedProcesses.stream().anyMatch(p -> p.getFullPid().equals(newFullPid))) {
                    JOptionPane.showMessageDialog(this, "A process with PID '" + newFullPid + "' already exists. Cannot update to a duplicate PID.");
                    updateProcessDefinitionTable();
                    return;
                }

                for (Process p : definedProcesses) {
                    if (p.getFullPid().equals(oldFullPid)) {
                        p.pid = newPidName;
                        p.extension = newExt;
                        p.arrivalTime = newAt;
                        p.burstTime = newBt;
                        p.priority = newPrio;
                        p.reset(); // Reset simulation-related times on update
                        actionMessageLabel.setText("Process " + p.getFullPid() + " updated.");
                        updateProcessDefinitionTable();
                        updateProcessDefinitionDropdown();
                        return;
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input in table. Please check numerical values.");
                updateProcessDefinitionTable(); // Revert display to old values
            } catch (HeadlessException ex) {
                JOptionPane.showMessageDialog(this, "Error updating process: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a process to update.");
        }
    }


    private void generateRandomProcesses() {
        try {
            int numProcesses = Integer.parseInt(randomLengthField.getText().trim());
            Random rand = new Random();
            for (int i = 0; i < numProcesses; i++) {
                String generatedPid;
                String ext = ".script";
                int counter = definedProcesses.size() + 1;

                do {
                    generatedPid = "P" + counter;
                    final String currentPidForLambda = generatedPid;
                    counter++;
                    if (definedProcesses.stream().anyMatch(p -> p.pid.equals(currentPidForLambda) && p.extension.equals(ext))) {
                    } else {
                        break;
                    }
                } while (true);

                int at = rand.nextInt(20); // Arrival time 0-19
                int bt = rand.nextInt(20) + 1; // Burst time 1-20 (must be > 0)
                int prio = rand.nextInt(10) + 1; // Priority 1-10

                definedProcesses.add(new Process(generatedPid, ext, at, bt, prio));
                getColorForProcess(generatedPid + ext);
            }
            updateProcessDefinitionTable();
            updateProcessDefinitionDropdown();
            actionMessageLabel.setText(numProcesses + " random processes generated.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number for 'Length' to generate random processes.");
        }
    }

    private void updateProcessDefinitionTable() {
        processDefinitionModel.setRowCount(0); // Clear existing rows
        definedProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int rowNum = 1;
        for (Process p : definedProcesses) {
            processDefinitionModel.addRow(new Object[]{
                rowNum++,
                p.pid,
                p.extension,
                p.arrivalTime,
                p.burstTime,
                p.priority
            });
        }
    }

    private void updateProcessDefinitionDropdown() {
        processNameDropdown.removeAllItems();
        for (Process p : definedProcesses) {
            processNameDropdown.addItem(p.getFullPid());
        }
    }

    private void updateLiveStatusTable() {
        liveStatusModel.setRowCount(0); // Clear existing rows
        if (currentSimulationProcesses == null) return;

        currentSimulationProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));

        for (Process p : currentSimulationProcesses) {
            double completionPercent = (p.burstTime > 0) ? (double)(p.burstTime - p.remainingTime) / p.burstTime * 100 : 0.0;
            if (p.remainingTime == 0 && p.completionTime != -1) completionPercent = 100.0;

            String statusText;
            if (p.isCompleted()) {
                statusText = "Completed";
            } else if (p.startTime != -1 && p.remainingTime > 0 && p.pid.equals(currentRunningProcessPID)) {
                statusText = "Running";
            } else if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                statusText = "Ready";
            } else {
                statusText = "New";
            }

            // Display "N/A" or "-" for completed metrics if not yet completed
            String completionTime = p.isCompleted() ? String.valueOf(p.completionTime) : "-";
            String turnaroundTime = p.isCompleted() ? String.valueOf(p.turnaroundTime) : "-";
            String responseTime = p.startTime != -1 ? String.valueOf(p.responseTime) : "-"; // Response time is known once started

            liveStatusModel.addRow(new Object[]{
                p.getFullPid(),
                statusText,
                completionPercent,
                p.remainingTime,
                p.waitingTime,
                completionTime,
                turnaroundTime,
                responseTime
            });
        }
    }

    // Custom Cell Renderer for JProgressBar in JTable
    class ProgressBarRenderer extends DefaultTableCellRenderer {
        private final JProgressBar progressBar = new JProgressBar(0, 100);

        public ProgressBarRenderer() {
            setOpaque(true);
            progressBar.setStringPainted(true);
            progressBar.setForeground(new Color(50, 205, 50)); // Lime Green
            progressBar.setBackground(new Color(230, 230, 230)); // Light grey background
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            if (value instanceof Double percent) {
                int intPercent = (int) Math.round(percent);
                if (intPercent < 0) intPercent = 0;
                if (intPercent > 100) intPercent = 100;

                progressBar.setValue(intPercent);
                progressBar.setString(String.format("%.2f%%", percent));
                return progressBar;
            } else {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                return this;
            }
        }
    }

    // --- Simulation Logic ---
    private Queue<Process> readyQueue;
    private Process currentCPUProcess = null;
    // currentProcessQuantumRemaining is already there
    // mlfqQuantums and mlfqAllotments arrays are now class fields

    private void toggleSimulation() {
        if (definedProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No processes defined. Please add processes first.");
            return;
        }

        if (simulationTimer.isRunning()) {
            simulationTimer.stop();
            simulateButton.setText("Simulate");
            actionMessageLabel.setText("Simulation Paused at time " + currentTime);
        } else {
            if (currentTime == 0 || currentSimulationProcesses == null || currentSimulationProcesses.isEmpty() || allProcessesCompleted()) {
                startNewSimulation(); // Start new if first run, or all completed
            }
            simulationTimer.setDelay(simulationSpeed);
            simulationTimer.start();
            actionMessageLabel.setText("Simulation Running...");
            simulateButton.setText("Pause");
        }
    }

    private void startNewSimulation() {
        currentTime = 0;
        currentRunningProcessPID = "None";
        nextInQueuePID = "None";
        ganttChartSequence.clear();
        currentCPUProcess = null;

        // Reset all defined processes to their initial state for a fresh run
        // Create deep copies of defined processes for simulation
        currentSimulationProcesses = new ArrayList<>();
        for (Process p : definedProcesses) {
            p.reset(); // Reset original process first
            currentSimulationProcesses.add(new Process(p)); // Then copy its reset state
        }

        // Initialize algorithm-specific data structures
        String algo = (String) algorithmComboBox.getSelectedItem();
        switch (algo) {
            case "First Come First Serve", "SJF", "SRTF", "Round Robin" -> readyQueue = new LinkedList<>();
            case "MLFQ" -> {
                mlfqQueues = new ArrayList<>();
                for (int i = 0; i < 4; i++) { // Always 4 levels for MLFQ
                    mlfqQueues.add(new LinkedList<>());
                }
                // Read MLFQ quantums and allotments from UI fields
                try {
                    mlfqQuantums[0] = Integer.parseInt(q0QuantumField.getText());
                    mlfqQuantums[1] = Integer.parseInt(q1QuantumField.getText());
                    mlfqQuantums[2] = Integer.parseInt(q2QuantumField.getText());
                    mlfqQuantums[3] = Integer.parseInt(q3QuantumField.getText());

                    mlfqAllotments[0] = Integer.parseInt(q0AllotmentField.getText());
                    mlfqAllotments[1] = Integer.parseInt(q1AllotmentField.getText());
                    mlfqAllotments[2] = Integer.parseInt(q2AllotmentField.getText());
                    mlfqAllotments[3] = Integer.parseInt(q3AllotmentField.getText());

                    // Basic validation
                    for (int i = 0; i < 4; i++) {
                        if (mlfqQuantums[i] <= 0 || mlfqAllotments[i] <= 0) {
                            throw new IllegalArgumentException("MLFQ quantums and allotments must be positive.");
                        }
                    }
                } catch (IllegalArgumentException e) { // Corrected: Just catch IllegalArgumentException
                    JOptionPane.showMessageDialog(this, "Invalid MLFQ quantum/allotment. Please enter positive integers. Using default values.");
                    // Reset to hardcoded defaults if error
                    mlfqQuantums = new int[]{2, 4, 8, 16};
                    mlfqAllotments = new int[]{4, 8, 12, 16};
                    // Update UI to reflect defaults
                    q0QuantumField.setText("2"); q1QuantumField.setText("4"); q2QuantumField.setText("8"); q3QuantumField.setText("16");
                    q0AllotmentField.setText("4"); q1AllotmentField.setText("8"); q2AllotmentField.setText("12"); q3AllotmentField.setText("16");
                }
            }
            default -> {
            }
        }

        updateLiveStatusTable();
        updateMetricsDisplay();
        ganttChartPanel.revalidate();
        ganttChartPanel.repaint();
    }


    private void simulationTick(ActionEvent e) {
        String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();

        // 0. Update waiting times for processes that are ready but not running
        for (Process p : currentSimulationProcesses) {
            // Only increment if arrived, not completed, and not currently running
            if (p.arrivalTime <= currentTime && p.remainingTime > 0 && !p.getFullPid().equals(currentRunningProcessPID)) {
                p.waitingTime++;
            }
        }

        // 1. Add arrived processes to ready queue(s)
        // Processes arriving at 'currentTime' (i.e., at the start of this second) become available.
        // We look for processes whose arrivalTime matches the current second.
        for (Process p : currentSimulationProcesses) {
            if (p.arrivalTime == currentTime && !p.hasArrived && p.remainingTime > 0) {
                p.hasArrived = true;
                switch (selectedAlgorithm) {
                    case "First Come First Serve", "Round Robin" -> readyQueue.add(p);
                    case "SJF", "SRTF" -> // For SJF/SRTF, processes are effectively "ready" once they arrive.
                        // We add them to the queue for consideration, but the selection logic will pick shortest.
                        readyQueue.add(p); // Add to the general pool of available processes
                    case "MLFQ" -> {
                        mlfqQueues.get(0).add(p); // All processes enter Q0
                        p.currentQuantum = mlfqQuantums[0]; // Set initial quantum for Q0
                    }
                }
            }
        }

        // 2. Select next process to run (preemption logic or next in queue)
        Process processToRun = null;

        if (currentCPUProcess != null && currentCPUProcess.remainingTime > 0) {
            // If there's a process currently running, consider if it continues or is preempted
            switch (selectedAlgorithm) {
                case "First Come First Serve", "SJF" -> // SJF is non-preemptive once started
                    processToRun = currentCPUProcess; // Continue current process
                case "SRTF" -> {
                    // Look for shortest remaining time among *all available* processes (current and newly arrived/ready)
                    Process shortestOverall = currentSimulationProcesses.stream()
                            .filter(p -> p.remainingTime > 0 && p.arrivalTime <= currentTime) // Only consider arrived and not completed
                            .min(Comparator.comparingInt(p -> p.remainingTime))
                            .orElse(null);
                    
                    if (shortestOverall != null && shortestOverall != currentCPUProcess) {
                        // Preemption occurs: if currentCPUProcess is not the shortest overall
                        if (currentCPUProcess.remainingTime > 0) {
                            readyQueue.remove(currentCPUProcess); // Remove from queue if it was put there earlier
                            readyQueue.add(currentCPUProcess); // Put current back to ready queue
                        }
                        processToRun = shortestOverall;
                        readyQueue.remove(shortestOverall); // Ensure it's removed from ready queue if it was there
                        actionMessageLabel.setText("SRTF: Preempted " + currentCPUProcess.getFullPid() + " for " + processToRun.getFullPid() + " at time " + currentTime);
                    } else {
                        processToRun = currentCPUProcess; // Continue if no preemption
                    }
                }
                case "Round Robin" -> {
                    currentProcessQuantumRemaining--;
                    if (currentProcessQuantumRemaining > 0) {
                        processToRun = currentCPUProcess; // Continue if quantum not expired
                    } else {
                        // Quantum expired, preempt current process and move to end of queue
                        readyQueue.add(currentCPUProcess);
                        currentCPUProcess = null; // Mark as null to force selection from queue
                        actionMessageLabel.setText("RR: Quantum expired for " + currentCPUProcess.getFullPid() + " at time " + currentTime);
                    }
                }
                case "MLFQ" -> {
                    currentCPUProcess.currentQuantum--;
                    currentCPUProcess.allotmentUsed++;

                    if (currentCPUProcess.currentQuantum <= 0 || currentCPUProcess.allotmentUsed >= mlfqAllotments[currentCPUProcess.currentQueueLevel]) {
                        // Quantum for current level expired OR total allotment for this level reached
                        if (currentCPUProcess.currentQueueLevel < mlfqQueues.size() - 1) {
                            // Demote to next queue
                            currentCPUProcess.currentQueueLevel++;
                            // Reset quantum and allotment for the new level
                            currentCPUProcess.currentQuantum = mlfqQuantums[currentCPUProcess.currentQueueLevel];
                            currentCPUProcess.allotmentUsed = 0;
                            mlfqQueues.get(currentCPUProcess.currentQueueLevel).add(currentCPUProcess); // Add to new queue
                            actionMessageLabel.setText("MLFQ: Demoted " + currentCPUProcess.getFullPid() + " to Q" + currentCPUProcess.currentQueueLevel + " at time " + currentTime);
                        } else {
                            // Already in lowest priority queue, put it back to end of same queue
                            mlfqQueues.get(currentCPUProcess.currentQueueLevel).add(currentCPUProcess);
                            currentCPUProcess.currentQuantum = mlfqQuantums[currentCPUProcess.currentQueueLevel]; // Reset quantum for this level
                            currentCPUProcess.allotmentUsed = 0; // Reset allotment for this level
                            actionMessageLabel.setText("MLFQ: Quantum/Allotment expired for " + currentCPUProcess.getFullPid() + " in Q" + currentCPUProcess.currentQueueLevel + " at time " + currentTime);
                        }
                        currentCPUProcess = null; // Force selection from queues
                    } else {
                        processToRun = currentCPUProcess; // Continue if quantum/allotment not expired
                    }
                }
            }
        }

        // If no process running, or current process finished/preempted, select new one
        if (processToRun == null || processToRun.remainingTime <= 0) {
            switch (selectedAlgorithm) {
                case "First Come First Serve" -> {
                    // Find the process that arrived earliest among the "ready" ones (arrival <= currentTime)
                    // and is not yet completed.
                    Process nextFCFS = readyQueue.stream()
                            .filter(p -> p.remainingTime > 0) // Only non-completed
                            .min(Comparator.comparingInt(p -> p.arrivalTime))
                            .orElse(null);
                    if (nextFCFS != null) readyQueue.remove(nextFCFS); // Remove from queue
                    processToRun = nextFCFS;
                }
                case "SJF" -> {
                    // Non-preemptive, select shortest among all arrived and not completed
                    Process nextSJF = currentSimulationProcesses.stream()
                            .filter(p -> p.remainingTime > 0 && p.arrivalTime <= currentTime)
                            .min(Comparator.comparingInt(p -> p.burstTime)) // Shortest burst time
                            .orElse(null);
                    processToRun = nextSJF;
                }
                case "SRTF" -> {
                    // Select shortest remaining time among all arrived and not completed processes.
                    Process nextSRTF = currentSimulationProcesses.stream()
                            .filter(p -> p.remainingTime > 0 && p.arrivalTime <= currentTime)
                            .min(Comparator.comparingInt(p -> p.remainingTime)) // Shortest remaining time
                            .orElse(null);
                    processToRun = nextSRTF;
                }
                case "Round Robin" -> {
                    // Get next process from ready queue if available
                    if (!readyQueue.isEmpty()) {
                        processToRun = readyQueue.poll();
                        currentProcessQuantumRemaining = Integer.parseInt(timeQuantumField.getText()); // Reset quantum
                    }
                }
                case "MLFQ" -> {
                    // Find highest priority queue with a ready process
                    for (Queue<Process> queue : mlfqQueues) {
                        if (!queue.isEmpty()) {
                            processToRun = queue.poll();
                            // Reset quantum for its current level, if not already reset by demotion
                            if (processToRun.currentQuantum == 0 || processToRun.currentQuantum == -1) { // -1 could mean it just got put back
                                processToRun.currentQuantum = mlfqQuantums[processToRun.currentQueueLevel];
                            }
                            break; // Found a process, exit loop
                        }
                    }
                }
            }
        }

        // 3. Execute the chosen process for 1 time unit
        if (processToRun != null && processToRun.remainingTime > 0) {
            if (processToRun.startTime == -1) {
                processToRun.startTime = currentTime; // Process started at the beginning of this second
                processToRun.responseTime = processToRun.startTime - processToRun.arrivalTime; // Calculate response time
            }
            processToRun.remainingTime--;
            currentCPUProcess = processToRun; // Set current CPU process
            currentRunningProcessPID = processToRun.getFullPid();

            // Store PID for Gantt Chart (with MLFQ level if applicable)
            if ("MLFQ".equals(selectedAlgorithm)) {
                ganttChartSequence.add(processToRun.getFullPid() + "(Q" + processToRun.currentQueueLevel + ")");
            } else {
                ganttChartSequence.add(processToRun.getFullPid());
            }

            actionMessageLabel.setText("Running: " + processToRun.getFullPid() + " at time " + currentTime);

            // If process completed in this tick
            if (processToRun.remainingTime == 0) {
                processToRun.completionTime = currentTime + 1; // Completed at the end of current second
                processToRun.calculateFinalMetrics(); // Calculate TAT, WT, RT
                currentRunningProcessPID = "None"; // CPU is free
                actionMessageLabel.setText(processToRun.getFullPid() + " completed at time " + processToRun.completionTime);

                // If MLFQ, ensure it's removed from all queues (shouldn't be in any, but just in case)
                if ("MLFQ".equals(selectedAlgorithm)) {
                    for(Queue<Process> q : mlfqQueues) {
                        q.remove(processToRun);
                    }
                }
                currentCPUProcess = null; // No process running if it just completed
            }
        } else {
            // CPU is idle
            currentCPUProcess = null;
            currentRunningProcessPID = "Idle";
            ganttChartSequence.add("Idle"); // Add 'Idle' to Gantt chart
            actionMessageLabel.setText("CPU is Idle at time " + currentTime);
        }

        // Increment global time *after* processing the current tick
        // This makes currentTime reflect the elapsed time correctly for the *next* second.
        currentTime++;

        // 4. Determine next in queue for display
        nextInQueuePID = "None";
        switch (selectedAlgorithm) {
            case "First Come First Serve", "Round Robin" -> {
                if (!readyQueue.isEmpty()) {
                    nextInQueuePID = readyQueue.peek().getFullPid();
                }
            }
            case "SJF", "SRTF" -> {
                Process nextCandidateShortest = currentSimulationProcesses.stream()
                        .filter(p -> p.remainingTime > 0 && p.arrivalTime <= currentTime && !p.isCompleted() && !p.equals(currentCPUProcess))
                        .min(Comparator.comparingInt(p -> "SJF".equals(selectedAlgorithm) ? p.burstTime : p.remainingTime))
                        .orElse(null);
                if (nextCandidateShortest != null) {
                    nextInQueuePID = nextCandidateShortest.getFullPid();
                }
            }
            case "MLFQ" -> {
                for (Queue<Process> queue : mlfqQueues) {
                    if (!queue.isEmpty()) {
                        nextInQueuePID = queue.peek().getFullPid() + "(Q" + queue.peek().currentQueueLevel + ")";
                        break;
                    }
                }
            }
        }

        // 5. Update GUI
        updateLiveStatusTable();
        updateMetricsDisplay();
        currentCPULabel.setText("CPU : " + currentRunningProcessPID); // T is now implicit
        nextQueueLabel.setText("Next Queue : " + nextInQueuePID);

        ganttChartPanel.revalidate();
        ganttChartPanel.repaint();

        // 6. Check for simulation completion
        if (allProcessesCompleted()) {
            simulationTimer.stop();
            simulateButton.setText("Simulate");
            actionMessageLabel.setText("Simulation Finished at time " + (currentTime -1) + "."); // Time is one past completion
            updateMetricsDisplay(); // Final update
        }
    }

    private boolean allProcessesCompleted() {
        if (currentSimulationProcesses == null || currentSimulationProcesses.isEmpty()) {
            return definedProcesses.isEmpty();
        }
        return currentSimulationProcesses.stream().allMatch(Process::isCompleted);
    }

    private void updateMetricsDisplay() {
        double totalWaitingTime = 0;
        double totalTurnaroundTime = 0;
        double totalResponseTime = 0;
        double totalActualBurstTime = 0; // Sum of original burst times for average
        int completedProcessesCount = 0;
        int totalOriginalBurstTimeForProgress = 0; // For overall progress bar

        for (Process p : definedProcesses) {
            totalOriginalBurstTimeForProgress += p.burstTime;
        }

        if (currentSimulationProcesses != null) {
            for (Process p : currentSimulationProcesses) {
                if (p.isCompleted()) {
                    totalWaitingTime += p.waitingTime;
                    totalTurnaroundTime += p.turnaroundTime;
                    totalResponseTime += p.responseTime;
                    totalActualBurstTime += p.burstTime; // Sum of original burst times for completed
                    completedProcessesCount++;
                }
            }
        }

        double avgWait = (completedProcessesCount > 0) ? totalWaitingTime / completedProcessesCount : 0.0;
        double avgExec = (completedProcessesCount > 0) ? totalActualBurstTime / completedProcessesCount : 0.0;
        double avgTAT = (completedProcessesCount > 0) ? totalTurnaroundTime / completedProcessesCount : 0.0;
        double avgRT = (completedProcessesCount > 0) ? totalResponseTime / completedProcessesCount : 0.0;

        // Overall progress based on total elapsed time vs. total expected burst time
        double overallProgress = (totalOriginalBurstTimeForProgress > 0) ? ((double) (currentTime -1) / totalOriginalBurstTimeForProgress) * 100 : 0.0;
        overallProgress = Math.min(overallProgress, 100.0); // Cap at 100%

        avgWaitingTimeLabel.setText(String.format("Average Waiting Time : %.2f", avgWait));
        avgExecutionTimeLabel.setText(String.format("Average Burst Time : %.2f", avgExec));
        avgTurnaroundTimeLabel.setText(String.format("Average Turnaround Time : %.2f", avgTAT));
        avgResponseTimeLabel.setText(String.format("Average Response Time : %.2f", avgRT));
        totalExecutionTimeLabel.setText("Total Simulation Time : " + (currentTime -1)); // Display actual elapsed time

        overallProgressBar.setValue((int) Math.round(overallProgress));
        overallProgressBar.setString(String.format("%.2f%%", overallProgress));
    }


    private void resetSimulation() {
        simulationTimer.stop();
        simulateButton.setText("Simulate");
        currentTime = 0;
        currentRunningProcessPID = "None";
        nextInQueuePID = "None";
        ganttChartSequence.clear();
        currentCPUProcess = null;

        // Reset all defined processes to their initial state for a fresh run
        for (Process p : definedProcesses) {
            p.reset();
        }
        currentSimulationProcesses = null; // Clear the simulation-specific list

        // Clear tables
        // processDefinitionModel.setRowCount(0); // No, keep definitions
        liveStatusModel.setRowCount(0); // Clear live status table

        // Re-populate definition table from definedProcesses (they are reset)
        updateProcessDefinitionTable(); // This will refresh and sort

        updateMetricsDisplay(); // Reset metrics to zero
        actionMessageLabel.setText("Simulation reset. Define processes and click Simulate.");

        ganttChartPanel.revalidate();
        ganttChartPanel.repaint();
    }

    // Helper to get consistent color for a process PID
    private Color getColorForProcess(String fullPid) {
        return processColors.computeIfAbsent(fullPid, k -> {
            Random rand = new Random();
            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();
            // Avoid very light colors that blend with white background, and too dark
            return new Color(r, g, b).brighter().brighter(); // Ensures some brightness
        });
    }

    private void drawGanttChart(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int blockWidth = GANTT_BLOCK_WIDTH;
        int rowHeight = GANTT_ROW_HEIGHT;
        int yTimeAxis = 15; // Y position for time numbers

        // Draw Time Axis
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawLine(0, GANTT_TIME_AXIS_HEIGHT, getWidth(), GANTT_TIME_AXIS_HEIGHT); // Horizontal line for axis
        // Draw up to currentTime (exclusive), then the current time (inclusive)
        int maxTimeOnAxis = currentTime > 0 ? currentTime : 0;
        for (int i = 0; i <= maxTimeOnAxis +1 ; i++) { // +1 to ensure current time tick is drawn
            String timeStr = String.valueOf(i);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(timeStr);
            g2d.drawString(timeStr, i * blockWidth - textWidth / 2, yTimeAxis); // Centered time label
            if (i > 0) {
                g2d.drawLine(i * blockWidth, GANTT_TIME_AXIS_HEIGHT - 5, i * blockWidth, GANTT_TIME_AXIS_HEIGHT + 5); // Tick marks
            }
        }

        int yBlockStart = GANTT_TIME_AXIS_HEIGHT + 10; // Start drawing blocks below the axis

        // Draw Gantt blocks
        int currentX = 0;
        String selectedAlgorithm = (String) algorithmComboBox.getSelectedItem();

        for (String ganttEntry : ganttChartSequence) {
            String actualPid = ganttEntry;
            String mlfqLevel = null;

            // Extract MLFQ level if present
            if ("MLFQ".equals(selectedAlgorithm) && ganttEntry.contains("(Q")) {
                int levelStart = ganttEntry.indexOf("(Q");
                mlfqLevel = ganttEntry.substring(levelStart);
                actualPid = ganttEntry.substring(0, levelStart);
            }

            Color processColor = getColorForProcess(actualPid);
            g2d.setColor(processColor);
            g2d.fillRect(currentX, yBlockStart, blockWidth, rowHeight); // Draw the block

            g2d.setColor(Color.BLACK); // Text and border color
            g2d.drawRect(currentX, yBlockStart, blockWidth, rowHeight); // Draw border

            // Draw PID in the center of the block
            FontMetrics fm = g2d.getFontMetrics();
            int textY = yBlockStart + ((rowHeight - fm.getHeight()) / 2) + fm.getAscent();

            if (mlfqLevel != null) {
                // Draw PID (smaller) and MLFQ level (smaller)
                g2d.setFont(g2d.getFont().deriveFont(8f)); // Smaller font for PID
                fm = g2d.getFontMetrics();
                int pidTextWidth = fm.stringWidth(actualPid);
                int pidTextX = currentX + (blockWidth - pidTextWidth) / 2;
                g2d.drawString(actualPid, pidTextX, yBlockStart + fm.getAscent() + 2); // Slightly above center

                g2d.setFont(g2d.getFont().deriveFont(8f)); // Smaller font for level
                fm = g2d.getFontMetrics();
                int levelTextWidth = fm.stringWidth(mlfqLevel);
                int levelTextX = currentX + (blockWidth - levelTextWidth) / 2;
                g2d.drawString(mlfqLevel, levelTextX, yBlockStart + rowHeight - 4); // Near bottom
                g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 12f)); // Reset font
            } else {
                // Draw PID normally
                int textWidth = fm.stringWidth(actualPid);
                int textX = currentX + (blockWidth - textWidth) / 2;
                g2d.drawString(actualPid, textX, textY);
            }

            currentX += blockWidth;
        }
    }


    private void exportResultsToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Simulation Results");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("simulation_results.txt")); // Default file name

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getAbsolutePath().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write("CPU Scheduling Simulation Results\n");
                writer.write("Developed By: Tenchavez Rieznick McCain G.\n");
                writer.write("----------------------------------------------------------------\n");
                writer.write("Algorithm: " + algorithmComboBox.getSelectedItem() + "\n");
                if ("Round Robin".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("Time Quantum: " + timeQuantumField.getText() + "\n");
                } else if ("MLFQ".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("MLFQ Quantums (Q0-Q3): " + mlfqQuantums[0] + ", " + mlfqQuantums[1] + ", " + mlfqQuantums[2] + ", " + mlfqQuantums[3] + "\n");
                    writer.write("MLFQ Allotments (Q0-Q3): " + mlfqAllotments[0] + ", " + mlfqAllotments[1] + ", " + mlfqAllotments[2] + ", " + mlfqAllotments[3] + "\n");
                }
                writer.write("Total Simulation Time: " + (currentTime - 1) + "\n");
                writer.write("----------------------------------------------------------------\n\n");

                // --- Gantt Chart ---
                writer.write("Gantt Chart:\n");
                StringBuilder ganttOutput = new StringBuilder();
                StringBuilder timeAxis = new StringBuilder();
                StringBuilder divider = new StringBuilder();

                int currentGanttTime = 0;
                for (String entry : ganttChartSequence) {
                    ganttOutput.append(String.format("| %-3s ", entry));
                    divider.append("------");
                    timeAxis.append(String.format("%-6s", currentGanttTime));
                    currentGanttTime++;
                }
                ganttOutput.append("|\n");
                divider.append("-\n"); // for the last '|'
                StringBuilder append = timeAxis.append(String.format("%-6s", currentGanttTime)).append("\n");

                writer.write(divider.toString());
                writer.write(ganttOutput.toString());
                writer.write(divider.toString());
                writer.write(timeAxis.toString());
                writer.write("\n");

                // --- Process Metrics Table ---
                writer.write("Process Metrics:\n");
                String header = String.format("%-10s %-10s %-10s %-10s %-10s %-10s %-10s\n",
                                                "Process ID", "Arrival", "Burst", "Completion", "Turnaround", "Response", "Waiting");
                writer.write(header);
                writer.write(String.join("", Collections.nCopies(header.length() -1, "-")) + "\n");

                for (Process p : currentSimulationProcesses) {
                    writer.write(String.format("%-10s %-10d %-10d %-10d %-10d %-10d %-10d\n",
                                                p.getFullPid(), p.arrivalTime, p.burstTime, p.completionTime,
                                                p.turnaroundTime, p.responseTime, p.waitingTime));
                }
                writer.write("\n");

                // --- Average Metrics ---
                writer.write("Average Metrics:\n");
                writer.write(avgWaitingTimeLabel.getText() + "\n");
                writer.write(avgExecutionTimeLabel.getText() + "\n");
                writer.write(avgTurnaroundTimeLabel.getText() + "\n");
                writer.write(avgResponseTimeLabel.getText() + "\n");

                JOptionPane.showMessageDialog(this, "Results exported successfully to:\n" + fileToSave.getAbsolutePath());

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting results: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
        }

        SwingUtilities.invokeLater(() -> new SchedulerGUI());
    }

    // --- Process Class ---
    static final class Process {
        String pid;
        String extension;
        int arrivalTime;
        int burstTime;
        int priority;
        int remainingTime;
        int startTime; // Time when process first starts execution (for Response Time)
        int completionTime; // Time when process finishes
        int waitingTime; // Total time spent waiting in ready queue
        int turnaroundTime; // Completion Time - Arrival Time
        int responseTime; // Start Time - Arrival Time

        // For MLFQ
        int currentQueueLevel;
        int currentQuantum; // Remaining quantum for current level (resets per level)
        int allotmentUsed; // Used within current level's total allotment

        boolean hasArrived; // To track if process has been added to ready queue system

        public Process(String pid, String extension, int arrivalTime, int burstTime, int priority) {
            this.pid = pid;
            this.extension = extension;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            reset(); // Initialize/reset all simulation-related fields
        }

        // Copy constructor for simulation
        public Process(Process other) {
            this.pid = other.pid;
            this.extension = other.extension;
            this.arrivalTime = other.arrivalTime;
            this.burstTime = other.burstTime;
            this.priority = other.priority;
            reset(); // Copying should also start in a reset state for simulation
        }

        public String getFullPid() {
            return pid + extension;
        }

        public boolean isCompleted() {
            return remainingTime <= 0 && completionTime != -1;
        }

        public void calculateFinalMetrics() {
            // These should be called when remainingTime becomes 0
            if (this.completionTime != -1) {
                this.turnaroundTime = this.completionTime - this.arrivalTime;
                // Response time is calculated when startTime is first set
                // Waiting time is accumulated per tick, so just ensure it's finalized
            }
        }

        public void reset() {
            this.remainingTime = this.burstTime;
            this.startTime = -1; // -1 indicates not started yet
            this.completionTime = -1; // -1 indicates not completed yet
            this.waitingTime = 0;
            this.turnaroundTime = 0;
            this.responseTime = 0; // Or -1 if not started

            this.currentQueueLevel = 0; // Default for MLFQ
            this.currentQuantum = -1; // Will be set when added to MLFQ queue
            this.allotmentUsed = 0;
            this.hasArrived = false;
        }

        @Override
        public String toString() {
            return getFullPid();
        }
    }
}