package common;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import manufacturer.License;
import java.util.HashMap;

/**
* 系统配置环境类，保存系统启动配文件内容，以一个Map来存储各项配置的键值信息。
*/
public class SystemEnv {
	ConcurrentHashMap<String, String> prop = new ConcurrentHashMap<>();
	public String permission="20300101 vm_vmware vm_hyperv files_linux files_windows database_mysql database_sqoop database_oracle database_sqlserver os_windowsserver client_20 space_10";
	/**
	 * @param filename
	 */
	public SystemEnv(String filename) {
		String homePath = "";
		String savePath = "";
		String logsPath = "logs";
		String ip = "localhost";
		String port = "9876";
		String missionPeriod = "5";
		this.prop.put("ip", ip);
		this.prop.put("port", port);
		this.prop.put("missionPeriod", missionPeriod);
		this.prop.put("savePath", savePath);
		this.prop.put("homePath", homePath);
		this.prop.put("logsPath", logsPath);
		if (this.load(filename) == false) {
			System.out.println("Error, load system.conf failured");
			System.out.println("Ask the manufacturer to get help");
			System.exit(0);
		}
	}
	/**
	 * @param line
	 */
	void setProperty(String line) {
		try {
			line = line.trim();
			if (line.length() < 1 || line.charAt(0) == '#'
					|| line.charAt(0) == ';')
				return;
			int eq = line.indexOf("=");
			if (eq < 1)
				return;
			String key = line.substring(0, eq).trim();
			String value = line.substring(eq + 1).trim();
			this.prop.put(key, value);
		} catch (Exception ex) {
		}
	}
	/**
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		if("license".equals(key)){
			return this.permission;
		}
		if (this.prop.containsKey(key) == false)
			return null;
		return this.prop.get(key);
	}
	public String getPath(String key, String defvalue) {
		String path=get(key,defvalue);
		if(path==null) return null;
		if(path.charAt(path.length()-1)!='/') path=path+"/";
		File f=new File(path);
		if(f.exists() || defvalue==null){
			if(f.isDirectory()) return path;
			else return null;
		}else{
			f.mkdirs();
			if(f.exists()) return path;
			else return null;
		}
	}
	/**
	 * @param key
	 * @param defvalue
	 * @return
	 */
	public String get(String key, String defvalue) {
		if (this.prop.containsKey(key) == false)
			return defvalue;
		String value = this.prop.get(key);
		if (null == value)
			return defvalue;
		return value;
	}
	/**
	 * @param key
	 * @param defvalue
	 * @return
	 */
	public int getInt(String key, int defvalue) {
		if (this.prop.containsKey(key) == false)
			return defvalue;
		try {
			String value = this.prop.get(key);
			int i = Integer.valueOf(value);
			return i;
		} catch (Exception ex) {
			return defvalue;
		}
	}
	/**
	 * @param key
	 * @param defvalue
	 * @return
	 */
	public Boolean getBool(String key, Boolean defvalue) {
		if (this.prop.containsKey(key) == false)
			return defvalue;
		String value = this.prop.get(key);
		if (null == value)
			return defvalue;
		value=value.trim().toLowerCase();
		if("yes".equals(value) || "y".equals(value) || "t".equals(value) || "true".equals(value)) return true;
		if("no".equals(value) || "n".equals(value) || "f".equals(value) || "false".equals(value)) return false;
		return defvalue;
	}
	/**
	 * @param filename
	 * @return
	 */
	boolean save(String filename) {
		try {
			FileWriter out = new FileWriter(filename, false);
			for (String key : this.prop.keySet()) {
				out.write(key + " = " + this.prop.get(key) + "\r\n");
			}
			out.close();
		} catch (Exception ex) {
			print("Save configure file error!");
			return false;
		}
		return true;
	}
	/**
	 * @param filename
	 * @return
	 */
	boolean load(String filename) {
		try {
			File f = new File(filename);
			Scanner fin = new Scanner(f);
			String line = "";
			while (fin.hasNext()) {
				line = fin.nextLine();
				this.setProperty(line);
			}
			fin.close();
		} catch (Exception ex) {
			print("Load configure file error!");
			return false;
		}
		return true;
	}
	public Timer validityPeriod(){
		System.out.println(permission);
		String p=permission.replaceAll("[#$%]", "").trim();
		int ps=p.indexOf(" ");
		if(ps==8) return Timer.set(p.substring(0,ps)+"235959");
		if(ps==14) return Timer.set(p.substring(0,ps));
		return null;
	}
	public int validityClientNumn() {
		String p = permission.replaceAll("[#$%]", "").trim();
		p = p.replaceAll("\\s+", " ");
		String[] pa = p.split(" ");
		int num = -1;
		for (int i=0; i < pa.length; i++) {
			if (pa[i].contains("clientnum")) {
				num = Integer.parseInt(pa[i].split("_")[1]);
			}
		}
		return num;
	}
	public String validitySpace()  { //TB
		String p = permission.replaceAll("[#$%]", "").trim();
		p = p.replaceAll("\\s+", " ");
		String[] pa = p.split(" ");
		String space = "not define";
		for (int i=0; i < pa.length; i++) {
			if (pa[i].contains("space")) {
				space = pa[i].split("_")[1];
			}
		}
		return space;
	}
	public void print(String a) {
		System.out.println(a);
	}
	public void print() {
		System.out.println(this.prop);
		System.out.println(validityPeriod());
	}
	public static void main(String args[]) throws Exception {
		SystemEnv env = new SystemEnv("system.conf");
		License.writeToEnv(env);
		System.out.println(env.getProperty("license"));
	}
}
