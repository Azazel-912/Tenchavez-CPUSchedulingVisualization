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
import javax.swing.filechooser.FileNameExtensionFilter;

public class SchedulerGUI extends JFrame {

    // --- GUI Components ---
    // Left Panels - Process Definition and Input
    private JTextField pidField, arrivalTimeField, execTimeField, priorityField;
    private JComboBox<String> processNameDropdown;
    private JTextField randomLengthField;
    private JButton enqueueButton, dequeueButton, updateButton, generateRandomBtn;
    private JButton dequeueAllButton;
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
    private JScrollPane ganttScrollPane; // Declare ganttScrollPane here to make it accessible

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
    private List<GanttBlock> ganttChartSequence = new ArrayList<>(); // Now stores GanttBlock objects
    private Map<String, Color> processColors = new HashMap<>(); // To keep consistent colors for processes

    // --- Gantt Chart specific constants ---
    private static final int GANTT_BLOCK_WIDTH = 45; // Increased from 30
    private static final int GANTT_ROW_HEIGHT = 35; // Increased from 20
    private static final int GANTT_TIME_AXIS_HEIGHT = 25; // Slightly increased for labels
    private static final int GANTT_PANEL_FIXED_HEIGHT = GANTT_TIME_AXIS_HEIGHT + GANTT_ROW_HEIGHT + 25; // Adjusted total fixed height

    // --- Colors ---
    private final Color CREAM = new Color(255, 253, 208);
    private final Color DARK_BLUE_BACKGROUND = new Color(30, 30, 60); // For action message and possibly other dark elements
    private final Color LIGHT_GRAY_BORDER = new Color(200, 200, 200);

    // Algorithm-specific data structures
    private Queue<Process> Queue; // General purpose queue for FCFS, RR, and as a pool for SJF/SRTF
    private Process currentCPUProcess = null; // Renamed from crentCPUProcess
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
        // Set initial message on the correctly initialized label
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
        arrivalTimeField = new JTextField("0", 3);
        execTimeField = new JTextField("10", 3);
        priorityField = new JTextField("5", 3);
        randomLengthField = new JTextField("5", 3); // Field for generating random processes

        // Process Action Buttons
        generateRandomBtn = new JButton("Generate Random");
        enqueueButton = new JButton("Enqueue");
        dequeueButton = new JButton("Dequeue");
        updateButton = new JButton("Update");
        dequeueAllButton = new JButton("Dequeue All");

        // Process Definition Table
        processDefinitionModel = new DefaultTableModel(
                new Object[]{"#", "Process", "Arrival Time", "Exec. Time", "Priority"}, 0 // Removed Extension column
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

        // Action Message Label Initialization (ALL properties set here)
        actionMessageLabel = new JLabel("Status Message Here", SwingConstants.CENTER);
        actionMessageLabel.setOpaque(true);
        actionMessageLabel.setBackground(DARK_BLUE_BACKGROUND);
        actionMessageLabel.setForeground(Color.WHITE);
        actionMessageLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        actionMessageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        actionMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // For BoxLayout centering
        actionMessageLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, actionMessageLabel.getPreferredSize().height)); // Allow stretching horizontally


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
        labelTable.put(50, new JLabel("<html>x100<br>(Fast)</html>")); 
        labelTable.put(300, new JLabel("x10"));
        labelTable.put(500, new JLabel("x5"));
        labelTable.put(750, new JLabel("x2"));
        labelTable.put(1000, new JLabel("<html>x1<br>(Slow)</html>"));
        simulationSpeedSlider.setLabelTable(labelTable);
        // Set a smaller preferred width for the slider to prevent it from pushing buttons down
        simulationSpeedSlider.setPreferredSize(new Dimension(220, simulationSpeedSlider.getPreferredSize().height)); 


        simulateButton = new JButton("Simulate");
        resetButton = new JButton("Reset All");
        exportResultsBtn = new JButton("Export Results"); // New button

        // Initialize simulation timer (not started yet)
        simulationTimer = new Timer(simulationSpeed, this::simulationTick);
    }
    private void dequeueAllProcesses() {
    if (definedProcesses.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No processes to dequeue.", "Information", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to dequeue all defined processes? This will also reset the simulation.",
            "Confirm Dequeue All", JOptionPane.YES_NO_OPTION);

    if (confirm == JOptionPane.YES_OPTION) {
        definedProcesses.clear(); // Clear the list of defined processes
        processColors.clear(); // Clear process colors as well
        updateProcessDefinitionTable(); // Update the table display
        updateProcessDefinitionDropdown(); // Update the dropdown
        resetSimulation(); // Reset the simulation state
        actionMessageLabel.setText("All processes dequeued and simulation reset.");
    }
}

    private void setupLayout() {
        // --- Left Panel (Process Definition, Algorithm, Gantt) ---
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(CREAM);

        // Fixed width for the left panel to control overall layout
        int fixedLeftPanelWidth = 700; // Increased width for better label display
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
        JPanel individualProcessInputPanel = new JPanel(new GridLayout(2, 4, 5, 5)); // Changed to 4 columns
        individualProcessInputPanel.setBackground(CREAM);
        individualProcessInputPanel.add(new JLabel("Process:"));
        individualProcessInputPanel.add(new JLabel("Arrival Time:"));
        individualProcessInputPanel.add(new JLabel("Exec. Time:"));
        individualProcessInputPanel.add(new JLabel("Priority:"));

        individualProcessInputPanel.add(pidField);
        individualProcessInputPanel.add(arrivalTimeField);
        individualProcessInputPanel.add(execTimeField);
        individualProcessInputPanel.add(priorityField);
        processInputSection.add(individualProcessInputPanel, BorderLayout.CENTER);

        // Row 3: Action Buttons (Enqueue, Dequeue, Update)
        JPanel processActionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        processActionButtonsPanel.setBackground(CREAM);
        processActionButtonsPanel.add(enqueueButton);
        processActionButtonsPanel.add(dequeueButton);
        processActionButtonsPanel.add(dequeueAllButton);
        processActionButtonsPanel.add(updateButton);
        processInputSection.add(processActionButtonsPanel, BorderLayout.SOUTH);

        leftPanel.add(processInputSection, BorderLayout.NORTH);


        // Middle-Left: Process Definition Table
        JPanel processTablePanel = new JPanel(new BorderLayout());
        processTablePanel.setBackground(CREAM);
        processTablePanel.setBorder(BorderFactory.createTitledBorder("Processes Defined"));
        processTablePanel.add(new JScrollPane(processDefinitionTable), BorderLayout.CENTER);
        leftPanel.add(processTablePanel, BorderLayout.CENTER);

        // --- Bottom-Left: Algorithm, Action Message, MLFQ Config, Gantt Chart ---
        // Create a new panel to hold algorithm, MLFQ config, action message, and Gantt
        JPanel bottomAreaContainer = new JPanel();
        bottomAreaContainer.setLayout(new BoxLayout(bottomAreaContainer, BoxLayout.Y_AXIS));
        bottomAreaContainer.setBackground(CREAM);
        bottomAreaContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add some padding

        // Add Algorithm Panel
        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        algorithmPanel.setBackground(CREAM);
        algorithmPanel.add(new JLabel("Algorithm:"));
        algorithmPanel.add(algorithmComboBox);
        algorithmPanel.add(new JLabel("Time Quantum for RR:"));
        algorithmPanel.add(timeQuantumField);
        bottomAreaContainer.add(algorithmPanel);

        // Add MLFQ Configuration Panel
        JPanel mlfqConfigPanel = new JPanel(new GridBagLayout()); // Keep current MLFQ config layout
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
        bottomAreaContainer.add(mlfqConfigPanel); // Add MLFQ config panel to the new container

        // Add Action Message Label
        bottomAreaContainer.add(actionMessageLabel);

        // Add Gantt Chart
        JPanel ganttChartWrapper = new JPanel(new BorderLayout());
        ganttChartWrapper.setBackground(CREAM);
        ganttChartWrapper.setBorder(BorderFactory.createTitledBorder("Gantt Chart (Each box represents a second)"));

        ganttScrollPane = new JScrollPane(ganttChartPanel); // Initialize ganttScrollPane here
        ganttScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        ganttScrollPane.setPreferredSize(new Dimension(fixedLeftPanelWidth - 20, GANTT_PANEL_FIXED_HEIGHT + 5));
        ganttScrollPane.setMinimumSize(new Dimension(fixedLeftPanelWidth - 20, GANTT_PANEL_FIXED_HEIGHT + 5));
        ganttChartWrapper.add(ganttScrollPane, BorderLayout.CENTER);
        bottomAreaContainer.add(ganttChartWrapper); // Add Gantt chart wrapper to the new container

        leftPanel.add(bottomAreaContainer, BorderLayout.SOUTH); // Add the new container to the left panel's SOUTH
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
        metricsPanel.add(avgResponseTimeLabel);    // New
        metricsPanel.add(totalExecutionTimeLabel);
        metricsPanel.add(currentCPULabel);
        metricsPanel.add(nextQueueLabel);
        metricsPanel.add(overallProgressBar); // CORRECTED LINE: overallProgressBar should be added to metricsPanel
        rightPanel.add(metricsPanel, BorderLayout.CENTER);

        // Bottom-Right: Simulation Controls
         JPanel speedControlWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); 
        speedControlWrapperPanel.setBackground(CREAM);
        speedControlWrapperPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); 
        speedControlWrapperPanel.add(new JLabel("Simulation Speed:"));
        speedControlWrapperPanel.add(simulationSpeedSlider);
        speedControlWrapperPanel.add(simulateButton);
        speedControlWrapperPanel.add(resetButton);
        speedControlWrapperPanel.add(exportResultsBtn); 
        rightPanel.add( speedControlWrapperPanel, BorderLayout.SOUTH);

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
        dequeueAllButton.addActionListener(e -> dequeueAllProcesses());
        // Simulation Control Buttons
        simulateButton.addActionListener(e -> toggleSimulation());
        resetButton.addActionListener(e -> resetSimulation());
        exportResultsBtn.addActionListener(e -> exportResultsToFile()); // Listener for new button

        // Simulation Speed Slider Listener
        simulationSpeedSlider.addChangeListener(e -> {
            simulationSpeed = simulationSpeedSlider.getValue();
            // ALWAYS update the timer's delay when the slider changes
            simulationTimer.setDelay(simulationSpeed);
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

            // Check for duplicate PID
            if (definedProcesses.stream().anyMatch(p -> p.pid.equals(pid))) {
                JOptionPane.showMessageDialog(this, "Process with PID '" + pid + "' already exists. Use Update to modify.");
                return;
            }

            Process newProcess = new Process(pid, at, bt, prio);
            definedProcesses.add(newProcess);
            updateProcessDefinitionTable();
            updateProcessDefinitionDropdown();
            actionMessageLabel.setText("Process " + newProcess.pid + " enqueued.");

            // Clear input fields after adding
            pidField.setText("");
            arrivalTimeField.setText("0");
            execTimeField.setText("10");
            priorityField.setText("5");

            // Assign a color for the new process for Gantt chart
            getColorForProcess(newProcess.pid);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format for Arrival Time, Exec. Time, or Priority.");
        }
    }

    private void deleteSelectedProcess() {
        int selectedRow = processDefinitionTable.getSelectedRow();
        if (selectedRow != -1) {
            String pidToDelete = (String) processDefinitionModel.getValueAt(selectedRow, 1);
            definedProcesses.removeIf(p -> p.pid.equals(pidToDelete));
            updateProcessDefinitionTable();
            updateProcessDefinitionDropdown();
            actionMessageLabel.setText("Process " + pidToDelete + " dequeued.");
        } else {
            JOptionPane.showMessageDialog(this, "Please select a process to delete.");
        }
    }

    private void updateSelectedProcess() {
        int selectedRow = processDefinitionTable.getSelectedRow();
        if (selectedRow != -1) {
            try {
                String oldPidName = (String) processDefinitionModel.getValueAt(selectedRow, 1);

                String newPidName = (String) processDefinitionModel.getValueAt(selectedRow, 1);
                int newAt = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 2).toString());
                int newBt = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 3).toString());
                int newPrio = Integer.parseInt(processDefinitionModel.getValueAt(selectedRow, 4).toString());

                if (newBt <= 0) {
                    JOptionPane.showMessageDialog(this, "Burst Time must be greater than 0.");
                    updateProcessDefinitionTable();
                    return;
                }

                // Check for duplicate PID if PID changed to an existing one
                if (!oldPidName.equals(newPidName) && definedProcesses.stream().anyMatch(p -> p.pid.equals(newPidName))) {
                    JOptionPane.showMessageDialog(this, "A process with PID '" + newPidName + "' already exists. Cannot update to a duplicate PID.");
                    updateProcessDefinitionTable();
                    return;
                }

                for (Process p : definedProcesses) {
                    if (p.pid.equals(oldPidName)) {
                        p.pid = newPidName;
                        p.arrivalTime = newAt;
                        p.burstTime = newBt;
                        p.priority = newPrio;
                        p.reset(); // Reset simulation-related times on update
                        actionMessageLabel.setText("Process " + p.pid + " updated.");
                        updateProcessDefinitionTable();
                        updateProcessDefinitionDropdown();
                        return;
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input in table. Please check numerical values.");
                updateProcessDefinitionTable();
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
                int counter = definedProcesses.size() + 1;

                do {
                    generatedPid = "P" + counter;
                    final String currentPidForLambda = generatedPid;
                    counter++;
                    if (definedProcesses.stream().anyMatch(p -> p.pid.equals(currentPidForLambda))) {
                    } else {
                        break;
                    }
                } while (true);

                int at = rand.nextInt(20); // Arrival time 0-19
                int bt = rand.nextInt(20) + 1; // Burst time 1-20 (must be > 0)
                int prio = rand.nextInt(10) + 1; // Priority 1-10

                definedProcesses.add(new Process(generatedPid, at, bt, prio));
                getColorForProcess(generatedPid);
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
                    p.arrivalTime,
                    p.burstTime,
                    p.priority
            });
        }
    }

    private void updateProcessDefinitionDropdown() {
        processNameDropdown.removeAllItems();
        for (Process p : definedProcesses) {
            processNameDropdown.addItem(p.pid);
        }
    }

    private void updateLiveStatusTable() {
        liveStatusModel.setRowCount(0); // Clear existing rows
        if (currentSimulationProcesses == null) return;

        // Sort by PID for consistent display
        currentSimulationProcesses.sort(Comparator.comparing(p -> p.pid));

        for (Process p : currentSimulationProcesses) {
            String status;
            if (p.hasCompleted) {
                status = "Completed";
            } else if (p.getRemainingBurstTime() < p.burstTime) { // Started but not finished
                status = "Running"; // Or "Waiting" if not currentCPUProcess
            } else {
                status = "Ready"; // Not yet started
            }

            // Refine "Running" vs "Waiting" for current simulation tick
            if (currentCPUProcess != null && currentCPUProcess.pid.equals(p.pid) && !p.hasCompleted) {
                status = "Running";
            } else if (!p.hasCompleted && p.getRemainingBurstTime() < p.burstTime) { // Has run before, but not running now
                status = "Waiting";
            } else if (p.arrivalTime > currentTime) {
                status = "Scheduled"; // Not yet arrived
            } else if (!p.hasCompleted) {
                status = "Ready"; // Arrived, not yet run, not currently running
            }


            double completionPercentage = (double) (p.burstTime - p.getRemainingBurstTime()) / p.burstTime * 100;
            if (p.hasCompleted) completionPercentage = 100.0;

            liveStatusModel.addRow(new Object[]{
                    p.pid,
                    status,
                    completionPercentage, // This column will use ProgressBarRenderer
                    p.getRemainingBurstTime(),
                    p.waitingTime,
                    p.completionTime > 0 ? p.completionTime : "-", // Display actual completion time or "-"
                    p.turnaroundTime > 0 ? p.turnaroundTime : "-", // Display actual turnaround time or "-"
                    p.responseTime > 0 ? p.responseTime : "-" // Display actual response time or "-"
            });
        }
    }

    private void updateMetricsDisplay() {
        if (currentSimulationProcesses == null || currentSimulationProcesses.isEmpty()) {
            avgWaitingTimeLabel.setText("Average Waiting Time : 0.00");
            avgExecutionTimeLabel.setText("Average Burst Time : 0.00");
            avgTurnaroundTimeLabel.setText("Average Turnaround Time : 0.00");
            avgResponseTimeLabel.setText("Average Response Time : 0.00");
            totalExecutionTimeLabel.setText("Total Simulation Time : 0");
            currentCPULabel.setText("CPU : None");
            nextQueueLabel.setText("Next Queue : None");
            overallProgressBar.setValue(0);
            overallProgressBar.setString("0.00%");
            return;
        }

        List<Process> completedProcesses = currentSimulationProcesses.stream()
                .filter(p -> p.hasCompleted)
                .collect(Collectors.toList());

        double totalWaitingTime = completedProcesses.stream().mapToInt(p -> p.waitingTime).sum();
        double totalTurnaroundTime = completedProcesses.stream().mapToInt(p -> p.turnaroundTime).sum();
        double totalResponseTime = completedProcesses.stream().mapToInt(p -> p.responseTime).sum();
        double totalBurstTime = currentSimulationProcesses.stream().mapToInt(p -> p.burstTime).sum();


        avgWaitingTimeLabel.setText(String.format("Average Waiting Time : %.2f", completedProcesses.isEmpty() ? 0.0 : totalWaitingTime / completedProcesses.size()));
        avgTurnaroundTimeLabel.setText(String.format("Average Turnaround Time : %.2f", completedProcesses.isEmpty() ? 0.0 : totalTurnaroundTime / completedProcesses.size()));
        avgResponseTimeLabel.setText(String.format("Average Response Time : %.2f", completedProcesses.isEmpty() ? 0.0 : totalResponseTime / completedProcesses.size()));
        avgExecutionTimeLabel.setText(String.format("Average Burst Time : %.2f", currentSimulationProcesses.isEmpty() ? 0.0 : totalBurstTime / currentSimulationProcesses.size()));
        totalExecutionTimeLabel.setText("Total Simulation Time : " + currentTime);

        currentCPULabel.setText("CPU : " + currentRunningProcessPID);
        nextQueueLabel.setText("Next Queue : " + nextInQueuePID);

        int totalOriginalBurstTime = definedProcesses.stream().mapToInt(p -> p.burstTime).sum();
        int totalRemainingBurstTime = currentSimulationProcesses.stream().mapToInt(p -> p.getRemainingBurstTime()).sum();
        int totalExecutedTime = totalOriginalBurstTime - totalRemainingBurstTime;

        if (totalOriginalBurstTime > 0) {
            int progress = (int) ((double) totalExecutedTime / totalOriginalBurstTime * 100);
            overallProgressBar.setValue(progress);
            overallProgressBar.setString(String.format("%.2f%%", (double) totalExecutedTime / totalOriginalBurstTime * 100));
        } else {
            overallProgressBar.setValue(0);
            overallProgressBar.setString("0.00%");
        }
    }


    private void toggleSimulation() {
        if (simulationTimer.isRunning()) {
            simulationTimer.stop();
            simulateButton.setText("Resume");
            actionMessageLabel.setText("Simulation Paused at Time " + currentTime);
        } else {
            if (currentSimulationProcesses == null || currentSimulationProcesses.isEmpty()) {
                startNewSimulation();
            }
            simulationTimer.start();
            simulateButton.setText("Pause");
            actionMessageLabel.setText("Simulation Running...");
        }
    }

    private void startNewSimulation() {
        if (definedProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please define processes before starting the simulation.");
            return;
        }

        // Deep copy defined processes to simulation processes
        currentSimulationProcesses = definedProcesses.stream()
                .map(Process::new) // Uses the copy constructor
                .collect(Collectors.toList());

        // Sort by arrival time initially
        currentSimulationProcesses.sort(Comparator.comparingInt(p -> p.arrivalTime));

        currentTime = 0;
        ganttChartSequence.clear();
        processColors.clear(); // Clear colors to re-assign if processes changed
        currentRunningProcessPID = "None";
        nextInQueuePID = "None";
        currentCPUProcess = null; // Initialize currentCPUProcess to null at start of new simulation
        currentProcessQuantumRemaining = 0;

        // Initialize algorithm-specific queues
        String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
        switch (selectedAlgo) {
            case "First Come First Serve", "Round Robin" -> Queue = new LinkedList<>();
            case "SJF", "SRTF" -> Queue = new PriorityQueue<>(Comparator.comparingInt(Process::getRemainingBurstTime));
            case "MLFQ" -> {
                mlfqQueues = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    mlfqQueues.add(new LinkedList<>());
                }
                // Read MLFQ quantums and allotments
                try {
                    mlfqQuantums[0] = Integer.parseInt(q0QuantumField.getText().trim());
                    mlfqQuantums[1] = Integer.parseInt(q1QuantumField.getText().trim());
                    mlfqQuantums[2] = Integer.parseInt(q2QuantumField.getText().trim());
                    mlfqQuantums[3] = Integer.parseInt(q3QuantumField.getText().trim());

                    mlfqAllotments[0] = Integer.parseInt(q0AllotmentField.getText().trim());
                    mlfqAllotments[1] = Integer.parseInt(q1AllotmentField.getText().trim());
                    mlfqAllotments[2] = Integer.parseInt(q2AllotmentField.getText().trim());
                    mlfqAllotments[3] = Integer.parseInt(q3AllotmentField.getText().trim());

                    for (int q : mlfqQuantums) if (q <= 0) throw new IllegalArgumentException("MLFQ quantums must be > 0");
                    for (int a : mlfqAllotments) if (a < 0) throw new IllegalArgumentException("MLFQ allotments must be >= 0");

                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid number for MLFQ quantum/allotment.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
                    resetSimulation();
                    return;
                } catch (IllegalArgumentException e) {
                    JOptionPane.showMessageDialog(this, "MLFQ configuration error: " + e.getMessage(), "Configuration Error", JOptionPane.ERROR_MESSAGE);
                    resetSimulation();
                    return;
                }
            }
        }

        actionMessageLabel.setText("Simulation started for " + selectedAlgo + ".");
        updateLiveStatusTable();
        updateMetricsDisplay();
        ganttChartPanel.revalidate();
        ganttChartPanel.repaint();
    }


    private void simulationTick(ActionEvent e) {
        // --- Initial checks and simulation termination ---
        if (currentSimulationProcesses == null || currentSimulationProcesses.isEmpty()) {
            simulationTimer.stop();
            simulateButton.setText("Simulate");
            actionMessageLabel.setText("Simulation Finished at Time " + currentTime);
            updateLiveStatusTable(); // Final update
            updateMetricsDisplay(); // Final update
            ganttChartPanel.revalidate();
            ganttChartPanel.repaint();
            return; // Exit the method as simulation is over
        }

        // 1. Handle processes arriving at the current time
        List<Process> arrivedProcesses = currentSimulationProcesses.stream()
                .filter(p -> p.arrivalTime == currentTime && !p.hasArrived)
                .collect(Collectors.toList());

        for (Process p : arrivedProcesses) {
            p.hasArrived = true;
            String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
            if (null == selectedAlgo) {
                Queue.offer(p); // For FCFS and RR, add to regular queue
            } else switch (selectedAlgo) {
                case "MLFQ" -> mlfqQueues.get(0).offer(p); // Arrive at Q0 for MLFQ
                case "SRTF", "SJF" -> // For SJF/SRTF, they enter the priority queue
                    Queue.offer(p);
                default -> Queue.offer(p); // For FCFS and RR, add to regular queue
            }
        }

        // 2. Select the next process to run based on the chosen algorithm
        String selectedAlgo = (String) algorithmComboBox.getSelectedItem();
        Process previouslyRunningProcess = currentCPUProcess;
        Process processToRunThisTick = null;

        switch (selectedAlgo) {
            case "First Come First Serve" -> processToRunThisTick = FCFS(previouslyRunningProcess);
            case "SJF" -> processToRunThisTick = SJF(previouslyRunningProcess);
            case "SRTF" -> processToRunThisTick = SRTF(previouslyRunningProcess);
            case "Round Robin" -> processToRunThisTick = RoundRobin(previouslyRunningProcess);
            case "MLFQ" -> processToRunThisTick = MLFQ(previouslyRunningProcess);
        }

        currentCPUProcess = processToRunThisTick; // Update the main CPU reference
        currentRunningProcessPID = (currentCPUProcess != null) ? currentCPUProcess.pid : "None";

        // Determine next in queue for display (based on current state after selection)
        updateNextInQueueDisplay(selectedAlgo);


        // 3. Execute the selected process for 1 time unit, or mark CPU as idle
        if (currentCPUProcess != null) { // If there's a process to run
            if (currentCPUProcess.getRemainingBurstTime() > 0) {
                // Set response time if this is the first time the process gets CPU
                if (currentCPUProcess.responseTime == -1) {
                    currentCPUProcess.responseTime = currentTime - currentCPUProcess.arrivalTime;
                }
                // Decrement remaining burst time
                currentCPUProcess.setRemainingBurstTime(currentCPUProcess.getRemainingBurstTime() - 1);

                // Add the process's block to the Gantt Chart sequence
                if ("MLFQ".equals(selectedAlgo)) {
                    ganttChartSequence.add(new GanttBlock(currentCPUProcess.pid, currentCPUProcess.currentQueueLevel));
                } else {
                    ganttChartSequence.add(new GanttBlock(currentCPUProcess.pid, -1)); // -1 for non-MLFQ
                }
            }

            // 4. Check for process completion or preemption after executing for this tick
            if (currentCPUProcess.getRemainingBurstTime() == 0) {
                // Process has completed
                currentCPUProcess.hasCompleted = true;
                currentCPUProcess.completionTime = currentTime + 1; // Completed at end of this tick
                currentCPUProcess.turnaroundTime = currentCPUProcess.completionTime - currentCPUProcess.arrivalTime;
                currentCPUProcess.waitingTime = currentCPUProcess.turnaroundTime - currentCPUProcess.burstTime;

                // For MLFQ, if a process completes, it's done. Remove it from any queue it might still be in.
                if ("MLFQ".equals(selectedAlgo)) {
                    for (Queue<Process> q : mlfqQueues) {
                        q.remove(currentCPUProcess); // Safe to remove non-existent items
                    }
                }

                currentCPUProcess = null; // CPU is now free
                currentProcessQuantumRemaining = 0; // Reset quantum for next process
            } else {
                // Process is still running, handle preemption or quantum exhaustion for RR/MLFQ
                if ("Round Robin".equals(selectedAlgo)) {
                    currentProcessQuantumRemaining--;
                    if (currentProcessQuantumRemaining == 0) {
                        // Quantum exhausted, preempt and re-add to end of queue
                        Queue.offer(currentCPUProcess);
                        currentCPUProcess = null; // CPU is free for next tick to pick from queue
                    }
                } else if ("MLFQ".equals(selectedAlgo)) {
                    currentProcessQuantumRemaining--;
                    if (currentProcessQuantumRemaining == 0) {
                        // Quantum exhausted, demote process (demotion logic handles re-adding to a queue)
                        demoteMLFQProcess(currentCPUProcess);
                        currentCPUProcess = null; // CPU is free for next tick
                    }
                }
            }
        } else { 
            ganttChartSequence.add(new GanttBlock("Idle", -1)); // Add an "Idle" block for this tick
        }

        // --- Update waiting times and advance simulation time ---
        updateWaitingTimes(); // Processes waiting in queues accumulate wait time

        currentTime++; // Advance simulation time by one unit

        // --- Update GUI elements ---
        updateLiveStatusTable();
        updateMetricsDisplay();
        ganttChartPanel.revalidate(); // Re-calculate preferred size as Gantt chart grows
        ganttChartPanel.repaint(); // Redraw Gantt chart

        // Auto-scroll Gantt chart to the right as it grows
        JScrollBar hScrollBar = ganttScrollPane.getHorizontalScrollBar();
        if (hScrollBar != null) {
            hScrollBar.setValue(hScrollBar.getMaximum());
        }

        // --- Check if all processes are completed to stop simulation ---
        boolean allCompleted = currentSimulationProcesses.stream().allMatch(p -> p.hasCompleted);
        if (allCompleted) {
            simulationTimer.stop();
            simulateButton.setText("Simulate");
            actionMessageLabel.setText("Simulation Finished. Total Time: " + currentTime);
        }
    }

    private void updateNextInQueueDisplay(String selectedAlgo) {
        nextInQueuePID = "None";
        switch (selectedAlgo) {
            case "First Come First Serve", "Round Robin", "SJF", "SRTF" -> {
                if (Queue != null && !Queue.isEmpty()) {
                    nextInQueuePID = Queue.peek().pid;
                }
            }
            case "MLFQ" -> {
                for (Queue<Process> q : mlfqQueues) {
                    if (!q.isEmpty()) {
                        nextInQueuePID = q.peek().pid + " (Q" + mlfqQueues.indexOf(q) + ")";
                        break; // Found the highest priority non-empty queue
                    }
                }
            }
        }
    }

    private void updateWaitingTimes() {
        for (Process p : currentSimulationProcesses) {
            if (!p.hasCompleted && p.hasArrived && p != currentCPUProcess) {
                p.waitingTime++;
            }
        }
    }

    private Process FCFS(Process previouslyRunningProcess) {
        if (previouslyRunningProcess != null && !previouslyRunningProcess.hasCompleted) {
            // FCFS is non-preemptive, so if a process is running, let it finish
            return previouslyRunningProcess;
        } else {
            // CPU is idle or previous process finished, pick next from queue
            if (Queue != null && !Queue.isEmpty()) {
                return Queue.poll();
            }
        }
        return null; // No process to run
    }

    private Process SJF(Process previouslyRunningProcess) {
        if (previouslyRunningProcess != null && !previouslyRunningProcess.hasCompleted) {
            // SJF is non-preemptive, so if a process is running, let it finish
            return previouslyRunningProcess;
        } else {
            // CPU is idle or previous process finished, pick shortest job from queue
            if (Queue != null && !Queue.isEmpty()) {
                // PriorityQueue naturally handles shortest remaining time first if comparator is set
                return Queue.poll();
            }
        }
        return null; // No process to run
    }

    private Process SRTF(Process previouslyRunningProcess) {
        // SRTF is preemptive. Always check if a new arriving process or
        // a process in the queue has a shorter remaining time than the current one.

        // Get all available processes (arrived and not completed)
        List<Process> availableProcesses = currentSimulationProcesses.stream()
                .filter(p -> p.hasArrived && !p.hasCompleted)
                .collect(Collectors.toList());

        // Add current CPU process back to candidates for re-evaluation if it was running
        if (previouslyRunningProcess != null && !previouslyRunningProcess.hasCompleted) {
            availableProcesses.add(previouslyRunningProcess);
        }

        Process bestCandidate = null;
        int shortestTime = Integer.MAX_VALUE;

        // Find the process with the shortest remaining burst time among available ones
        for (Process p : availableProcesses) {
            if (p.getRemainingBurstTime() < shortestTime) {
                shortestTime = p.getRemainingBurstTime();
                bestCandidate = p;
            }
        }

        // If the best candidate is different from the currently running process,
        // or if there was no previously running process, take the best candidate.
        if (bestCandidate != null && bestCandidate != previouslyRunningProcess) {
            // If there was a previously running process, and it's not the best candidate,
            // put it back into the general queue for SRTF re-evaluation later if needed.
            if (previouslyRunningProcess != null && previouslyRunningProcess.getRemainingBurstTime() > 0 && !previouslyRunningProcess.hasCompleted) {
                // Only add it back if it's not already in the queue AND if it's not the new best candidate
                if (!Queue.contains(previouslyRunningProcess)) {
                    Queue.offer(previouslyRunningProcess);
                }
            }
            // Remove the chosen bestCandidate from the queue to prevent re-adding it
            Queue.remove(bestCandidate);
            return bestCandidate;
        } else if (previouslyRunningProcess != null && !previouslyRunningProcess.hasCompleted) {
            // If the previously running process is still the best, continue running it
            return previouslyRunningProcess;
        }

        return null; // No process to run
    }


    private Process RoundRobin(Process previouslyRunningProcess) {
        if (currentCPUProcess == null || currentCPUProcess.hasCompleted || currentProcessQuantumRemaining == 0) {
            // If CPU is idle, previous process completed, or quantum expired
            if (previouslyRunningProcess != null && previouslyRunningProcess.getRemainingBurstTime() > 0 && !previouslyRunningProcess.hasCompleted) {
                // If quantum expired, previous process was already put back in queue by simulationTick
                // If previous process completed, it's null now or will be set null
            }

            if (Queue != null && !Queue.isEmpty()) {
                Process nextProcess = Queue.poll();
                try {
                    currentProcessQuantumRemaining = Integer.parseInt(timeQuantumField.getText().trim());
                    if (currentProcessQuantumRemaining <= 0) {
                        JOptionPane.showMessageDialog(this, "Round Robin Time Quantum must be greater than 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        resetSimulation();
                        return null;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number for Time Quantum.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    resetSimulation();
                    return null;
                }
                return nextProcess;
            }
        }
        return previouslyRunningProcess; // Continue current process
    }

    private Process MLFQ(Process previouslyRunningProcess) {
        // Check if previously running process needs to be re-evaluated
        if (previouslyRunningProcess != null && !previouslyRunningProcess.hasCompleted) {
            if (currentProcessQuantumRemaining == 0) {
                // Demotion handled in simulationTick. previouslyRunningProcess is now null after demotion.
            } else {
                // Still has quantum remaining, continue running
                return previouslyRunningProcess;
            }
        }

        // Find the highest priority non-empty queue
        for (int i = 0; i < mlfqQueues.size(); i++) {
            Queue<Process> currentQueue = mlfqQueues.get(i);
            if (!currentQueue.isEmpty()) {
                Process nextProcess = currentQueue.poll();
                nextProcess.currentQueueLevel = i; // Update the process's current queue level

                // Set quantum for the current process based on its queue level
                currentProcessQuantumRemaining = mlfqQuantums[i];
                return nextProcess;
            }
        }
        return null; // No process to run
    }

    private void demoteMLFQProcess(Process p) {
        if (p == null || p.hasCompleted) return;

        // Increment current_allotment_time for the process based on the quantum of the queue it just left
        p.currentAllotmentTime += mlfqQuantums[p.currentQueueLevel];

        // Check if allotment is exceeded for its *current* queue level
        boolean allotmentExceeded = (p.currentAllotmentTime >= mlfqAllotments[p.currentQueueLevel]);

        if (p.currentQueueLevel < mlfqQueues.size() - 1 && allotmentExceeded) {
            // Demote to the next queue if allotment exceeded and not the lowest queue
            p.currentQueueLevel++;
            mlfqQueues.get(p.currentQueueLevel).offer(p);
            p.currentAllotmentTime = 0; // Reset allotment time for the new queue level
            actionMessageLabel.setText("Process " + p.pid + " demoted to Q" + p.currentQueueLevel);
        } else {
            // If already in the lowest queue (Q3) or allotment not exceeded,
            // re-add to the end of its current queue (Round Robin within the queue)
            mlfqQueues.get(p.currentQueueLevel).offer(p);
            // No reset of allotment time if not demoted
            actionMessageLabel.setText("Process " + p.pid + " returned to Q" + p.currentQueueLevel);
        }
    }


    private void resetSimulation() {
        simulationTimer.stop();
        simulateButton.setText("Simulate");
        currentTime = 0;
        currentRunningProcessPID = "None";
        nextInQueuePID = "None";
        currentCPUProcess = null; // Reset currentCPUProcess on reset
        currentProcessQuantumRemaining = 0;
        ganttChartSequence.clear();
        processColors.clear(); // Clear colors on reset too

        // Reset all processes to their initial state
        definedProcesses.forEach(Process::reset);
        currentSimulationProcesses = null; // Clear simulation processes

        if (Queue != null) Queue.clear();
        if (mlfqQueues != null) mlfqQueues.forEach(java.util.Queue::clear);

        updateLiveStatusTable();
        updateMetricsDisplay();
        ganttChartPanel.revalidate();
        ganttChartPanel.repaint();
        actionMessageLabel.setText("Simulation reset. Define processes or click Simulate to start.");
    }

    // Helper to get consistent colors for processes
    private Color getColorForProcess(String pid) {
        return processColors.computeIfAbsent(pid, k -> new Color(
                (int) (Math.random() * 255),
                (int) (Math.random() * 255),
                (int) (Math.random() * 255)
        ).brighter()); // Ensure it's a bit brighter
    }

    private void drawGanttChart(Graphics g) {
        int yOffset = GANTT_ROW_HEIGHT + 5; // Start drawing blocks below the time axis

        // Draw time axis
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics(); // Get FontMetrics once for efficiency

        for (int i = 0; i <= currentTime; i++) {
            int x = i * GANTT_BLOCK_WIDTH;
            g.drawLine(x, GANTT_ROW_HEIGHT + 2, x, GANTT_ROW_HEIGHT + 7); // Tick mark

            String timeStr = String.valueOf(i);
            int stringWidth = fm.stringWidth(timeStr);

            // Calculate x position to center the string on the tick mark
            int textX = x - (stringWidth / 2);

            g.drawString(timeStr, textX, GANTT_ROW_HEIGHT - 5); // Time label
        }

        // Draw the Gantt chart blocks
        for (int i = 0; i < ganttChartSequence.size(); i++) {
            GanttBlock block = ganttChartSequence.get(i);
            String displayId = block.processId;
            if (block.queueLevel != -1) { // If it's an MLFQ process, show queue level
                displayId += " (Q" + block.queueLevel + ")";
            }

            int x = i * GANTT_BLOCK_WIDTH;
            Color processColor = getColorForProcess(block.processId); // Get color based on original PID

            g.setColor(processColor);
            g.fillRect(x, yOffset, GANTT_BLOCK_WIDTH, GANTT_ROW_HEIGHT);
            g.setColor(Color.BLACK);
            g.drawRect(x, yOffset, GANTT_BLOCK_WIDTH, GANTT_ROW_HEIGHT); // Border

            // Draw PID (and queue level if MLFQ) in the center of the block
            if (!"Idle".equals(block.processId)) {
                int stringWidth = fm.stringWidth(displayId);
                int stringHeight = fm.getHeight();
                g.setColor(Color.BLACK); // Text color
                g.drawString(displayId, x + (GANTT_BLOCK_WIDTH - stringWidth) / 2, yOffset + (GANTT_ROW_HEIGHT - stringHeight) / 2 + fm.getAscent());
            } else {
                // Draw "Idle" text instead of a line
                String idleText = "Idle";
                int stringWidth = fm.stringWidth(idleText);
                int stringHeight = fm.getHeight();
                g.setColor(Color.GRAY); // Choose a suitable color for Idle text
                g.drawString(idleText, x + (GANTT_BLOCK_WIDTH - stringWidth) / 2, yOffset + (GANTT_ROW_HEIGHT - stringHeight) / 2 + fm.getAscent());
            }
        }
    }


    private void exportResultsToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Simulation Results");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        fileChooser.setSelectedFile(new File("simulation_results.txt"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getAbsolutePath().endsWith(".txt")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write("CPU Scheduling Simulation Results\n");
                writer.write("Developed by: TENCHAVEZ RIEZNICK MCCAIN G.\n");
                writer.write("--------------------------------------------------\n");
                writer.write("Algorithm Used: " + algorithmComboBox.getSelectedItem() + "\n");
                if ("Round Robin".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("Time Quantum (RR): " + timeQuantumField.getText() + "\n");
                }
                if ("MLFQ".equals(algorithmComboBox.getSelectedItem())) {
                    writer.write("MLFQ Quantums: Q0=" + mlfqQuantums[0] + ", Q1=" + mlfqQuantums[1] + ", Q2=" + mlfqQuantums[2] + ", Q3=" + mlfqQuantums[3] + "\n");
                    writer.write("MLFQ Allotments: Q0=" + mlfqAllotments[0] + ", Q1=" + mlfqAllotments[1] + ", Q2=" + mlfqAllotments[2] + ", Q3=" + mlfqAllotments[3] + "\n");
                }
                writer.write("Total Simulation Time: " + currentTime + "\n");
                writer.write("--------------------------------------------------\n\n");

                writer.write("Process Definitions:\n");
                writer.write(String.format("%-10s %-15s %-15s %-10s\n", "Process", "Arrival Time", "Exec. Time", "Priority"));
                for (Process p : definedProcesses) {
                    writer.write(String.format("%-10s %-15d %-15d %-10d\n", p.pid, p.arrivalTime, p.burstTime, p.priority));
                }
                writer.write("\n");

                writer.write("Live Simulation Status (Final):\n");
                writer.write(String.format("%-10s %-12s %-15s %-12s %-12s %-12s %-12s %-12s\n",
                        "Process", "Status", "Completion %", "Rem. Time", "Wait Time", "Comp. Time", "TAT", "Resp. Time"));

                // Ensure data in liveStatusModel reflects the final state
                updateLiveStatusTable(); // Just to be sure the model is up-to-date
                for (int i = 0; i < liveStatusModel.getRowCount(); i++) {
                    String pid = (String) liveStatusModel.getValueAt(i, 0);
                    String status = (String) liveStatusModel.getValueAt(i, 1);
                    Object completion = liveStatusModel.getValueAt(i, 2);
                    Object remTime = liveStatusModel.getValueAt(i, 3);
                    Object waitTime = liveStatusModel.getValueAt(i, 4);
                    Object compTime = liveStatusModel.getValueAt(i, 5);
                    Object tat = liveStatusModel.getValueAt(i, 6);
                    Object respTime = liveStatusModel.getValueAt(i, 7);

                    writer.write(String.format("%-10s %-12s %-15s %-12s %-12s %-12s %-12s %-12s\n",
                            pid, status,
                            (completion instanceof Double) ? String.format("%.2f%%", (Double) completion) : completion.toString(),
                            remTime.toString(), waitTime.toString(), compTime.toString(), tat.toString(), respTime.toString()));
                }
                writer.write("\n");

                writer.write("Overall Metrics:\n");
                writer.write(avgWaitingTimeLabel.getText() + "\n");
                writer.write(avgExecutionTimeLabel.getText() + "\n");
                writer.write(avgTurnaroundTimeLabel.getText() + "\n");
                writer.write(avgResponseTimeLabel.getText() + "\n");
                writer.write(totalExecutionTimeLabel.getText() + "\n");
                writer.write("Overall Progress: " + overallProgressBar.getString() + "\n");
                writer.write("\n");

                writer.write("Gantt Chart Sequence (PID and Queue Level at each time unit):\n");
                for (int i = 0; i < ganttChartSequence.size(); i++) {
                    GanttBlock block = ganttChartSequence.get(i);
                    String display = block.processId;
                    if(block.queueLevel != -1) {
                        display += " (Q" + block.queueLevel + ")";
                    }
                    writer.write(String.format("Time %d: %s\n", i, display));
                }

                JOptionPane.showMessageDialog(this, "Simulation results exported successfully to:\n" + fileToSave.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting results: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // --- Inner class for Process definition ---
    final class Process {
        String pid;
        int arrivalTime;
        int burstTime;
        int priority; // For Priority Scheduling / MLFQ

        // Simulation-specific fields
        int remainingBurstTime;
        int waitingTime;
        int turnaroundTime;
        int completionTime;
        int responseTime; // Time from arrival to first execution
        boolean hasArrived; // To track if process has arrived in simulation
        boolean hasCompleted; // To track if process has completed
        int currentQueueLevel; // For MLFQ: 0, 1, 2, 3
        int currentAllotmentTime; // For MLFQ: time accumulated in current queue level

        public Process(String pid, int arrivalTime, int burstTime, int priority) {
            this.pid = pid;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            reset(); // Initialize simulation-specific fields
        }

        // Copy constructor for simulation runs
        public Process(Process other) {
            this.pid = other.pid;
            this.arrivalTime = other.arrivalTime;
            this.burstTime = other.burstTime;
            this.priority = other.priority;
            reset(); // Ensure reset for the copy
        }

        public void reset() {
            this.remainingBurstTime = this.burstTime;
            this.waitingTime = 0;
            this.turnaroundTime = 0;
            this.completionTime = 0;
            this.responseTime = -1; // -1 indicates not yet responded
            this.hasArrived = false;
            this.hasCompleted = false;
            this.currentQueueLevel = 0; // All processes start in Q0 for MLFQ
            this.currentAllotmentTime = 0; // Reset allotment time
        }

        public int getRemainingBurstTime() {
            return remainingBurstTime;
        }

        public void setRemainingBurstTime(int remainingBurstTime) {
            this.remainingBurstTime = remainingBurstTime;
        }

        @Override
        public String toString() {
            return pid;
        }
    }

    // --- New Inner class for Gantt Chart Block ---
    class GanttBlock {
        String processId;
        int queueLevel; // -1 if not MLFQ, otherwise 0-3

        public GanttBlock(String processId, int queueLevel) {
            this.processId = processId;
            this.queueLevel = queueLevel;
        }
    }

    // --- Inner class for JTable Progress Bar Renderer ---
    class ProgressBarRenderer extends DefaultTableCellRenderer {
        private final JProgressBar progressBar = new JProgressBar(0, 100);

        public ProgressBarRenderer() {
            progressBar.setStringPainted(true);
            progressBar.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Double progressValue = 0.0;
            switch (value) {
                case Double aDouble -> progressValue = aDouble;
                case Integer integer -> progressValue = integer.doubleValue();
                default -> {
                }
            }

            progressBar.setValue(progressValue.intValue());
            progressBar.setString(String.format("%.2f%%", progressValue));

            // Set color based on progress (e.g., green for completed, blue for active)
            if (progressValue >= 100.0) {
                progressBar.setForeground(new Color(0, 150, 0)); // Dark green
            } else if (progressValue > 0) {
                progressBar.setForeground(new Color(50, 100, 200)); // Medium blue
            } else {
                progressBar.setForeground(Color.LIGHT_GRAY); // Grey for 0%
            }

            return progressBar;
        }
    }

    // --- Main method to run the GUI ---
    public static void main(String[] args) {
        // Ensure GUI updates are done on the Event Dispatch Thread
        SwingUtilities.invokeLater(SchedulerGUI::new);
    }
}
