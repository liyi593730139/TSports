package common.remote;

/***  FileName: Client.java
 *  Description: 与备份机器建立连接*/
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import common.Tools;

/*** Client 是 客户端机器Agent建立连接的类，主要功能有两个：1.让客户端执行某个命令 2.让客户端传输某个文件
*/
public class Client {
	/** 文件传输过程中的三种状态，分别我为：继续，停止，取消传输*/
	enum State { CONTINUED, STOPPED, CANCEL};

	/** bufferSize: 发送buffer的大小
	* chunkSize: 一次读取文件的大小上限，防止文件过大加载不进内存
	*/
	private static final int bufferSize = 1024;
	private static final int chunkSize = 1024*1024*64;

    /*** @param socket: 与客户端建立连接的阻塞socket
	* @param ip:客户端的ip
	* @param port: 客户端连接的端口
	* @param state: 对应作业的状态，默认初始化为CONTINUE
	*/
    private Socket socket;
    private String ip;
    private int port;
    private volatile State state;

    /*** 构造函数
    * @param ip:客户端的ip
	* @param port: 客户端连接的端口
    */
    public Client(String ip,int port) {
		this.ip = ip;
		this.port = port;
    }
	public String getIP() {
		return this.ip;
	}
	public int getPort() {
		return this.port;
	}

	/*** 与客户端机器建立连接并执行某个命令，并将所得信息以ArrayList的形式返回。
	* 先与客户端建立socket连接，把要执行的命令发送给客户端，
	* 客户端执行之后将执行结果返回，本地以一行一行的方式读取，然后以Array<String>的形式返回。
	* @param cmd0: 发送给客户端的命令
	* @return ArrayList<String>: 客户端执行某项命令后返回的内容，如是否成功，以及命令运行结果
	*/
    public ArrayList<String> getString(String cmd0) throws Exception {
    	ArrayList<String> dirs = new ArrayList<String>();
    	String encoding = System.getProperty("file.encoding");
        String cmd = encoding + "@-%-@" + cmd0;
		socket = new Socket(ip, port);
		BufferedReader in = new BufferedReader(new InputStreamReader( socket.getInputStream() ));
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
		out.println(cmd);
		String line = null;
		while (null != (line = in.readLine() )) {
	        line = line.trim();
	        dirs.add(line);
		}
		socket.close();
    	return dirs;
    }

    /*** 检查本线程是否被中断，如果是则关闭socket
	* @param s:本线程管理的socket
	*/
    private void checkInterupted(Socket s) throws InterruptedException {
    	if (Thread.currentThread().isInterrupted()) {
    		try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    		throw new InterruptedException();
    	}
    }

    private void checkInterupted() throws InterruptedException{
    	if(Thread.currentThread().isInterrupted()){
    		throw new InterruptedException();
    	}
    }

    /*** 与客户端机器建立连接并执行某个命令，如有问题直接抛出异常
	* @param cmd0: 发送给客户端的命令
	*/
    public void run(String cmd0) throws Exception {
        System.out.println("Command:"+cmd0);
        String cmd = cmd0;
    	String encoding = System.getProperty("file.encoding");
        cmd = encoding + "@-%-@" + cmd;
		socket = new Socket(ip, port);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		//As coding of Go is utf-8
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream() , "utf-8"), true);
		out.println(cmd);
		String line = null;
		String err = null;
		while (null != (line = in.readLine() )) {
	        System.out.println(line);
	        if(line.startsWith("ERROR"))
	        	err = line;
		}
		socket.close();
		if (err != null)  //有异常直接抛出
			throw new Exception(err);
    }
    public void setStateStopped() {
    	state = State.STOPPED;
    }
    public void setStateContinue() {
    	state = State.CONTINUED;
    }
    public void setStateCancel() {
    	state = State.CANCEL;
    }

    /*** 返回文件传输的状态
	* @return int: 0 代表继续， 1代表停止， 2代表取消传输
	*/
    public int getState(){
    	if(state == State.CONTINUED){
    		return 0;
    	}else if(state == State.STOPPED){
    		return 1;
    	}else if(state == State.CANCEL){
    		return 2;
    	}
    	return 0;
    }

    /*** 从本地读文件然后发送到客户端
	* @param filename: 本地文件的名称
	* @param os: 输出流
	* @param fsize: 文件大小
	*/
    private static void readFile(String filename, OutputStream os, long fsize) throws Exception{
    	//若文件大小 < chunkSize，直接将整个文件读入内存，并以阻塞IO的方式发送
    	if(fsize < chunkSize){
    		FileInputStream fis = new FileInputStream(filename);
    		byte bf[] = new byte[bufferSize];
    		int n;
    		while((n = fis.read(bf)) > 0){
    			os.write(bf,0,n);
    		}
    		fis.close();
    	} else {
    		//若文件大小 > chunkSize，则每次读取chunkSize的数据到内存，并以阻塞IO的方式发送， 循环到发送完整个文件
            FileChannel fileChannel = new RandomAccessFile(filename, "r").getChannel();
            MappedByteBuffer mbb = null;
            byte[] data = new byte[chunkSize];
            long pos = 0;
            long len = fsize;
            int tmp;

            //循环传输，知道待传文件小于chunkSize
            while (len >= chunkSize) {
                mbb = fileChannel.map(MapMode.READ_ONLY, pos, chunkSize);
                mbb.get(data, 0, (int) chunkSize);
                for(int i = 0; i < chunkSize; i += 1024)
                	os.write(data, i, 1024);
                len -= chunkSize;
                pos += chunkSize;
            }
            if (len > 0) {
                mbb = fileChannel.map(MapMode.READ_ONLY, pos, len);
                data = new byte[(int) len];
                mbb.get(data, 0, (int) len);
                for (int i = 0; i < len; i += 1024) {
                	tmp = (int) ((len-i)>1024?  1024: len-i);
                	os.write(data, i, tmp);
                }
            }
            data = null;
            fileChannel.close();
    	}
	}

	/*** 从本地读文件然后发送到客户端
	* @param localSrc: 本地文件名
	* @param remoteDes: 远端文件名
	*/
    public void put(String localSrc,String remoteDes) throws Exception {
        File f = new File(localSrc);
        if (!f.exists() || !f.isFile()) {
        	System.out.println(localSrc + "not exits or not a file");
        	return;
        }

        //先传输所执行的操作和待传文件信息
        String cmd = "put " + remoteDes + "@-%-@" + f.length();
        System.out.println(cmd);

        //建立socket连接
		Socket socket = new Socket(ip, port);
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
		checkInterupted(socket);

		//传输具体文件数据
		out.println(cmd);
		if((is.read()) >= 1)
			readFile(localSrc, os, f.length());
		byte[] bf = new byte[bufferSize];
		if( (is.read(bf)) >= 1){
			System.out.println(new String(bf));
		}
		socket.close();
    }

    /*** 从客户端读文件然后写在本地。
	* 先查看是否需要新建文件的父路径，然后接收文件数据，写入到本地文件。
	* @param remoteSrc: 本地文件名
	* @param localDes: 远端文件名
	* @return long: 成功后返回所写文件的大小，失败则返回0
	*/
	private long writeFile(String remoteSrc, String localDes) throws Exception {
		//发送让客户端传输文件的命令
		String getCommand = "get " + remoteSrc;
        System.out.println(getCommand);
		InputStream is = socket.getInputStream();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
		out.println(getCommand);
		File file = new File(localDes);

		//本地新建目录
		if(!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}

		//读取filesize
		FileOutputStream fos = new FileOutputStream(localDes);
		byte bf[] = new byte[bufferSize];
		int n;
		n = is.read(bf);
		long filesize = 0, tmpSize = 0;
		if (n != 8) {
			System.out.println(new String(bf));
			fos.close();
			return 0;
		}
		for (int i = 7; i >= 0; i--) {
			filesize = filesize << 8;
			filesize += (bf[i]+256)%256;
		}
		System.out.println("Filesize is :"+filesize);
		out.println("Please send");
		//循环读文件数据，直到接受到的文件大小和所给的文件信息中的filesize一致才结束
		while (tmpSize != filesize){
 			n = is.read(bf);
 			if (n <= 0)
 				break;
			fos.write(bf,0,n);
			tmpSize += n;
		}
		fos.close();
		if (tmpSize == filesize) {
			System.out.println("Get File : " + localDes + " successfully!");
			return filesize;
		} else {
			System.out.println("Get File : " + localDes + " failed!");
			return 0;
		}
	}

	/*** 从客户端读文件然后写在本地
	* @param remoteSrc: 本地文件名
	* @param localDes: 远端文件名
	*/
    public void get(String remoteSrc, String localDes) throws Exception{
    	socket = new Socket(ip, port);
		writeFile(remoteSrc, localDes);
		socket.close();
    }

    /*** 在客户端创建文件夹
	* 让客户端根据接收到的参数新建空文件夹
	* @param remoteDir: 本地文件名
	*/
    public void makeDir(String remoteDir) throws Exception {

    	String getCommand = "makeDir " + remoteDir;
        System.out.println(getCommand);

		//与客户端建立连接，并发送新建文件的命令
		socket = new Socket(ip, port);
		InputStream is = socket.getInputStream();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream() , "utf-8"), true);
		out.println(getCommand);
		byte[] bf = new byte[bufferSize];
		is.read(bf);
		System.out.println(new String(bf));
 		socket.close();
    }

    /*** 将本地目录传输到客户端，中间需要检测做作业的状态。
	* 将本地的目录完整传输到客户端，每传完一个文件后都需要检测作业的状态。
	* @param localDir: 本地目录
	* @param remoteDir: 远端目录
	*/
    public void putDir(String localDir, String remoteDir) throws Exception {
    	File f = new File(localDir);
    	if (!f.isDirectory()) {
    		System.out.println("Not a directory");
    		return;
    	}
    	makeDir(remoteDir);
    	File[] files = f.listFiles();
    	for (int i = 0; i < files.length; i++) {
    		//检测状态是否发生变化
    		while (state == State.STOPPED) {
    			checkInterupted();
				TimeUnit.SECONDS.sleep(2);
			}
    		if (state == State.CANCEL) {
    			System.out.println("You cancel it");
    			return;
    		}

    		//是文件则执行文件传输操作，是目录则执行目录传输操作
    		if (files[i].isFile()) {
    				String str = files[i].getPath().substring(f.getPath().length());
        			put(files[i].getPath(), remoteDir+str);
    		} else if (files[i].isDirectory()) {
    			String str = files[i].getPath().substring(f.getPath().length());
    			putDir(files[i].getPath(), remoteDir+str);
    		}
    	}
    }

    /*** 将客户端的目录传输到本地，中间需要检测做作业的状态。
	* 非增量备份，传输完整的文件。
	* @param remoteDir: 客户端目录
	* @param localDir: 本地目录
	*/
    public void getDir(String remoteDir, String localDir) throws Exception{
    	String getCommand = "getDir " + remoteDir + "@-%-@" + localDir;
        System.out.println(getCommand);

        //与客户端建立socket连接，并将传输目录的命令发送过去
		socket = new Socket(ip, port);
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		PrintWriter out = new PrintWriter(os,true);
		out.println(getCommand);
		byte[] bf = new byte[bufferSize];
		int n;

		//循环执行读操作，读数据，知道读到操作为"end"
		while ( (n = is.read(bf)) > 0) {
			String str = new String(bf, 0, n, "utf-8");
			System.out.println(str);
			//接收文件
			if (str.startsWith("get ")) {
				//获取文件
				str = str.substring(4);
				String[] strs = str.split("@-%-@");
				writeFile(strs[0], strs[1]);
			} else if (str.startsWith("makeDir ")) {
				//本地创建文件夹
				str = str.substring(8);
				File folder = new File(str);
				if (!folder.exists() || !folder.isDirectory()) {
					if (folder.mkdir()) {
						System.out.printf("make dir : %s successfully\n", str);
					} else {
						System.out.printf("make dir : %s failed\n", str);
					}
				}

			} else if (str.startsWith("end")) {    //结束
				break;
			} else if (str.startsWith("error")) {  //出错了
				System.out.println(str);
				break;
			} else {
				System.out.println(str);
				break;
			}

			//检测状态是否发生变化
			while (state == State.STOPPED) {
				checkInterupted(socket);
				TimeUnit.SECONDS.sleep(2);
			}
			checkInterupted(socket);
			if (state == State.CANCEL) {
				out.println("cancel");
			} else {
				out.println("next");
			}
		};
		socket.close();
    }

    /*** 将客户端的目录增量备份到本地，中间需要检测做作业的状态
	* 增量备份，每次传输完一个文件后都需要检测作业的状态。
	* @param remoteDir: 客户端目录
	* @param localDir: 本地目录
	* @param logDir: 日志目录
	* @param curTimeStr: 备份的时间点
	* @return long: 所有备份文件的大小总和
	*/
    public long wsyncDir(String remoteDir, String localDir, String logDir, String curTimeStr) throws Exception {
    	String getCommand = "wsyncDir " + remoteDir + "@-%-@" + localDir + "@-%-@" + logDir + "@-%-@" + curTimeStr;
        System.out.println(getCommand);

        //与客户端建立socket连接，并将传输目录的命令发送过去
		socket = new Socket(ip,port);
		InputStream is = socket.getInputStream();
		PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);
		out.println(getCommand);
		byte[] bf = new byte[bufferSize];
		int n;
        long usedFileSize = 0;

        //循环执行读操作，读数据，知道读到操作为"end"
		while ( (n = is.read(bf)) > 0) {
			String str = new String(bf, 0, n, "utf-8");
			System.out.println(str);
			if (str.startsWith("get ") || str.startsWith("Log")) {
				//获取普通文件或者日志文件
				str = str.substring(4);
				String[] strs = str.split("@-%-@");
				writeFile(strs[0], strs[1]);
			} else if(str.startsWith("makeDir ")) {
				//本地创建文件夹
				str = str.substring(8);
				Tools.mkdir(str, true);
			} else if (str.startsWith("end")) {                       //传输完成
				break;
			} else if (str.startsWith("error")) {                     //出错了
				System.out.println(str);
				break;
			} else {
				System.out.println(str);
				break;
			}

			//检测状态是否发生变化
			while (state == State.STOPPED) {
				checkInterupted(socket);
				TimeUnit.SECONDS.sleep(2);
			}
			if (state == State.CANCEL) {
				if (str.startsWith("Log")) {
					System.out.println("Can not cancel now!");
					state = State.CONTINUED;
					out.println("next");
				} else {
					out.println("cancel");
				}
			} else {
				out.println("next");
			}
		};
		socket.close();
        return usedFileSize;
    }
    public static void main(String[] args) {
		try {
			Client c=new Client("192.168.0.1", 50000);
			c.run("ls -R /home/tester");
		} catch(Exception e) {
            System.out.println(e);
        }
    }
}
