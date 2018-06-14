package common.monitor;

import common.MysqlCtrl;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class DiskMonitor implements Monitor {

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
    public DiskMonitor() {
        log = LogIntegration.getLogger();
    }

    @Override
    public void initial() {
        sql = MysqlCtrl.instance();
    }


    @Override
    public void detect() {

        try {
            String cmd = "df";
            Runtime run = Runtime.getRuntime();
            Process p = run.exec(cmd);// 启动另一个进程来执行命令
            BufferedInputStream in = new BufferedInputStream(p.getInputStream());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
            String lineStr;
            String diskTotal = " ";
            String diskUse = " ";
            String diskAvailable = " ";
            String diskUnFree = " ";
            int lineNum = 0;

            while ((lineStr = inBr.readLine()) != null) {
                lineNum++;
                String[] lineStrArr = lineStr.split("\\s+");
                if(lineStrArr.length > 4 && lineStrArr[lineStrArr.length - 1].equals("/")) {
                    diskTotal = lineStrArr[1];
                    diskUse = lineStrArr[2];
                    diskAvailable = lineStrArr[3];
                    diskUnFree = lineStrArr[4];
                    break;
                }
            }
            in.close();
            inBr.close();

            diskUnFree = diskUnFree.replace("%", "");
            System.out.println("diskTotal:" + diskTotal);
            System.out.println("diskUse:" + diskUse);
            System.out.println("diskAvailable:" + diskAvailable);
            System.out.println("diskUnFree:" + diskUnFree);

            int diskTotalInt = (int)(Double.parseDouble(diskTotal)/1000);
            int diskUseInt = (int)(Double.parseDouble(diskUse)/1000);
            int diskAvailableInt = (int)(Double.parseDouble(diskAvailable)/1000);
            int diskUnFreeInt = Integer.parseInt(diskUnFree);

            String state = "NORMAL";

            //test
            //diskUnFreeInt = 95;
            //

           if (diskUnFreeInt > 90) {
               String message = "ERROR:SCD-01003.LEVEL:5.磁盘使用率超过90%。";
               try {
                   throw new LogException(message);
               } catch (LogException msg) {
                   log.record(msg);
               }
               state = "ALERT";
           }

            /*
            if (recordCount >= 10) {
                for (int i = 0; i < 9; i++) {
                    record[i] = record[i+1];
                }
                record[9] = diskUnFreeInt;
            } else {
                record[recordCount] = diskUnFreeInt;
                recordCount++;
            }
             */

            //sql insert
            String sqlCmd = String.format("INSERT INTO DiskInfo VALUES('syshost', CURDATE(), CURTIME(), %d, %d, %d, %d, '%s');",
                    diskTotalInt, diskUseInt, diskAvailableInt, diskUnFreeInt, state);

            System.out.println(sqlCmd);
            sql.execUpdate(sqlCmd);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        Monitor mon = new DiskMonitor();
        mon.initial();
        while (true) {
            mon.detect();
            Thread.sleep(5000);
        }
        //Monitor mon2 = new CPUMonitor();
        //mon2.detect();
        //Monitor mon3 = new MemMonitor();
        //mon3.detect();
    }
}
