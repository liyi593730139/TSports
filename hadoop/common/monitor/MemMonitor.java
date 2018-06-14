package common.monitor;

import common.MysqlCtrl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MemMonitor implements Monitor {

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
    public MemMonitor() {
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
            String cmd = "cat /proc/meminfo";
            Runtime run = Runtime.getRuntime();
            Process p = run.exec(cmd);// 启动另一个进程来执行命令
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            String lineStr;
            String memTotal = "";
            String memFree = "";

            while ((lineStr = inBr.readLine()) != null) {
                if (lineStr.startsWith("MemTotal")) {
                    String[] lineStrArr = lineStr.split(":");
                    String s = lineStrArr[1];
                    s = s.trim();
                    String[] sArr = s.split(" ");
                    memTotal = sArr[0].trim();
                }
                if (lineStr.startsWith("MemFree")) {
                    String[] lineStrArr = lineStr.split(":");
                    String s = lineStrArr[1];
                    s = s.trim();
                    String[] sArr = s.split(" ");
                    memFree = sArr[0].trim();
                }
                if (memFree != "" && memTotal != "") {
                    break;
                }
            }
            in.close();
            inBr.close();

            System.out.println("MemTotal:" + memTotal);
            System.out.println("MemFree:" + memFree);

            int memTotalInt = (int)(Double.parseDouble(memTotal)/1000);
            int memFreeInt = (int)(Double.parseDouble(memFree)/1000);

            int useRate = (memTotalInt - memFreeInt) * 100 / memTotalInt;
            String state = "NORMAL";

            System.out.println("useRate:" + useRate);

            //test
            //useRate = 90;
            //

            if (useRate > 85 && recordCount >= 10) {
                int iCount = 0;
                for (int i : record) {
                    if (i > 85) {
                        iCount++;
                    }
                }
                if (iCount >= 7) {
                    String message = "ERROR:SCD-01002.LEVEL:5.内存使用率超过85%。";
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
                record[9] = useRate;
            } else {
                record[recordCount] = useRate;
                recordCount++;
            }

            //sql insert
            String sqlCmd = String.format("INSERT INTO MemInfo VALUES ('syshost', CURDATE(), CURTIME(), %d, %d, '%s');",
                    memTotalInt, memFreeInt, state);

            System.out.println(sqlCmd);
            sql.execUpdate(sqlCmd);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        Monitor mon = new MemMonitor();
        mon.initial();
        while (true) {
            mon.detect();
            Thread.sleep(5000);
        }
    }
}
