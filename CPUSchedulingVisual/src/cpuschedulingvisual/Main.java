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

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Process> processes = new ArrayList<>();

        System.out.print("Enter number of processes: ");
        int n = sc.nextInt();

        for (int i = 0; i < n; i++) {
            System.out.printf("Process P%d\n", i+1);
            System.out.print("Arrival Time: ");
            int at = sc.nextInt();
            System.out.print("Burst Time: ");
            int bt = sc.nextInt();
            processes.add(new Process("P" + (i+1), at, bt));
        }

        System.out.println("\nSelect Algorithm:");
        System.out.println("1. FIFO");
        int choice = sc.nextInt();

        if (choice == 1) {
            CPUScheduler.fifo(processes)
        } else {
            System.out.println("Invalid choice.");
        }
    }
}

