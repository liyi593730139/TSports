package common.monitor;
import common.MysqlCtrl;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CPUMonitor implements Monitor {

    /**
    * 检测执行次数计数。
    */
    int recordCount;

    /**
    * 缓存之前检测结果，用于检测结果判断。
    */
    int[] record;

    /**
    * 数据库操作句柄。
    */
    MysqlCtrl sql;

    /**
    * 日志记录句柄。
    */
    LogIntegration log;


    /**
    * 构造方法。
    */
    public CPUMonitor() {
        recordCount = 0;
        record = new int[10];
        log = LogIntegration.getLogger();
    }

    @Override
    public void initial() {
        sql = MysqlCtrl.instance();
    }

    @Override
    public void detect() {

        try {
            String cmd = "vmstat 1 3";
            Runtime run = Runtime.getRuntime();
            Process p = run.exec(cmd);// 启动另一个进程来执行命令
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            String lineStr;
            String cpuFree = " ";
            int lineNum = 0;

            while ((lineStr = inBr.readLine()) != null) {
                lineNum++;
                if (lineNum == 5) {
                    String[] lineStrArr = lineStr.split("\\s+");
                    cpuFree = lineStrArr[lineStrArr.length-3];
                }
            }
            in.close();
            inBr.close();

            //System.out.println("MemTotal:" + memTotal);
            System.out.println("cpuFree:" + cpuFree);

            int cpuFreeInt = Integer.parseInt(cpuFree);
            int cpuUseRate = 100 - cpuFreeInt;
            String state = "NORMAL";

            //test
            //cpuUseRate = 90;
            //

            if (cpuUseRate > 85 && recordCount >= 10) {
                int iCount = 0;
                for (int i : record) {
                    if (i > 85) {
                        iCount++;
                    }
                }
                if (iCount >= 7) {
                    String message = "ERROR:SCD-01001.LEVEL:5.处理器使用率超过85%。";
                    try {
                        throw new LogException(message);
                    } catch (LogException msg) {
                        log.record(msg);
                    }
                    state = "ALERT";
                }
            }

            if (recordCount >= 10) {
                for (int i = 0; i < 9; i++) {
                    record[i] = record[i+1];
                }
                record[9] = cpuUseRate;
            } else {
                record[recordCount] = cpuUseRate;
                recordCount++;
            }

            //sql insert
            String sqlCmd = String.format("INSERT INTO CpuInfo VALUES('syshost', CURDATE(), CURTIME(), %d, %d, '%s');",
                    cpuUseRate, cpuFreeInt, state);

            System.out.println(sqlCmd);
            sql.execUpdate(sqlCmd);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        Monitor mon = new CPUMonitor();
        mon.initial();
        while (true) {
            mon.detect();
            Thread.sleep(5000);
        }
    }
}
