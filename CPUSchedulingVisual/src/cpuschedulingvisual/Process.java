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
    public int arrivalTime;
    public int burstTime;
    public int remainingTime;
    public int waitingTime;
    public int turnaroundTime;
    public int priority;
    public int queueLevel; // for MLFQ

    public Process(String pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.waitingTime = 0;
        this.turnaroundTime = 0;
        this.priority = 0;
        this.queueLevel = 0;
    }

    public Process copy() {
        Process p = new Process(this.pid, this.arrivalTime, this.burstTime);
        p.remainingTime = this.remainingTime;
        p.waitingTime = this.waitingTime;
        p.turnaroundTime = this.turnaroundTime;
        p.priority = this.priority;
        p.queueLevel = this.queueLevel;
        return p;
    }
}
