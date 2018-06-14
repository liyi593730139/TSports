package common.monitor;

import common.MysqlCtrl;
import common.remote.Client;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DiskRemoteMonitor4Win implements Monitor {

	/**
	* 客户端检测脚本相对路径。
	*/
    public static final String INVOKE_CMD = "monitor\\diskinfo.bat";

	/**
	* 客户端监控对应任务ID。
	*/
    String taskId;

    /**
    * 客户端脚本调用接口。
    */
    Client client;

    /**
    * 日志文件句柄。
    */
    File file;

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
    * @param taskID 监控器对应任务id。
    * @param client 客户端操作句柄。
    */
    public DiskRemoteMonitor4Win(String taskID, Client client) {
        this.taskId = taskID;
        this.client = client;
        String clientIP = client.getIP();
        /*
        file = new File("syslog/clientmonitor/" + clientIP + ".out");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        log = LogIntegration.getLogger();
        sql = MysqlCtrl.instance();
        */
    }

    @Override
    public void initial() {}

    @Override
    public void detect() {

        try {
            String encoding = System.getProperty("file.encoding");
            String invoke = encoding + "@-%-@" + INVOKE_CMD;
            Socket socket = new Socket(client.getIP(), client.getPort());
            BufferedReader inBr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //As coding of Go is utf-8
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
            out.println(invoke);

            String lineStr;
            String[] lineStrArr;
            int lineCount = 0;
            int diskTotal = 0;
            String diskUse = " ";
            int diskAvailable = 0;
            String diskUnFree = " ";

            while ((lineStr = inBr.readLine()) != null) {
                lineCount++;
                if (lineCount > 3) {
                    lineStrArr = lineStr.split("\\s+");
                    if (lineStrArr.length > 3) {
                        diskAvailable += Long.parseLong(lineStrArr[2])/1000000;
                        diskTotal += Long.parseLong(lineStrArr[3])/1000000;
                    }
                }
            }
            socket.close();

            System.out.println(diskAvailable);
            System.out.println(diskTotal);

            int diskTotalInt = diskTotal;
            int diskAvailableInt = diskAvailable;
            int diskUseInt = diskTotalInt - diskAvailableInt;
            int diskUnFreeInt = (int) ((float)diskUseInt/(float) diskTotal * 100);

            String state = STATE_NORMAL;

            //test
            //diskUnFreeInt = 95;
            //

            if (diskUnFreeInt > 90) {
                String message = "ERROR:SCD-01103.LEVEL:4.磁盘使用率超过90%。";
                try {
                    throw new LogException(taskId, LogException.TYPE_CLIENT_RESOURCE, message);
                } catch (LogException msg) {
                    log.record(msg);
                }
                state = STATE_ALERT;
            }

            //result record, sql insert
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStamp = df.format(new Date());

            FileOutputStream fo = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fo);
            String theRecord = String.format("%s DIS %d %d %d %d %s",
                    timeStamp, diskTotalInt, diskUseInt, diskAvailableInt, diskUnFreeInt, state);
            pw.println(theRecord);
            pw.close();

            if (state.equals(STATE_ALERT)) {
                String sqlCmd = String.format("INSERT INTO ClientDiskInfo VALUES('%s', CURDATE(), CURTIME(), %d, %d, %d, %d, '%s');",
                        client.getIP(), diskTotalInt, diskUseInt, diskAvailableInt, diskUnFreeInt, state);

                System.out.println(sqlCmd);
                sql.execUpdate(sqlCmd);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DiskRemoteMonitor4Win mon = new DiskRemoteMonitor4Win("0000", new Client("127.0.0.1", 50000));
        mon.detect();
    }
}
