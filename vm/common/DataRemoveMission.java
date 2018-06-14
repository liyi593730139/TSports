package common;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;

/**
* 作业删除类，记录待删除作业的hash值和备份时间点。
*/
public class DataRemoveMission implements Serializable{

	/**
	* 序列化传输
	*/
	private static final long serialVersionUID = 1L;

	/**
	* 从此次数据删除任务配置文件中解析需要字段。
	* @param path 配置文件路径。
	* @return DataRemoveMission 返回解析后的数据删除任务对象。
	* @throws 可能抛出文件处理异常。
	*/
	public static DataRemoveMission missionLoader(String path) throws Exception{
		Properties pro = new Properties();
		FileInputStream fi = new FileInputStream(path);
		pro.load(fi);
		String hash = pro.getProperty("hash");
		String rmTime = pro.getProperty("rmtime");
		if (verify(hash, rmTime)) {
			String[] rmTimes = rmTime.split(" ");
			return new DataRemoveMission(hash, rmTimes);
		} else {
			return null;
		}
	}

	/**
	* 构造数据删除任务实例。
	* @param hash 任务hash值。
	* @param rmTime 待删除作业时间点，多个时间点之间用空格隔开。
	* @return DataRemoveMission 返回数据删除任务对象。
	*/
	public static DataRemoveMission newInstance(String hash, String rmTime) {
		if (verify(hash, rmTime)) {
			String[] rmTimes = rmTime.split(" ");
			return new DataRemoveMission(hash, rmTimes);
		} else {
			return null;
		}
	}

	/**
	* 在构建数据删除任务前初步校验各字段是否合法。
	* @param hash 任务hash值。
	* @param rmTime 待删除时间点。
	* @return boolean 表明校验是否通过。
	*/
	private static boolean verify(String hash, String rmTime) {
		if (hash == null || rmTime == null || hash.length() == 0 || rmTime.length() == 0) {
			return false;
		}
		return true;
	}


	/**
	* 待删除作业所属的任务hash值。
	*/
	private final String hash;

	/**
	* 待删除作业时间点。
	*/
	private final String[] rmTimes;

	/**
	* 作业删除对象构造方法。
	* @param hash 作业所属任务hash值。
	* @param rmTimes 待删除作业时间点。
	*/
	private DataRemoveMission(String hash, String[] rmTimes) {
		this.hash = hash;
		this.rmTimes = rmTimes;
	}

	/**
	* 获得作业删除对象所属hash值。
	* @return String 作业对象所属hash值。
	*/
	public String getHash() {
		return hash;
	}

	/**
	* 获得作业删除对象待删除时间点。
	* @return String[] 作业对象待删除时间点。
	*/
	public String[] getRmTimes() {
		return rmTimes;
	}
}
