/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
import java.util.*;

public class CPUScheduler {

    public static void fifo(List<Process> processes) {
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int currentTime = 0;
        StringBuilder gantt = new StringBuilder("\nGantt Chart:\n|");

        for (Process p : processes) {
            if (currentTime < p.arrivalTime) currentTime = p.arrivalTime;

            p.responseTime = currentTime - p.arrivalTime;
            currentTime += p.burstTime;
            p.completionTime = currentTime;
            p.turnaroundTime = p.completionTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            for (int i = 0; i < p.burstTime; i++) {
                gantt.append(" ").append(p.pid).append(" |");
            }
        }

        System.out.println(gantt);
        System.out.println("\nPID\tAT\tBT\tCT\tTAT\tWT\tRT");
        double tat = 0, wt = 0, rt = 0;
        for (Process p : processes) {
            System.out.printf("%s\t%d\t%d\t%d\t%d\t%d\t%d\n",
                    p.pid, p.arrivalTime, p.burstTime, p.completionTime,
                    p.turnaroundTime, p.waitingTime, p.responseTime);
            tat += p.turnaroundTime;
            wt += p.waitingTime;
            rt += p.responseTime;
        }
        int n = processes.size();
        System.out.printf("\nAvg TAT: %.2f\nAvg WT: %.2f\nAvg RT: %.2f\n", tat/n, wt/n, rt/n);
    }
}
