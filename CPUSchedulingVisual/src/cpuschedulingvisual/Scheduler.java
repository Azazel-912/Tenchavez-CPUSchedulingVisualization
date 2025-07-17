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
import java.io.IOException;
import java.util.*;
import javax.swing.JOptionPane;

public class Scheduler {

    public static List<Process> defaultProcesses() {
        return Arrays.asList(
            new Process("P1", 0, 8),
            new Process("P2", 1, 4),
            new Process("P3", 2, 9),
            new Process("P4", 3, 5)
        );
    }

    public static String fifo(List<Process> plist) {
        plist.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int time = 0;
        StringBuilder out = new StringBuilder("FIFO:\n");

        for (Process p : plist) {
            if (time < p.arrivalTime) time = p.arrivalTime;
            p.waitingTime = time - p.arrivalTime;
            time += p.burstTime;
            p.turnaroundTime = p.waitingTime + p.burstTime;
            out.append(p.pid).append(": WT=").append(p.waitingTime).append(", TAT=").append(p.turnaroundTime).append("\n");
        }
        return out.toString();
    }

    public static String sjf(List<Process> plist) {
        List<Process> processes = new ArrayList<>(plist);
        processes.sort(Comparator.comparingInt(p -> p.burstTime));
        return fifo(processes);
    }

    public static String srtf(List<Process> plist) {
        List<Process> processes = new ArrayList<>(plist);
        int time = 0, complete = 0;
        int n = processes.size();
        int[] remaining = new int[n];
        for (int i = 0; i < n; i++) remaining[i] = processes.get(i).burstTime;
        int[] wt = new int[n], tat = new int[n];
        boolean[] done = new boolean[n];
        StringBuilder out = new StringBuilder("SRTF:\n");

        while (complete < n) {
            int idx = -1, min = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!done[i] && processes.get(i).arrivalTime <= time && remaining[i] < min && remaining[i] > 0) {
                    min = remaining[i];
                    idx = i;
                }
            }
            if (idx == -1) {
                time++;
                continue;
            }
            remaining[idx]--;
            if (remaining[idx] == 0) {
                complete++;
                done[idx] = true;
                int finish = time + 1;
                wt[idx] = finish - processes.get(idx).burstTime - processes.get(idx).arrivalTime;
                if (wt[idx] < 0) wt[idx] = 0;
                tat[idx] = wt[idx] + processes.get(idx).burstTime;
                out.append(processes.get(idx).pid).append(": WT=").append(wt[idx]).append(", TAT=").append(tat[idx]).append("\n");
            }
            time++;
        }
        return out.toString();
    }

    public static String roundRobin(List<Process> plist, int quantum) {
        Queue<Process> queue = new LinkedList<>();
        List<Process> processes = new ArrayList<>(plist);
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int time = 0;
        int[] rem = new int[processes.size()];
        for (int i = 0; i < processes.size(); i++) rem[i] = processes.get(i).burstTime;
        int[] wt = new int[processes.size()];
        boolean[] added = new boolean[processes.size()];
        StringBuilder out = new StringBuilder("RR:\n");

        int idx = 0;
        while (!queue.isEmpty() || idx < processes.size()) {
            while (idx < processes.size() && processes.get(idx).arrivalTime <= time) {
                queue.offer(processes.get(idx));
                added[idx] = true;
                idx++;
            }
            if (queue.isEmpty()) {
                time++;
                continue;
            }
            Process p = queue.poll();
            int i = processes.indexOf(p);
            int exec = Math.min(quantum, rem[i]);
            rem[i] -= exec;
            time += exec;
            for (int j = idx; j < processes.size(); j++) {
                if (!added[j] && processes.get(j).arrivalTime <= time) {
                    queue.offer(processes.get(j));
                    added[j] = true;
                }
            }
            if (rem[i] > 0) {
                queue.offer(p);
            } else {
                wt[i] = time - p.arrivalTime - p.burstTime;
                out.append(p.pid).append(": WT=").append(wt[i]).append(", TAT=").append(wt[i]).append(p.burstTime).append("\n");
            }
        }
        return out.toString();
    }

    public static String mlfq(List<Process> plist, int quantum) {
        List<Queue<Process>> queues = new ArrayList<>();
        for (int i = 0; i < 3; i++) queues.add(new LinkedList<>());
        List<Process> processes = new ArrayList<>(plist);
        int time = 0;
        int[] rem = new int[processes.size()];
        for (int i = 0; i < processes.size(); i++) rem[i] = processes.get(i).burstTime;
        int[] wt = new int[processes.size()];
        int[] level = new int[processes.size()];
        boolean[] added = new boolean[processes.size()];
        StringBuilder out = new StringBuilder("MLFQ:\n");

        int idx = 0, completed = 0;
        while (completed < processes.size()) {
            for (int i = idx; i < processes.size(); i++) {
                if (!added[i] && processes.get(i).arrivalTime <= time) {
                    queues.get(0).offer(processes.get(i));
                    added[i] = true;
                    idx++;
                }
            }
            int lvl = -1;
            for (int i = 0; i < 3; i++) {
                if (!queues.get(i).isEmpty()) {
                    lvl = i;
                    break;
                }
            }
            if (lvl == -1) {
                time++;
                continue;
            }
            Process p = queues.get(lvl).poll();
            int i = processes.indexOf(p);
            int exec = Math.min(quantum * (lvl + 1), rem[i]);
            rem[i] -= exec;
            time += exec;
            for (int j = idx; j < processes.size(); j++) {
                if (!added[j] && processes.get(j).arrivalTime <= time) {
                    queues.get(0).offer(processes.get(j));
                    added[j] = true;
                    idx++;
                }
            }
            if (rem[i] > 0) {
                level[i] = Math.min(2, level[i] + 1);
                queues.get(level[i]).offer(p);
            } else {
                wt[i] = time - p.arrivalTime - p.burstTime;
                out.append(p.pid).append(": WT=").append(wt[i]).append(", TAT=").append(wt[i]).append(p.burstTime).append("\n");
                completed++;
            }
        }
        return out.toString();
    }

    public static void exportToFile(String data) {
        try (FileWriter fw = new FileWriter("output.txt")) {
            fw.write(data);
            JOptionPane.showMessageDialog(null, "Output exported to output.txt");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to export: " + e.getMessage());
        }
    }
}
