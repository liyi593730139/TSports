package common.monitor;


import common.remote.Client;

import java.util.ArrayList;
import java.util.List;

public class MonitorManager implements Runnable{

	public final static int CYCLE = 6 * 1000; //millisecond

    private static MonitorManager manager;

    public static synchronized MonitorManager getMonitorManager() {
        if (manager == null) {
            manager = new MonitorManager();
        }
        return manager;
    }

    private List<MonitorAffair> monitorAffairs;

    /**
    * 构造方法。
    */
    private MonitorManager() {
        monitorAffairs =  new ArrayList<MonitorAffair>();
    }

    public boolean addMonitor(MonitorAffair affair) {
        synchronized (monitorAffairs) {
            return monitorAffairs.add(affair);
        }
    }

    public void addMonitors(MonitorAffair[] affairs) {
        synchronized (monitorAffairs) {
            for (int i = 0; i < affairs.length; i++) {
                monitorAffairs.add(affairs[i]);
            }
        }
    }
    /*
    //os conf - 1:linux;2:win
    public boolean batchAddMonitor(int os, String taskID, Client client, int interval) {
        Boolean res1 = false;
        Boolean res2 = false;
        Boolean res3 = false;

        if (os == 1) {
            res1 = addMonitor(new MonitorAffair("mem", interval, new MemRemoteMonitor4Linux(taskID, client)));
            res2 = addMonitor(new MonitorAffair("cpu", interval, new CPURemoteMonitor4Linux(taskID, client)));
            res3 = addMonitor(new MonitorAffair("disk", interval, new DiskRemoteMonitor4Linux(taskID, client)));
        } else if (os == 2) {
            res1 = addMonitor(new MonitorAffair("mem", interval, new MemRemoteMonitor4Win(taskID, client)));
            res2 = addMonitor(new MonitorAffair("cpu", interval, new CPURemoteMonitor4Win(taskID, client)));
            res3 = addMonitor(new MonitorAffair("disk", interval, new DiskRemoteMonitor4Win(taskID, client)));
        }
        return res1 && res2 && res3;
    }
    */

    public boolean removeMonitor(MonitorAffair affair) {
        synchronized (monitorAffairs) {
            return monitorAffairs.remove(affair);
        }
    }

    public void removeMonitors(MonitorAffair[] affairs) {
        synchronized (monitorAffairs) {
            for (int i = 0; i < affairs.length; i++) {
                monitorAffairs.remove(affairs[i]);
            }
        }
    }

    public void initial() {
        synchronized (monitorAffairs) {
            for (MonitorAffair affair : monitorAffairs) {
                affair.initial();
            }
        }
    }

    private void start() {
        try {
            initial();

            while (true) {
                Thread.sleep(CYCLE);
                synchronized (monitorAffairs) {
                    for (final MonitorAffair affair : monitorAffairs) {
                        affair.remain -= CYCLE / 1000;
                        if (affair.remain <= 0) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    affair.detect();
                                }
                            };
                            Thread thread = new Thread(runnable);
                            thread.start();
                            affair.remain = affair.interval;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        start();
    }

    public static void main(String[] args) {
        Monitor memMon = new MemMonitor();
        MonitorAffair ma = new MonitorAffair("mem", 5, memMon);

        Monitor cpuMon = new CPUMonitor();
        MonitorAffair ma2 = new MonitorAffair("cpu", 5, cpuMon);

        Monitor diskMon = new DiskMonitor();
        MonitorAffair ma3 = new MonitorAffair("disk", 5, diskMon);

        MonitorManager mm = new MonitorManager();
        mm.addMonitor(ma);
        mm.addMonitor(ma2);
        mm.addMonitor(ma3);

        mm.start();
    }
}
