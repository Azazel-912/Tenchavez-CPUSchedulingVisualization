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
    public int completionTime;
    public int turnaroundTime;
    public int waitingTime;
    public int responseTime;

    public Process(String pid, int at, int bt) {
        this.pid = pid;
        this.arrivalTime = at;
        this.burstTime = bt;
    }
}
