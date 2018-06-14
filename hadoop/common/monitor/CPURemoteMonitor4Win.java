package common.monitor;

import common.MysqlCtrl;
import common.remote.Client;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CPURemoteMonitor4Win implements Monitor {

	/**
	* 客户端检测脚本相对路径。
	*/
	public static final String INVOKE_CMD = "monitor\\cpuinfo.bat";

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
    * @param taskID 监控器对应任务id。
    * @param client 客户端操作句柄。
    */
    public CPURemoteMonitor4Win(String taskID, Client client) {
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

        recordCount = 0;
        record = new int[10];
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
            String cpuUse = " ";
            int lineNum = 0;

            while ((lineStr = inBr.readLine()) != null) {
                cpuUse = lineStr;
                break;
            }

            socket.close();

            if (cpuUse.equals("")) {
                return;
            }

            int cpuUseRate  = Integer.parseInt(cpuUse);
            int cpuFreeInt = 100 - cpuUseRate;
            String state = STATE_NORMAL;

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
                    String message = "ERROR:SCD-01101.LEVEL:4.处理器使用率超过85%。";
                    try {
                        throw new LogException(taskId, LogException.TYPE_CLIENT_RESOURCE, message);
                    } catch (LogException msg) {
                        log.record(msg);
                    }
                    state = STATE_ALERT;
                }
            }

            if (recordCount >= 10) {
                for (int i = 0; i < 9; i++) {
                    record[i] = record[i + 1];
                }
                record[9] = cpuUseRate;
            } else {
                record[recordCount] = cpuUseRate;
                recordCount++;
            }

            //result record, sql insert
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timeStamp = df.format(new Date());

            FileOutputStream fo = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fo);
            String theRecord = String.format("%s CPU %d %d %s", timeStamp, cpuUseRate, cpuFreeInt, state);
            pw.println(theRecord);
            pw.close();

            if (state.equals(STATE_ALERT)) {
                String sqlCmd = String.format("INSERT INTO ClientCpuInfo VALUES('%s', CURDATE(), CURTIME(), %d, %d, '%s');",
                        client.getIP(), cpuUseRate, cpuFreeInt, state);

                System.out.println(sqlCmd);
                sql.execUpdate(sqlCmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception{
        CPURemoteMonitor4Win mon = new CPURemoteMonitor4Win("0000", new Client("127.0.0.1", 50000));
        mon.detect();
    }
}
