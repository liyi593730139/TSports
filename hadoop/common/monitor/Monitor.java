package common.monitor;

public interface Monitor {

    public static final String STATE_NORMAL = "NORMAL";

    public static final String STATE_ALERT = "ALERT";

public void initial();

    public void detect();
}
