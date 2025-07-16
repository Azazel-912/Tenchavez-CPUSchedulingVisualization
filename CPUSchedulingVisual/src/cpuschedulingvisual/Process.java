/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
public class Process {
    public String pid;
    public int arrivalTime;
    public int burstTime;
    public int remainingTime;
    public int completionTime;
    public int startTime;
    public int queueLevel;  // For MLFQ

    public Process(String pid, int arrivalTime, int burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.startTime = -1;
        this.queueLevel = 0;
    }
}
