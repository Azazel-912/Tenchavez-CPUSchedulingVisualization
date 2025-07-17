/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cpuschedulingvisual;

/**
 *
 * @author Admin
 */
// Scheduler.java
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;

public class Scheduler {

    public static void fifo(List<Process> list, StringBuilder out) {
        list.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int time = 0;
        for (Process p : list) {
            if (time < p.arrivalTime) time = p.arrivalTime;
            p.startTime = time;
            time += p.burstTime;
            p.completionTime = time;
        }
        print(list, out);
    }

    // ========== SJF ===========
    public static void sjf(List<Process> list, StringBuilder out) {
        List<Process> temp = new ArrayList<>(list);
        List<Process> done = new ArrayList<>();
        int time = 0;

        while (!temp.isEmpty()) {
            Process shortest = null;
            for (Process p : temp) {
                if (p.arrivalTime <= time) {
                    if (shortest == null || p.burstTime < shortest.burstTime) {
                        shortest = p;
                    }
                }
            }

            if (shortest == null) {
                time++;
                continue;
            }

            shortest.startTime = time;
            time += shortest.burstTime;
            shortest.completionTime = time;
            done.add(shortest);
            temp.remove(shortest);
        }

        print(done, out);
    }

    // ========== SRTF ===========
    public static void srtf(List<Process> list, StringBuilder out) {
        List<Process> temp = new ArrayList<>();
        for (Process p : list) temp.add(new Process(p.pid, p.extension, p.arrivalTime, p.burstTime, p.priority)); // Use new constructor
        // Make sure remainingTime is re-initialized for temp processes if they are copies
        for(Process p : temp) p.remainingTime = p.burstTime;


        int time = 0, done = 0;
        while (done < temp.size()) {
            Process next = null;
            for (Process p : temp) {
                if (p.arrivalTime <= time && p.remainingTime > 0) {
                    if (next == null || p.remainingTime < next.remainingTime) {
                        next = p;
                    }
                }
            }

            if (next == null) {
                time++;
                continue;
            }

            if (next.startTime == -1) next.startTime = time;
            next.remainingTime--;
            time++;

            if (next.remainingTime == 0) {
                next.completionTime = time;
                done++;
            }
        }

        print(temp, out);
    }

    // ========== Round Robin ===========
    public static void rr(List<Process> list, int quantum, StringBuilder out) {
        Queue<Process> queue = new LinkedList<>();
        List<Process> temp = new ArrayList<>();
        for (Process p : list) temp.add(new Process(p.pid, p.extension, p.arrivalTime, p.burstTime, p.priority)); // Use new constructor
        for(Process p : temp) p.remainingTime = p.burstTime; // Re-initialize remaining time

        int time = 0, index = 0;
        // Sort original list by arrival time for initial queuing
        list.sort(Comparator.comparingInt(p -> p.arrivalTime)); // Sort the original list for processing
        for (Process p : list) temp.set(temp.indexOf(p), new Process(p)); // Deep copy based on sorted original list if needed
        
        while (true) {
            // Add arrived processes to queue
            while (index < temp.size() && temp.get(index).arrivalTime <= time) {
                queue.add(temp.get(index));
                index++;
            }

            if (queue.isEmpty()) {
                if (index < temp.size()) {
                    time = temp.get(index).arrivalTime; // Jump time to next arrival
                    continue;
                } else {
                    break; // All processes arrived and queue is empty, so done
                }
            }

            Process p = queue.poll();
            if (p.startTime == -1) p.startTime = time; // Set start time if it's the first run

            int runTime = Math.min(quantum, p.remainingTime);
            p.remainingTime -= runTime;
            time += runTime;

            // Check for new arrivals again after running a process
            while (index < temp.size() && temp.get(index).arrivalTime <= time) {
                queue.add(temp.get(index));
                index++;
            }

            if (p.remainingTime > 0) {
                queue.add(p); // Add back to queue if not finished
            } else {
                p.completionTime = time; // Mark completion time
            }
        }

        print(temp, out);
    }

    // ========== MLFQ ===========
    public static void mlfq(List<Process> list, int[] timeQuantums, int[] allotments, StringBuilder out) {
        final int levels = 4;
        List<Queue<Process>> queues = new ArrayList<>();
        for (int i = 0; i < levels; i++) queues.add(new LinkedList<>());

        List<Process> all = new ArrayList<>();
        for (Process p : list) all.add(new Process(p.pid, p.extension, p.arrivalTime, p.burstTime, p.priority)); // Use new constructor
        for(Process p : all) p.remainingTime = p.burstTime; // Re-initialize remaining time


        // Sort processes by arrival time initially
        all.sort(Comparator.comparingInt(p -> p.arrivalTime));

        int time = 0, index = 0;
        while (true) {
            // Add newly arrived processes to the highest priority queue (level 0)
            while (index < all.size() && all.get(index).arrivalTime <= time) {
                all.get(index).queueLevel = 0;
                queues.get(0).add(all.get(index));
                index++;
            }

            Process current = null;
            int level = -1;
            // Find the highest priority non-empty queue
            for (int i = 0; i < levels; i++) {
                if (!queues.get(i).isEmpty()) {
                    current = queues.get(i).poll();
                    level = i;
                    break;
                }
            }

            if (current == null) {
                // If no process in any queue and no more processes to arrive
                if (index >= all.size()) break;
                time++; // Advance time if CPU is idle
                continue;
            }

            if (current.startTime == -1) current.startTime = time; // Set start time if first run

            int runtime = Math.min(timeQuantums[level], current.remainingTime);
            current.remainingTime -= runtime;
            time += runtime;

            // Add newly arrived processes during current process's execution
            while (index < all.size() && all.get(index).arrivalTime <= time) {
                all.get(index).queueLevel = 0;
                queues.get(0).add(all.get(index));
                index++;
            }

            if (current.remainingTime == 0) {
                current.completionTime = time; // Process completed
            } else {
                // Demote process to next level if not completed within its quantum
                int nextLevel = Math.min(level + 1, levels - 1);
                current.queueLevel = nextLevel;
                queues.get(nextLevel).add(current);
            }
        }

        print(all, out);
    }

    // ========== Print and Export ===========
    // This method is for the *final* output string, not for live GUI updates.
    // Live GUI updates will calculate metrics directly from currentSimulationProcesses.
    public static void print(List<Process> list, StringBuilder out) {
        int totalTAT = 0;
        int totalRT = 0; // Total Response Time
        out.append("PID\tAT\tBT\tCT\tTAT\tRT\n");
        for (Process p : list) {
            int tat = p.completionTime - p.arrivalTime;
            int rt = p.startTime - p.arrivalTime; // Response Time
            totalTAT += tat;
            totalRT += rt;
            out.append(p.getFullPid()).append("\t").append(p.arrivalTime).append("\t").append(p.burstTime).append("\t").append(p.completionTime).append("\t").append(tat).append("\t").append(rt).append("\n");
        }
        out.append("\nAverage Turnaround Time: ").append(String.format("%.2f", (double) totalTAT / list.size()));
        out.append("\nAverage Response Time: ").append(String.format("%.2f", (double) totalRT / list.size()));
    }

    public static void exportToFile(String content) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String filename = "cpu_results_" + timestamp + ".txt";
            try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
                out.print(content);
            }
        } catch (Exception e) {
            System.err.println("Error exporting to file: " + e.getMessage());
        }
    }
}