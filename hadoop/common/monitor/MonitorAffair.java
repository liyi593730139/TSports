package common.monitor;

public class MonitorAffair {

    String name;

    int interval; //second

    int remain; //second

    Monitor monitor;

    public MonitorAffair(String name, int interval, Monitor monitor) {
        this.name = name;
        this.interval = interval;
        this.remain = interval;
        this.monitor = monitor;
    }
    public void initial() {
        monitor.initial();
    }

    public void detect() {
        monitor.detect();
    }
}
