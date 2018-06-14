package common;

//日志类，将信息写入硬盘文件中，日志位置由system.conf中的"logsPath"决定

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SysLogger {
	private Logger log = null;

	public SysLogger(String name) {
		log = Logger.getLogger(name);
		log.setLevel(Level.INFO);
	}

	public SysLogger(String name, SystemEnv env) {
		log = Logger.getLogger(name);
		log.setLevel(Level.ALL);
		String logsPath=env.get("logsPath", "logs");
		try {
			File f=new File(logsPath);
			if(f.exists()){
				if(f.isDirectory()==false) throw new Exception("logsPath is not a directory");
			}else f.mkdirs();
			System.out.println(logsPath+System.getProperty("file.separator")+name+"_%g.log");
			FileHandler fileHandler = new FileHandler(logsPath+System.getProperty("file.separator")+name+"_%g.log");
			fileHandler.setLevel(Level.ALL);
			fileHandler.setFormatter(new Formatter() {
				public String format(LogRecord record) {
					return Timer.current() + " [" + record.getLevel() + "] "
							+ record.getMessage() + "\r\n";
				}
			});
			log.addHandler(fileHandler);
		} catch (Exception ex) {
			System.out.println("Error, "+ex);
		}
	}

	/**
	 * 用于一般的调试，主要让开发人员查看
	 * @param msg
	 */
	public void debug(String msg) {
		log.fine(msg);
	}

	/**
	 * 普通的信息反馈
	 * @param msg
	 */
	public void info(String msg) {
		log.info(msg);
	}
	/**
	 * 任务执行时参数有异常，但可以自行处理
	 * @param msg
	 */
	public void warning(String msg) {
		log.warning(msg);
	}
	/**
	 * 任务执行时遇到异常被迫退出，或系统遇到问题需要留意
	 * @param msg
	 */
	public void error(String msg) {
		log.severe(msg);
	}
	/**
	 * 系统严重故障，几乎不能再正常运行
	 * @param msg
	 */
	public void critica(String msg) {
		log.severe(msg);
		System.out.println("critica");
	}

    public static void main(String[] args) throws IOException {
		SystemEnv env = new SystemEnv("system.conf");
    	SysLogger log=new SysLogger("aa",env);
    	log.info("123");
    }
}
