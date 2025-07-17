/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
// File: MyProcess.java
    /**
     *
     */

public class Process {
    public String pid;
    public String extension; // New: To store .script, .ino etc.
    public int arrivalTime;
    public int burstTime;
    public int priority; // New: For priority scheduling
    public int remainingTime;
    public int completionTime;
    public int startTime; // Time when process first starts execution
    public int waitingTime; // To track waiting time for live updates
    public int queueLevel;  // For MLFQ

    // Constructor updated to include extension and priority
    public Process(String pid, String extension, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.extension = extension != null ? extension : ""; // Ensure not null
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
        this.remainingTime = burstTime;
        this.startTime = -1; // -1 indicates not yet started
        this.completionTime = -1; // -1 indicates not yet completed
        this.waitingTime = 0; // Initialize waiting time
        this.queueLevel = 0;
    }

    // Copy constructor for simulation (important to avoid modifying original process objects)
    public Process(Process other) {
        this.pid = other.pid;
        this.extension = other.extension;
        this.arrivalTime = other.arrivalTime;
        this.burstTime = other.burstTime;
        this.priority = other.priority;
        this.remainingTime = other.burstTime; // Always reset remaining time for a new simulation
        this.startTime = -1;
        this.completionTime = -1;
        this.waitingTime = 0;
        this.queueLevel = 0;
    }

    public String getFullPid() {
        return pid + (extension != null && !extension.isEmpty() ? extension : "");
    }
}