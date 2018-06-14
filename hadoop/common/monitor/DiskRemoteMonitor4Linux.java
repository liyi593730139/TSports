package common.monitor;

import common.MysqlCtrl;
import common.remote.Client;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DiskRemoteMonitor4Linux implements Monitor {

	/**
	* 客户端检测脚本相对路径。
	*/
	public static final String INVOKE_CMD = "df";

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
    public DiskRemoteMonitor4Linux(String taskID, Client client) {
        this.taskId = taskID;
        this.client = client;
        String clientIP = client.getIP();
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
            String diskTotal = " ";
            String diskUse = " ";
            String diskAvailable = " ";
            String diskUnFree = " ";

            while ((lineStr = inBr.readLine()) != null) {
                String[] lineStrArr = lineStr.split("\\s+");
                if(lineStrArr.length > 4 && lineStrArr[lineStrArr.length - 1].equals("/")) {
                    diskTotal = lineStrArr[1];
                    diskUse = lineStrArr[2];
                    diskAvailable = lineStrArr[3];
                    diskUnFree = lineStrArr[4];
                    break;
                }
            }
            socket.close();

            diskUnFree = diskUnFree.replace("%", "");
            System.out.println("diskTotal:" + diskTotal);
            System.out.println("diskUse:" + diskUse);
            System.out.println("diskAvailable:" + diskAvailable);
            System.out.println("diskUnFree:" + diskUnFree);

            int diskTotalInt = (int)(Double.parseDouble(diskTotal)/1000);
            int diskUseInt = (int)(Double.parseDouble(diskUse)/1000);
            int diskAvailableInt = (int)(Double.parseDouble(diskAvailable)/1000);
            int diskUnFreeInt = Integer.parseInt(diskUnFree);

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

    public static void main(String[] args) throws Exception{
        DiskRemoteMonitor4Linux mon = new DiskRemoteMonitor4Linux("0000", new Client("127.0.0.1", 50000));
        mon.detect();
    }
}
