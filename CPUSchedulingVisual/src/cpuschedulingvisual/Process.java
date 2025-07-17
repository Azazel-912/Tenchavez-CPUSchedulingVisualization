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

    static void add(Process process) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    public String pid;
    public int arrivalTime;
    public int burstTime;
    public int remainingTime;
    public int waitingTime;
    public int turnaroundTime;
    public int priority; // optional: for MLFQ or priority-based scheduling

    public Process(String pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.priority = 0;
    }

    // Optional constructor for priority
    public Process(String pid, int arrivalTime, int burstTime, int priority) {
        this(pid, arrivalTime, burstTime);
        this.priority = priority;
    }

    // Add any helper methods if needed
    public Process copy() {
        return new Process(pid, arrivalTime, burstTime, priority);
    }
} 
