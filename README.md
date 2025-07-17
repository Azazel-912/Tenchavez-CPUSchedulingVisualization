# TENCHAVEZ CPU Scheduling Visualization

## Project Overview

This project is a Java Swing-based GUI application designed to simulate and visualize various CPU scheduling algorithms. The primary goal is to provide anatina interactive platform where users can define processes, observe their execution order on a single CPU, and analyze key performance metrics. This simulation aims to demonstrate the fundamental principles of how operating systems manage and schedule processes for CPU execution.

## Implemented CPU Scheduling Algorithms

The simulation supports the following CPU scheduling policies:

1.  **First-In First-Out (FIFO / FCFS):** Processes are executed in the order they arrive in the ready queue. Non-preemptive.
2.  **Shortest Job First (SJF) – Non-Preemptive:** The process with the shortest burst time (execution time) is selected next. Once a process starts, it runs to completion.
3.  **Shortest Remaining Time First (SRTF) – Preemptive:** A preemptive version of SJF. The process with the shortest remaining time is executed. If a new process arrives with a shorter remaining time than the currently running process, the current process is preempted.
4.  **Round Robin (RR):** Each process is given a small unit of CPU time (time quantum). If a process does not complete within its quantum, it is preempted and added to the end of the ready queue. The time quantum is user-configurable.
5.  **Multilevel Feedback Queue (MLFQ):** Processes are assigned to different queues with varying priorities. Lower-numbered queues (e.g., Q0) have higher priority. Processes can move between queues based on their behavior (e.g., demoted if they use too much CPU time, promoted if they wait too long). This implementation uses 4 priority levels (Q0, Q1, Q2, Q3) with user-configurable quantum and allotment times for each level.

## Assumptions

To simplify the simulation problem, the following assumptions are made:

* **Predefined Processes:** All processes are defined and known at the start of the simulation. Dynamic arrival during runtime is not handled.
* **CPU-Bound:** Processes are assumed to be CPU-bound only, with no I/O operations. Once scheduled, a process runs uninterrupted unless preempted by the algorithm.
* **No Context Switching Overhead:** The time taken to switch between processes is considered zero.
* **Single-Core Processor:** The simulation runs on a single CPU, meaning only one instruction can execute at any given time.
* **Valid Input:** User input for process details and algorithm parameters is assumed to be valid (e.g., positive burst times, valid numbers).

## Features and Functionality

* **Interactive GUI:** A user-friendly graphical interface built with Java Swing.
* **Process Definition:**
    * Manually input process details (PID, Arrival Time, Burst Time, Priority).
    * Generate a specified number of random processes.
    * Add, delete, and update processes in a definition table.
* **Algorithm Selection:** Choose from the five implemented scheduling algorithms via a dropdown menu.
* **Configurable Parameters:**
    * Set the time quantum for the Round Robin algorithm.
    * Configure quantum and allotment times for each of the four MLFQ levels (Q0-Q3).
* **Real-time Visualization:**
    * **Gantt Chart:** A dynamic, visual representation of process execution over time, showing the order of processes on the CPU. For MLFQ, it annotates the queue level of the running process (e.g., "P1(Q0)").
    * **Live Status Table:** Displays the real-time status of all processes, including their PID, current status (New, Ready, Running, Completed), completion percentage, remaining execution time, and accumulated waiting time.
* **Detailed Metrics Display:**
    * **Individual Process Metrics:** For each completed process, displays:
        * Process ID
        * Arrival Time (AT)
        * Burst Time (BT)
        * Completion Time (CT)
        * Turnaround Time (TAT = CT - AT)
        * Response Time (RT = First Start Time - AT)
        * Waiting Time (WT = TAT - BT)
    * **Average Metrics:** Calculates and displays the average Turnaround Time, Response Time, Waiting Time, and Burst Time across all completed processes.
    * **Total Simulation Time:** Shows the total elapsed time of the simulation.
    * **Overall Progress Bar:** Visualizes the progress of the entire simulation.
* **Simulation Controls:**
    * Start/Pause/Resume simulation.
    * Reset the simulation to its initial state.
    * Adjust the simulation speed.
* **Export Results:** An "Export Results" button allows users to save a comprehensive text file (`.txt`) containing:
    * A text-based (ASCII) Gantt chart.
    * A table of all individual process metrics.
    * All calculated average metrics.

## How to Run the Simulation

### Prerequisites

* **Java Development Kit (JDK):** Version 8 or higher is required.

### Steps

1.  **Clone the Repository:**
    ```bash
    git clone [https://github.com/your-username/your-repo-name.git](https://github.com/your-username/your-repo-name.git)
    cd your-repo-name
    ```
    *(Replace `your-username` and `your-repo-name` with your actual GitHub details)*

2.  **Compile the Source Code:**
    Open your terminal or command prompt, navigate to the directory where `SchedulerGUI.java` is located, and compile the Java file:
    ```bash
    javac SchedulerGUI.java
    ```

3.  **Run the Application:**
    After successful compilation, run the application:
    ```bash
    java SchedulerGUI
    ```
    The GUI window should appear, maximized by default.

## Source Code Organization

The entire application logic, including the GUI components, simulation algorithms, and data structures (like the `Process` class), is contained within a single file: `SchedulerGUI.java`.

## Description of Each Scheduling Algorithm

*(This section provides a brief description, as required. You can elaborate further if you wish.)*

### First-In First-Out (FIFO / FCFS)
Processes are executed in the order they arrive in the ready queue. It's simple to implement but can lead to long waiting times for short processes if a long process arrives first (convoy effect).

### Shortest Job First (SJF) – Non-Preemptive
The CPU is allocated to the process with the smallest burst time. Once the CPU is allocated to a process, it cannot be preempted until it completes its execution. SJF typically provides the minimum average waiting time for a given set of processes.

### Shortest Remaining Time First (SRTF) – Preemptive
This is the preemptive version of SJF. If a new process arrives with a burst time less than the remaining time of the currently executing process, the current process is preempted, and the new process starts execution. This algorithm can minimize average waiting time and turnaround time.

### Round Robin
A time-sharing scheduling algorithm designed for time-sharing systems. Each process gets a small unit of CPU time, called a time quantum (or time slice). After this time quantum, the process is preempted and added to the end of the ready queue. This ensures fairness and responsiveness.

### Multilevel Feedback Queue (MLFQ)
This algorithm uses multiple queues, each with its own scheduling algorithm. Processes move between queues based on their CPU burst characteristics. High-priority queues might use FCFS or a small Round Robin quantum, while lower-priority queues might use a larger quantum or SJF. Processes that consume too much CPU time in a high-priority queue are demoted to a lower-priority queue. Processes waiting for too long in a low-priority queue might be promoted. This aims to balance responsiveness for interactive jobs with efficiency for CPU-bound jobs.

## Screenshots / Terminal Output Examples

*(**IMPORTANT:** You will need to add your own screenshots here after running the application. Create an `assets` folder in your repository and link them.)*

* **Main GUI:** (Show the initial state of the application)
    `![Main GUI](assets/main_gui.png)` *(Replace with your image path)*
* **Simulation in Progress:** (Show the Gantt chart and live status table during a simulation run)
    `![Simulation In Progress](assets/simulation_running.png)` *(Replace with your image path)*
* **Completed Simulation with Metrics:** (Show the final state with all metrics calculated)
    `![Completed Simulation](assets/simulation_completed.png)` *(Replace with your image path)*
* **MLFQ Configuration:** (Show the MLFQ input fields)
    `![MLFQ Configuration](assets/mlfq_config.png)` *(Replace with your image path)*
* **Exported Results File Example:** (You can copy-paste a snippet of the content of a generated `simulation_results.txt` file here, or take a screenshot of the text file itself).
    ```
    # --- Example content from simulation_results.txt ---
    CPU Scheduling Simulation Results
    Developed By: Tenchavez Rieznick McCain G.
    ----------------------------------------------------------------
    Algorithm: Round Robin
    Time Quantum: 2
    Total Simulation Time: 25
    ----------------------------------------------------------------

    Gantt Chart:
    -----------------------------------
    | P1.sc | P2.sc | P1.sc | P3.sc | ...
    -----------------------------------
    0       1       2       3       ...

    Process Metrics:
    Process ID Arrival    Burst      Completion Turnaround Response   Waiting
    ---------- ---------- ---------- ---------- ---------- ---------- ----------
    P1.sc      0          10         20         20         0          10
    P2.sc      2          5          15         13         0          8
    ...

    Average Metrics:
    Average Waiting Time : 5.00
    Average Burst Time : 7.00
    Average Turnaround Time : 10.00
    Average Response Time : 0.50
    ```

## Known Bugs, Limitations, or Incomplete Features

* **Extreme Number of Processes:** While designed to handle a reasonable number, an extremely high number of processes (e.g., hundreds) might impact GUI performance or the readability of the Gantt chart due to display constraints.
* **Window Resizing:** Although the application attempts to maximize, extreme manual resizing behavior by the user might occasionally lead to minor layout quirks, particularly with very small window sizes.
* **Gantt Chart Scrolling:** The Gantt chart provides horizontal scrolling, but vertical scrolling is not implemented as processes are displayed in a single row.

## Member Roles and Contributions

This project was developed individually.

* **Tenchavez Rieznick McCain G.:**
    * Designed and implemented the core Java Swing GUI.
    * Developed and integrated all five CPU scheduling algorithms (FCFS, SJF, SRTF, Round Robin, MLFQ).
    * Implemented real-time Gantt chart visualization with MLFQ queue annotation.
    * Developed the live process status table and comprehensive metric calculations (Completion Time, Turnaround Time, Response Time, Waiting Time, and their averages).
    * Implemented configurable parameters for Round Robin and Multilevel Feedback Queue.
    * Added random process generation, and process add/delete/update functionalities.
    * Implemented the "Export Results" feature to generate a detailed text report.
    * Managed the project's GitHub repository and documentation.

---
