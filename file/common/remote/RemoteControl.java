package common.remote;

/** *  FileName: RemoteControl.java
 *  Description: 封装本地对客户端的数据传输和命令执行*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import common.SysLogger;
import common.Tools;

class FileAction {
	ArrayList<String> newFile = null;
	ArrayList<String> newDir = null;
	ArrayList<String> newLink = null;
	FileAction(HashMap<String,String> olddir, HashMap<String,String> newdir) {
		get(olddir,newdir);
		print();
	}
	void get(HashMap<String,String> olddir,HashMap<String,String> newdir) {
		newFile = new ArrayList<String>();
		newDir = new ArrayList<String>();
		newLink = new ArrayList<String>();
		if (newdir == null)
			return;
		if (olddir == null)
			olddir = new HashMap<String,String>();
		for (String file: newdir.keySet()) {
			String prop = newdir.get(file);
			if (olddir.containsKey(file) == false) {
				switch(prop.charAt(0)) {
					case '-': newFile.add(file); break;
					case 'd': newDir.add(file); break;
					default: newLink.add(file);
				}
			} else if (prop.equals(olddir.get(file)) == false) {
				switch(prop.charAt(0)) {
					case '-':newFile.add(file);break;
					case 'd':break;
					default:newLink.add(file);
				}}}}

	void print(){
		for(String f:newFile) {
			System.out.println("- "+f);
		}
		for(String f:newDir) {
			System.out.println("d "+f);
		}
		for(String f:newLink) {
			System.out.println("l "+f);
		}
	}
}

/*** RemoteControl封装本地对客户端的数据传输和命令执行
* @see Client
*/
public class RemoteControl {
	//客户端的IP地址和用户名
	private String ip, user;
	private int remoteConnect = 0;
	private Client c;

	//用户名
	private String uname;
	private String wordDirect;

	//用于标记客户端到底是Windows操作系统还是Linux操作系统
	public static final int WINDOWS = 1;
	public static final int LINUX = 2;

	/*** 私有构造函数：根据用户ip, 用户名建立连接
	* @param ip: 客户端的ip地址
	* @param user: 用户名
	* @param mask: 掩码
	*/
	private RemoteControl(String ip, String user, int mask) {
		this.ip = ip;
		this.user = user;
		this.uname = null;
		getRemoteConnect(mask);
		this.wordDirect = WorkDir();
		System.out.println(remoteConnect + " " + uname);
	}
	public String getUname() {
		return uname;
	}
	public String getWordDirect() {
		return wordDirect;
	}
	public Client getWClient() {
		if (remoteConnect != 1)
			return null;
		return c;
	}

	/*** 建立Windows客户端的连接
	* 在最初的设计中，与Agent进行通信的都是Windows，用ssh进行通信的都是Linux，后来无论windows还是linux都改为了与Agent通信
	* @param ip: 客户端ip
	* @param user: 客户端用户名
	*/
	public static RemoteControl getInstanceW(String ip, String user) {
		RemoteControl rc = new RemoteControl(ip, user, WINDOWS);
		if(rc.remoteConnect == 0)
			return null;
		return rc;
	}

	/*** 建立Linux客户端的连接
	* @param ip: 客户端ip
	* @param user: 客户端用户名
	* @deprecated
	*/
	public static RemoteControl getInstanceL(String ip, String user){
		RemoteControl rc = new RemoteControl(ip, user, LINUX);
		if(rc.remoteConnect == 0)
			return null;
		return rc;
	}

	/*** 建立客户端的连接，如果Windows连接方式则与Agent建立连接，
	* 当前默认是无论客户机是windows还是linux都是与Agent建立连接，只有Linux客户端的Agent挂了才用ssh方式
	* @param mask: 掩码，辨别到底是Windows还是Linux
	*/
	private void getRemoteConnect(int mask) {
		this.remoteConnect = 0;
		if((mask & WINDOWS) != 0)
		try {
			c = new Client(this.ip, 50000);
			ArrayList<String> list = c.getString("uname");
			if(list != null && list.size() == 1) {
				this.remoteConnect = WINDOWS;
				this.uname = list.get(0).toLowerCase();
				return;
			}
		} catch (Exception ex) {
			System.out.println("connect 1 failed");
			c = null;
		}
		if((mask & LINUX) != 0)
		try {
			ArrayList<String> list = getLocalString(new String[]{"ssh", user + "@" + ip, "uname"});
			if(list != null && list.size() == 1){
				this.remoteConnect = LINUX;
				this.uname = list.get(0).toLowerCase();
				return;
			}
		} catch(Exception ex) {
			System.out.println("connect 2 failed");
		}
		this.remoteConnect = 0;
	}

	/*** 通过ssh方式让客户端执行某条命令
	* @param cmd: 需要客户端执行的命令
	*/
	private int runSSH(String cmd) throws IOException {
		String cmds[] = new String[]{"ssh", user + "@" + ip, cmd};
		return runLocal(cmds);
	}
	public Client getClientConnect(){
		return c;
	}
	public int getTransferState(){
		return c.getState();
	}
	public void setTransferStop(){
		c.setStateStopped();
	}
	public void setTransferContinue(){
		c.setStateContinue();
	}
	public void setTransferCancel(){
		c.setStateCancel();
	}

	/*@deprecated*/
	public class Connect{
		Process p;
		BufferedReader br;
		BufferedWriter bw;
		public String readLine() throws IOException {
			return br.readLine();
		}
		public int close(){
			int exit=255;
			try{
				exit=p.exitValue();
				br.close();
			}catch(Exception ex){
			}
			return exit;
		}
		public void waitUntilFinished(){
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				return;
			}
		}
		public void writeLine(String str) throws IOException {
			bw.write(str);
			bw.newLine();
			bw.flush();
		}
	}
	public Connect open(String cmd)throws IOException {
		Connect c = new Connect();
		Runtime rt = Runtime.getRuntime();
		c.p = rt.exec(new String[]{"ssh", user + "@" + ip, cmd});
		InputStream stdout = c.p.getInputStream();
		c.br = new BufferedReader(new InputStreamReader(stdout));
		c.bw = new BufferedWriter(new OutputStreamWriter(c.p.getOutputStream()));
		return c;
	}

	/*** 在本地调用执行cmd命令
	* @param cmd[]: 需要本地执行的命令
	*/
	public static int runLocal(String cmd[]) throws IOException {
		for(String c:cmd)
			System.out.print(c+" ");
		System.out.println();
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec(cmd);
		InputStream stdout = p.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			System.out.println(line);
		}
		br.close();
		int exit = 255;

		//等待长命令的执行结束
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			exit = p.exitValue();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return exit;
	}

	/*** 调用本地目录归并命令
	* @param cmd[]: 需要本地执行的命令
	*/
	public static int runLocalMerge(String cmd[]) throws IOException {
		for(String c : cmd)
			System.out.print(c + " ");
		System.out.println();
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec(cmd);
		int size = -1;
		InputStream stdout = p.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			try {
				size = Integer.valueOf(line.trim());
			} catch(Exception e) {
				continue;
			}
		}
		return size;
	}
	public static ArrayList<String> getLocalString(String cmd[]) throws IOException {
    	ArrayList<String> dirs = new ArrayList<String>();
		Process p = Runtime.getRuntime().exec(cmd);
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			dirs.add(line.trim());
		}
		br.close();
		return dirs;
	}

	/*@deprecated*/
	private ArrayList<String> getDir0(String dir) throws IOException {
		return getLocalString(new String[]{"ls", "-alR", dir});
	}
	/*@deprecated*/
	public void run(String cmd,SysLogger log) throws Exception {
		try{
			if(log != null)
				log.debug(cmd);
			switch (this.remoteConnect) {
			case 1:
				System.out.println("IP: "+this.ip);
				c.run(cmd);
				break;
			case 2:
				try {
					runSSH(cmd);
				} catch (Exception ex) {}
				break;
			default:
				throw new IOException("Connect Error");
			}
		} catch(Exception ex) {
			if(log!=null)
				log.error(ex.toString());
			throw ex;
		}
	}

	/*** 对路径进行转码，主要是Linux和Windows路径的斜杠和反斜杠的转换
	* @param remote: 待转换的路径
	*/
	public static String encodePath(String remote) {
		StringBuffer sb = new StringBuffer();
		for(char c:remote.replaceAll("//", "/").toCharArray()){
			switch(c){
			case ' ':sb.append("\\ ");break;
			case '(':sb.append("\\(");break;
			case ')':sb.append("\\)");break;
			default:sb.append(c);
			}
		}
		return sb.toString();
	}

	/*@deprecated*/
	public static void putFile(String user, String ip, String remoteSrc, String localDst)throws Exception {
		Tools.mkdir(Tools.splitPath(localDst)[0], true);
		String remote = encodePath(user+"@"+ip+":"+remoteSrc);

		runLocal(new String[]{"sh","-c","scp -r "+localDst+" \""+remote+"\""});
	}

	/*@deprecated*/
	public void getFile(String remoteSrc,String localDst) throws Exception {
		switch(this.remoteConnect) {
		case 1:
			c.get(remoteSrc, localDst);
			break;
		case 2:
			Tools.mkdir(Tools.splitPath(localDst)[0], true);
			String remote = encodePath(user + "@" + ip + ":" + remoteSrc);
			runLocal(new String[]{"sh","-c","scp \""+remote+"\" "+localDst});
			break;
		default:throw new IOException("Connect Error");
		}
	}

	/*@deprecated*/
	public void putFile(String localSrc,String remoteDst) throws Exception {
		switch(this.remoteConnect) {
		case 1:
			c.put(localSrc, remoteDst);
			break;
		case 2:
			String remote = encodePath(user+"@"+ip+":"+remoteDst);
			runLocal(new String[]{"sh","-c","scp "+localSrc+" \""+remote+"\""});
			break;
		default:throw new IOException("Connect Error");
		}
	}

	public long wsyncDir(String remoteDir, String localDir, String logDir, String curTimeStr) throws Exception {
		return c.wsyncDir(remoteDir, localDir, logDir, curTimeStr);
	}

	/*@deprecated*/
	public void getDir(String remoteSrc,String localDst) throws Exception {
		switch(this.remoteConnect){
		case 1:
			c.getDir(remoteSrc, localDst+"/"+Tools.splitPath(remoteSrc)[1]);
			break;
		case 2:
			Tools.mkdir(localDst,true);
			String remote = encodePath(user+"@"+ip+":"+remoteSrc);
			runLocal(new String[]{"sh","-c","scp -r \""+remote+"\" "+localDst});
			break;
		default:throw new IOException("Connect Error");
		}
	}

	/*@deprecated*/
	public void putDir(String localSrc, String remoteDst) throws Exception {
		switch(this.remoteConnect){
		case 1:
			c.putDir(localSrc, remoteDst);
			break;
		case 2:
			String remote=encodePath(user + "@" + ip + ":" + remoteDst);
			runLocal(new String[]{"sh","-c","scp -r "+localSrc+" \""+remote+"\""});
			break;
		default:throw new IOException("Connect Error");
		}
	}

	/*** 调用客户端执行命令，并将运行命令结果返回
	* @param cmd:客户端需执行的命令
	* @return ArrayList<String>:执行命令的结果
	*/
	public ArrayList<String> runCmd(String cmd){
		ArrayList<String> strs = null;
		try {
			switch(this.remoteConnect){
			case 1:
				strs = c.getString(cmd);
				break;
			case 2:
				strs = getLocalString(new String[]{"ssh", user+"@"+ip,cmd});
				break;
			//default:throw new IOException("Connect Error");
			}
		} catch(Exception ex){
		}
		return strs;
	}

	/*** 获取客户端某个目录下的文件信息并将结果返回
	* 根据不同的系统执行不同的命令
	* @param dir:目录名称
	* @return ArrayList<String>:目录下所有文件信息
	*/
	public ArrayList<String> getDirInfo(String dir){
		ArrayList<String> dirs = null;
		try {
			//根据不同的系统执行不同的命令
			switch(this.remoteConnect){
			case 1:
				if("Windows".equals(uname) || "windows".equals(uname))
					dirs = c.getString("dir "+dir);
				else
					dirs = c.getString("ls -alR "+dir);
				break;
			case 2:
				dirs = getDir0(encodePath(dir));
				break;
			}
		}catch(Exception ex){}

		for(String s:dirs){
			System.out.println(this.remoteConnect+" "+s);
		}
		return dirs;
	}

	/*** 将目录信息写入指定文件
	* @param filename: 写入文件名称
	* @param dirs: 目录信息
	*/
	public void saveDirInfo(String filename, ArrayList<String> dirs)throws IOException{
		if(uname == null || dirs == null) return;
		FileWriter fw = new FileWriter(filename);
		fw.write(uname + "\n");
		for(String s:dirs){
			fw.write(s + "\n");
		}
		fw.close();
	}

	/*** 读取指定文件并将目录信息写入Map
	* @param filename:文件名称
	* @return HashMap<String,String>: 转载目录映射信息的MAP
	*/
	public static HashMap<String,String> loadDir(String filename) throws IOException {
		HashMap<String,String> map = new HashMap<String,String>();
		Scanner scan = new Scanner(new File(filename));
		String uname = scan.nextLine();
		String localdir = "";

		//循环读取，将信息写入Map
		while (scan.hasNext()) {
			String line = scan.nextLine().trim();
			String sp[] = line.split("\\s+", 9);
			if(sp.length > 8) {
				if(".".equals(sp[8]) == false && "..".equals(sp[8]) == false)
				map.put(localdir + sp[8], line);
			} else if(line.startsWith("/")) {
				localdir = line.substring(0, line.length()-1) + "/";
			}
		}
		scan.close();
		return map;
	}

	/*** 通过执行shell命令获取工作目录
	* @return String:工作目录
	*/
	private String WorkDir(){
		ArrayList<String> wds = null;
		try {
			//根据不同系统来执行不同的命令
			if (uname.equals("windows")) {
				wds = c.getString("agentinfo.bat");
				String wd = wds.get(2);
				int a = wd.lastIndexOf('\\');
				return wd.substring(0, a);
			} else if(uname.equals("linux")) {
				wds = c.getString("./agentinfo.sh");
				return wds.get(0);
			}
		}catch(Exception ex){}
		return null;
	}
	public static void main(String args[]){
		try{
		RemoteControl rc1=RemoteControl.getInstanceW("192.168.86.167","root");
		System.out.println(rc1.wordDirect);
		}catch(Exception ex){
			System.out.println(ex);
			ex.printStackTrace();
		}
	}
}
