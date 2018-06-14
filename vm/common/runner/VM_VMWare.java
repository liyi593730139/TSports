package common.runner;

import java.util.Date;
import java.util.List;
import common.Mission;
import common.MissionRunnerBase;
import common.MissionServer;
import common.SCDException;
import common.SystemEnv;
import common.Tools;
import common.remote.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class VM_VMWare extends MissionRunnerBase {
	public final static int ERROR_VW=10000;
	//参数问题
	public final static int ERROR_VW_VMID=ERROR_VW+1;// invalid vmid
	//数据文件问题
	public final static int ERROR_VW_VMX_N=ERROR_VW+11;// vmx file not found
	public final static int ERROR_VW_VMX_D=ERROR_VW+12;// vmx file duplicated
	public final static int ERROR_VW_GET_REMOTE_FILE=ERROR_VW+13;// get file from remote error
	public final static int ERROR_VW_PUT_REMOTE_FILE=ERROR_VW+14;// send file to remote error
	public final static int ERROR_VW_SEARCH_FILE=ERROR_VW+15;// not found such file
	//远程控制问题
	public final static int ERROR_VW_POWERON=ERROR_VW+21;// power on
	public final static int ERROR_VW_POWEROFF=ERROR_VW+22;// power off
	public final static int ERROR_VW_RM_SNAPSHOT=ERROR_VW+23;// remove shapshot
	public final static int ERROR_VW_RV_SNAPSHOT=ERROR_VW+24;// revert/recover shapshot
	public final static int ERROR_VW_CREATE=ERROR_VW+25;// create a Virtual Machine
	public final static int ERROR_VW_RM_VM=ERROR_VW+26;// remove a Virutual Machine
	public final static int ERROR_VW_RM_DIR=ERROR_VW+27;// remove a direct in VMWare
	public final static int ERROR_VW_REGISTER_VM = ERROR_VW + 28; //can not register a vm via .vmx
	//其他问题
	public final static int ERROR_VW_DANGEROUS_OPERATION = ERROR_VW + 100; //dangerous operation
	public final static int ERROR_VW_INVALID_ARGU = ERROR_VW + 101;//invalid argument
	public final static int ERROR_VW_INVALID_OPERATION = ERROR_VW + 102; //invalid operation
	public final static int ERROR_VW_UPLOAD_FAILED = ERROR_VW + 103; //fail to upload file
	public final static int ERROR_VW_CONNECT_VM_FAILED = ERROR_VW + 104;//fail to connect to vm
	public final static int ERROR_VW_BACKUP_STATE_WHEN_POWER_ON = ERROR_VW + 104; //try to backup vm's state when it powered on

	private String vm_cmd = null;

	private VM_VMWare(Mission mis, SystemEnv env) throws Exception{
		super(mis, env, false);
		this.mis = mis;
		rc=RemoteControl.getInstanceL(this.ip, this.user);
		//连接失败
		if(rc==null){
			logger(2, ERROR_VW_CONNECT_VM_FAILED, "连接vmware esx(i)失败，ip： " + ip);
			throw new SCDException(ERROR_VW_CONNECT_VM_FAILED, "连接vmware esx(i)失败，ip： " + ip);
		}

		vm_cmd = getVmCmd();
	}

	/* function：获取指定vmware上所有虚拟机的信息
    * Parameters:
    *  ip: Vmware ip
    *  user: Vmware user
    * Return:
    *  一个包含所有虚拟机信息的Json字符串
    */
    public static VM_VMWare getInstance(Mission mission, SystemEnv env){
    	VM_VMWare vm = null;
    	try {
    		vm = new VM_VMWare(mission,env);
		} catch (Exception e) {
			return null;
		}
    	if(vm.rc==null)	return null;
    	return vm;
    }

	public static void main(String argv[]) throws Exception {
		SystemEnv env = new SystemEnv("system.conf");
		MissionServer ms = new MissionServer(env);
		ms.start();
		List<Mission> misList = Mission.missionLoader(argv[0]);
		System.out.println(argv[0]);
		ms.update(misList);
	}

	/* function：获取虚拟机状态
    * Parameters:
    *  int：虚拟机id标识
    * Return:
    *  int：0代表关机、1代表开机、-1代表失败
    */
	private int getPowerState(int vmid){
		//RemoteControl rc = RemoteControl.getInstanceL(ip, user);
		RemoteControl.Connect co = null;
		String cmd = String.format("%s vmsvc/power.getstate %d", vm_cmd, vmid);
		String line = null;
		try {
			co = rc.open(cmd);
			line = co.readLine();
			line = co.readLine();
			if(line.equals("Powered on")){
				return 1;
			}else if(line.equals("Powered off")){
				return 0;
			}else{
				throw new Exception();
			}
		} catch (Exception e) {
			return -1;
		} finally {
			co.close();
		}

	}

	/* function：备份虚拟机
    * Parameters:
    *  int：虚拟机id标识
    *  boolean：是否保存运行信息
    *  dest：目标路径
    * Return:
    *  int：非0代表备份失败
    */
	private int backupi(int vmid, boolean sv_state, String dest){
		RemoteControl.Connect co = null;
		String cmd = null, vmx_dir = null, vm_name = null, line = null;;

		//如果是关机状态则不能备份运行状态
		if(sv_state && getPowerState(vmid)==0){
			logger(2, ERROR_VW_BACKUP_STATE_WHEN_POWER_ON, "VMware错误：尝试对已关机的vm备份运行状态.");
			return 1;
		}

		//规范输入
		if(dest.charAt(dest.length()-1)!='/')	dest+='/';
		//如果目录不存在则创建
		try{
		  	Tools.mkdir(dest, true);
		} catch (Exception e) {
			logger(2, ERROR_VW_INVALID_OPERATION, "无效的操作：无法创建文件夹" + dest);
			return 1;
		}

		//拷贝执行脚本
		try {
			RemoteControl.runLocal(new String[]{"sh","-c",String.format("scp sbin/backupi.sh %s@%s:/tmp/", user, ip)});
			logger(0, 0, "Successfully uploaded files!");
		} catch (Exception e) {
			logger(2, ERROR_VW_INVALID_OPERATION, "无效的操作，上传脚本失败.");
			return 1;
		}

		try{
			//获取vm的name
			vm_name = getVmName(vmid);
			logger(0, 0, vm_name);

			//获取vm的存储目录
			vmx_dir = getVmDir(vmid);
			//logger(0,0, vmx_dir);
			if(vm_name==null || vmx_dir==null)	throw new Exception();
		}catch (Exception e){
			logger(2, ERROR_VW_INVALID_OPERATION, "无效的操作，无法获取客户vm参数.");
			return 1;
		}finally {
			if(co!=null) co.close();
		}

		cmd = String.format("sh /tmp/backupi.sh -i %d -m %d -d %s -n \"%s\"", vmid, sv_state?1:0, vmx_dir, vm_name);

		try {
			co = rc.open(cmd);
			while((line=co.readLine())!=null){
				//两次备份
				if(line.equals("SCDEngine:+")){
					cmd = "scp ";
					while(!( (line=co.readLine()).equals("SCDEngine:-")) ){
						line = line.trim();
						line = line.replaceAll(" ", "\\\\ ");
						cmd += String.format("%s@%s:\"%s/%s\" ", user, ip, vmx_dir, line);
					}
					cmd += dest;
					RemoteControl.runLocal(new String[]{"sh", "-c", cmd});
					if(co==null)	System.out.println("connection is null");
					co.writeLine("go on");
					logger(0, 0, "Successfully copied from remote server.");
				}

				//生成文件用于标识快照名称
				if(line.equals("SCDEngine:snapshot_id1")){
					String ssname = co.readLine();
					ssname+=".scdengine";
					RemoteControl.runLocal(new String[]{"sh", "-c", String.format("touch %s%s", dest, ssname)});
					logger(0, 0, String.format("touch %s%s", dest, ssname));
				}
			}
			//logger(0, 0, "");
		} catch (Exception e) {
			e.printStackTrace();
			logger(2, ERROR_VW_GET_REMOTE_FILE, "无法从服务器下载备份数据.");
			return 1;
		}finally {
			if(co!=null) co.close();
		}
		return 0;
	}

	/* function：恢复虚拟机
    * Parameters:
    *  String：源路径
    *  dest：目标路径
    * Return:
    *  int：非0代表恢复失败
    */
	private int restorei(String src, String dest){
		String cmd = null, vm_cmd = null, line = null, vm_conf = null;
		RemoteControl.Connect co = null, nco = null;
		int nVm_id = -1, ss_id = -1;

		//如果末尾没有'/'则加上'/'
		char last = src.charAt(src.length()-1);
		if(last!='/')	src+='/';
		last = dest.charAt(dest.length()-1);
		if(last!='/')	dest+='/';

		//合理性检测
		if(sanityCheck_restore(src, dest)!=0)	return 1;

		try {
			//上传文件到服务器
			try {
				dest = dest.replaceAll(" ", "\\\\ ");
				RemoteControl.runLocal(new String[]{"sh", "-c", String.format("scp -r %s* %s@%s:\"%s\"", src, user, ip, dest)});
				logger(0, 0, "Successfully uploaded files.");
			} catch (IOException e) {
				throw new SCDException(ERROR_VW_UPLOAD_FAILED, "上传备份数据失败.");
			}

			//注册虚拟机
			vm_conf = getVmConf(dest);
			nVm_id = registerVM(dest + vm_conf);
			System.out.println(dest + vm_conf);
			logger(0, 0, "vmid: " + nVm_id);

			//打开虚拟机恢复
			powerOnVM(nVm_id);
			//应答虚拟机电源
			answerVMware(nVm_id);

			//查找snapshot的id值
			ss_id = getNewistSnapshotId(dest);

			//删除.scdengine文件，以免越来越多
			try {
				cmd = String.format("rm -f `ls %s*.scdengine`", dest);
				co = rc.open(cmd);
				co.waitUntilFinished();
				cmd = String.format("test -e %s*.scdengine; echo $?", dest);
				co = rc.open(cmd);
				line = co.readLine();
				if(line.equals("0")) throw new Exception();
			} catch (Exception e) {
				throw new SCDException(ERROR_VW_INVALID_OPERATION, "无效的操作：无法删除.scdengine文件");
			} finally {
				if(co!=null)	co.close();
			}

			//恢复快照，即恢复到运行状态
			revertSnapshot(nVm_id, ss_id);

			//合并快照，恢复到备份前的状态
			removeSnapshot(nVm_id, ss_id);
		} catch (SCDException e) {
			logger(2, e.getErrorCode(), e.getRawMsg());
			return 1;
		}

		return 0;
	}

	private String getVmConf(String dest) throws SCDException{
		RemoteControl.Connect co = null;
		String line = null;
		try {
			co = rc.open(String.format("cd %s; ls *.vmx", dest));
			line = co.readLine();
			if(line==null) throw new Exception();
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_VMX_N, String.format("找不到指定的vmx文件，请检查\"%s\"路径是否正确.", dest));
		} finally {
			if(co!=null) co.close();
		}
		line = line.trim().replaceAll(" ", "\\\\ ");
		return line;
	}

	/* function：恢复时合理性检验
    * Parameters:
    *  String：源路径
    *  dest：目标路径
    * Return:
    *  int：非0代表校验失败
    */
	private int sanityCheck_restore(String src, String dest){
		RemoteControl.Connect co = null;
		String cmd = null, line = null;
		String[] cmd_ls;
		Process process = null;
		BufferedReader br = null;//new BufferedReader(new InputStreamReader(ps.getInputStream()));
		//检查源路径是否存在vmx文件
		cmd = String.format("ls %s*.vmx > /dev/null 2>&1; echo $?", src);
		cmd_ls = new String[]{"sh","-c",cmd};
		try{
			process = Runtime.getRuntime().exec(cmd_ls);
			process.waitFor();
			br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			line = br.readLine();
			if(!line.equals("0")){
				logger(2, ERROR_VW_VMX_N, String.format("找不到指定的vmx文件，请检查\"%s\"路径是否正确.", src));
				return 1;
			}
		}catch(Exception e){
			logger(2, ERROR_VW_INVALID_OPERATION, "无效的操作：无法检测vmx文件.");
			return 1;
		}

		//检查目的路径的父目录是否存在
		dest = dest.substring(0,dest.length()-1);
		String pdest = null;
		if(dest.lastIndexOf('/')==0)
			pdest="/";
		else if(dest.lastIndexOf('/')<0){
			logger(2, ERROR_VW_INVALID_ARGU, "目的路径参数无效");
			return 1;
		}else	pdest = dest.substring(0, dest.lastIndexOf('/'));
		cmd = String.format("test -e %s; echo $?", pdest);
		try{
			co = rc.open(cmd);
			if(!(line = co.readLine()).equals("0")){
				logger(2, ERROR_VW_SEARCH_FILE, String.format("无法找到指定的父目录：%s", pdest));
				return 1;
			}
			cmd = String.format("test -e %s; echo $?", dest);
			co = rc.open(cmd);
			if(!(line = co.readLine()).equals("0")){
				cmd = String.format("mkdir %s; echo $?", dest);
				co = rc.open(cmd);
				if(!(line = co.readLine()).equals("0")){
					logger(2, ERROR_VW_INVALID_OPERATION, String.format("无效的操作：VMWare无法创建文件夹%s", dest));
					return 1;
				}
			}
		}catch(Exception e){
			logger(2, ERROR_VW_INVALID_OPERATION, "无效的操作：无法检测路径" + dest);
			return 1;
		}finally {
			co.close();
		}

		return 0;
	}

	/*
	 * dLevel:
	 * 	0--info
	 * 	1--debug
	 * 	2--error
	 * 	3--warn
	 */
	private static void logger(int dLevel, int code, String msg){
		switch (dLevel) {
		case 0:
			Date date=new Date();
			DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String time=format.format(date);
			System.out.println(String.format("%s--info: %s", time, msg));
			break;
		case 2:
			System.err.println(String.format("ERROR:SCD-%05d.LEVEL:4.%s", code, msg));
		default:
			break;
		}
	}

	private String getVmName(int vmid) throws Exception{
		RemoteControl.Connect co = null;
		String vm_name = null, cmd = null;
		cmd = String.format("%s vmsvc/getallvms | sed 's/[[:blank:]]\\{3,\\}/   /g' | fgrep \"[\" "
				+ "| fgrep \"vmx-\" | fgrep \".vmx\" | fgrep \"/\" | awk -F'   ' '$1==\"%d\" {print $2}'", vm_cmd, vmid);
		co = rc.open(cmd);
		vm_name = co.readLine();
		co.close();
		return vm_name;
	}

	/* function：获取EXS的CLI
    * Parameters:
    *  int：vm的id标识
    * Return:
    *  string：获取vm所在的目录路径
    */
	private String getVmDir(int vm_id) throws Exception{
		RemoteControl.Connect co = null;
		String cmd = null, vmx_dir = null, vmx_conf = null, vmfs_volume = null;

		String vm_list_cmd = vm_cmd + " vmsvc/getallvms | sed 's/[[:blank:]]\\{3,\\}/   /g' | fgrep \"[\" | fgrep \"vmx-\" | fgrep \".vmx\" | fgrep \"/\" "
				+ "| awk -F'   ' '{print \"\\\"\"$1\"\\\";\\\"\"$2\"\\\";\\\"\"$3\"\\\"\"}' |  sed 's/\\] /\\]\\\";\\\"/g'";
		cmd = String.format(vm_list_cmd
				+ "| awk -F \";\" '$1==\"\\\"%d\\\"\" {print $3}' | sed 's/\\[//;s/\\]//;s/\"//g'", vm_id);
		co = rc.open(cmd);
		vmfs_volume = co.readLine();
		co.close();

		cmd = String.format(vm_list_cmd
					+ "| awk -F \";\" '$1==\"\\\"%d\\\"\" {print $4}' | sed 's/\\[//;s/\\]//;s/\"//g'", vm_id);
		co = rc.open(cmd);
		vmx_conf = co.readLine();
		co.close();

		vmx_conf = vmx_conf.replaceAll(" ", "\\\\ ");
		vmfs_volume = vmfs_volume.replaceAll(" ", "\\\\ ");
		cmd = String.format("dirname /vmfs/volumes/%s/%s", vmfs_volume, vmx_conf);

		co = rc.open(cmd);
		vmx_dir = co.readLine();
		co.close();
		vmx_dir = vmx_dir.replaceAll(" ", "\\ ");
		return vmx_dir;

	}

   /* function：获取EXS的CLI
    * Parameters:
    *  none
    * Return:
    *  一个包含当前版本EXS CLI字符串
    */
	private String getVmCmd(){
		if(rc!=null){
			try{
				String cmd = "test -f /usr/bin/vmware-vim-cmd; echo $?";
				RemoteControl.Connect co = rc.open(cmd);
				String line = co.readLine();
				if("0".equals(line))
					return "vmware-vim-cmd";

				cmd = "test -f /bin/vim-cmd; echo $?";
				co = rc.open(cmd);
				line = co.readLine();
				if("0".equals(line))
					return "vim-cmd";
			}catch (Exception e){
				System.out.println(e);
			}
		}
		return null;
	}

    /* function：获取指定vmware上所有虚拟机的信息
    * Parameters:
    *  ip: Vmware ip
    *  user: Vmware user
    * Return:
    *  一个包含所有虚拟机信息的Json字符串
    */
	public static String getallvms(String ip,String user) {
		RemoteControl rc=RemoteControl.getInstanceL(ip, user);
		if(rc==null) return "Error: Connect to VMWare";
		int i=0;
		StringBuffer sbf=new StringBuffer();
		sbf.append("[");
		RemoteControl.Connect co=null;
		try {
			co=rc.open("vim-cmd vmsvc/getallvms");

			while (true) {
				String line = co.readLine();
				if (line == null)
					break;
			//	System.out.println(line);
				int aa = line.indexOf("[");
				int bb = line.indexOf(".vmx");
				if (aa < 0 || bb < 0)
					continue;
				int blank=line.indexOf(' ');
				if(blank<0) continue;
				String back=line.substring(blank).trim();
				int brk=back.indexOf("   [");
				if(brk<0) continue;
				String name=back.substring(0,brk).trim();
				name=decodeTarget(name);
				back=back.substring(brk);
				int vmx=back.indexOf("vmx  ");
				if(vmx<0) continue;
				String vmxpath=back.substring(0,vmx+3);
				back=back.substring(vmx+3).trim();
				String backs[]=back.split("\\s+", 3);
			//	System.out.println(line.substring(0,blank)+" "+name.replaceAll("\"", "\\\\\"")+" "+vmxpath+" "+backs[0]+" "+backs[1]);

				String json=String.format("{\"Vmid\":\"%s\",\"Name\":\"%s\",\"File\":\"%s\",\"Guest Os\":\"%s\",\"Versoin\":\"%s\",\"Annotation\":",
						line.substring(0,blank),
						Tools.addJsonEscapeChar(name),
						Tools.addJsonEscapeChar(vmxpath.trim()),
						backs[0],
						backs[1]);


				if(i>0) sbf.append(",\n");
				else sbf.append("\n");
				i++;

				if(backs.length>2) json=json+"\""+Tools.addJsonEscapeChar(backs[2])+"\"}";
				else json=json+"\"\"}";
				sbf.append(json);
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			if(co!=null) co.close();
		}
		sbf.append("\n]");
		return sbf.toString();
	}

	/* function：将虚拟机名字中的'%','/'等特殊字符分别替换成"%25","%2f"
    * Parameters:
    *  vmname0: 虚拟机名
    * Return:
    *  String: 替换后的虚拟机名
    */
	String encodeTarget(String vmname0){
		String vmname=vmname0.replaceAll("%", "%25").replaceAll("/", "%2f").
				replaceAll("\\\\", "%5c");//.replaceAll("\\*","%2a");
		return vmname;
	}
	static String decodeTarget(String vmname0){
		String vmname=vmname0.
			//	replaceAll("%2a","\\*").
				replaceAll("%5c","\\\\" ).
				replaceAll( "%2f","/").
				replaceAll( "%25","%");

		return vmname;
	}

	/* function：关闭电源
    * Parameters:
    *  String vmid: 虚拟机id标识
    * Return:
    *  void
    */
	void powerOff(String vmid) throws SCDException {
		if (vmid == null) {
			System.out.println("Error, not found vmid");
			throw new SCDException(ERROR_VW_VMID);
		}
		String cmd = "vim-cmd vmsvc/power.off " + vmid;
		try{
			rc.run(cmd,this.log);
		}catch(Exception ex){
			throw new SCDException(ERROR_VW_POWEROFF);
		}
	}

   /* function：启动电源
    * Parameters:
    *  String vmid: 虚拟机id标识
    * Return:
    *  void
    */
	void powerOnVM(int vmid) throws SCDException {
		RemoteControl.Connect co = null;
		String line = null;
		try {
			co = rc.open(String.format("%s vmsvc/power.on %d", vm_cmd, vmid));
			line = co.readLine();
			if(line==null)	throw new Exception();
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_POWERON, String.format("无法打开虚拟机电源,vm_id: %d", vmid));
		} finally {
			if(co!=null)	co.close();
		}
	}

	//如果是第一次打开电源，则需要应答虚拟机
	void answerVMware(int vmid) throws SCDException {
		RemoteControl.Connect co = null;
		String line = null;
		RemoteControl nrc = RemoteControl.getInstanceL(ip, user);
		try {
			Thread.sleep(10000);
			co = nrc.open(String.format("%s vmsvc/message %d", vm_cmd, vmid));
			line = co.readLine();
			if(line.indexOf("Virtual machine message")>=0){
				String order_msg = line.substring("Virtual machine message".length() + 1, line.length()-1);
				rc.open(String.format("%s vmsvc/message %d %s 2", vm_cmd, vmid, order_msg));
			}else{
				throw new Exception();
			}
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_POWERON, String.format("无法打开虚拟机电源,vm_id: %d", vmid));
		} finally {
			if(co!=null)	co.close();
		}

	}

	/* function：获取虚拟机最新快照的id标识
    * Parameters:
    *  String vmid: 虚拟机路径
    * Return:
    *  int: 虚拟机最新快照的id标识
    */
	private int getNewistSnapshotId(String dest) throws SCDException{
		RemoteControl.Connect co = null;
		String line = null;
		try {
			co = rc.open(String.format("cd %s; ls *.scdengine", dest));
			line = co.readLine();
			if(line==null)	throw new Exception();
			line = line.substring(0, line.indexOf(".scdengine"));
			return Integer.parseInt(line);
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_INVALID_OPERATION, "无效的操作：无法查看snapshot_id");
		} finally {
			if(co!=null) co.close();
		}
	}

	/* function：注销虚拟机
    * Parameters:
    *  String vmid: 虚拟机id标识
    * Return:
    *  void
    */
	void removeVM(String vmid) throws SCDException {
		if (vmid == null) {
			System.out.println("Error, not found vmid");
			throw new SCDException(ERROR_VW_VMID);
		}
		String cmd = "vim-cmd vmsvc/power.off " + vmid
				+ ";vim-cmd vmsvc/unregister " + vmid;
		try{
			rc.run(cmd,this.log);
		}catch(Exception ex){
			throw new SCDException(ERROR_VW_RM_VM);
		}
	}

	/* function：获取当前虚拟机文件存放的目录列表
    * Parameters:
    *  String vmid: 虚拟机id标识
    *  String snapshotname: 需要被移除的快照名
    * Return:
    *  none
    */
	void removeSnapshot(int vmid, int ss_id) throws SCDException {
		RemoteControl.Connect co = null;
		try{
			String cmd = String.format("%s vmsvc/snapshot.remove %d %d > /dev/null 2>&1; echo $?", vm_cmd, vmid, ss_id);
			co = rc.open(cmd);
			String line = co.readLine();
			if(line==null || !line.equals("0"))	throw new Exception();
		} catch (Exception e){
			throw new SCDException(ERROR_VW_RM_SNAPSHOT, "无法移除快照，snapshot_id： " + ss_id);
		} finally {
			if(co!=null)	co.close();
		}
	}

	/* function：将指定的虚拟机恢复到指定快照
    * Parameters:
    *  String vmid: 虚拟机id标识
    *  String snapshotname: 被恢复的快照点的快照名（为null时默认恢复到最新）
    * Return:
    *  none
    */
	void revertSnapshot(int vmid, int ss_id) throws SCDException {
		RemoteControl.Connect co = null;
		try {
			String cmd = String.format("%s vmsvc/snapshot.revert %d %d 0 > /dev/null 2>&1; echo $?", vm_cmd, vmid, ss_id);
			co = rc.open(cmd);
			String line = co.readLine();
			if(line==null || !line.equals("0"))	throw new Exception();
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_RV_SNAPSHOT, "无法恢复快照，snapshot_id： " + ss_id);
		} finally {
			if(co!=null)	co.close();
		}
	}

	/* function：注册虚拟机
    * Parameters:
    *  String name: 虚拟机注册名
    *  String regpath: vmx文件存放位置
    *  String vmxname: 注册时需要的.vmx文件，存放在regpath目录下
    * Return:
    *  none
    */
	int registerVM(String regpath) throws SCDException {
		int nVm_id = -1;
		RemoteControl.Connect co = null;
		try {
			System.out.println(String.format("%s solo/registervm %s", vm_cmd, regpath));
			co = rc.open(String.format("%s solo/registervm %s", vm_cmd, regpath));
			String vid_str = co.readLine();
			if(vid_str!=null)	nVm_id = Integer.valueOf(vid_str);
			else	throw new Exception();
		} catch (Exception e) {
			throw new SCDException(ERROR_VW_REGISTER_VM, "无法注册虚拟机：" + regpath);
		} finally {
			if(co!=null) co.close();
		}
		return nVm_id;
	}

	/* function: 获取文件存放路径
    * Parameters:
    *  String target: 目标虚拟机名
    *  String ip: 要存放的节点的ip
    * Return:
    *  String: 文件存放路径
    */
	protected String getSavePath(String target, String ip){
		try{
			String name=getSHash();//Tools.hash(this.mis.get("sip")+this.mis.get("starget"));
			String time=this.mis.get("bktime");
			return this.ldm.getSavePath(name, time);
		}catch(Exception ex){
			return null;
		}
	}

	private String LoadDir() throws SCDException {
		String name=getSHash();//Tools.hash(this.mis.get("sip")+this.mis.get("starget"));
		try{
			return this.ldm.LoadDir(name,this.mis.get("bktime"));
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	protected void runBackup() throws SCDException {
		String name = getHash();
		String tmp0 = ldm.getTmp0Path(), tmp1 = ldm.getTmp1Path();
		String src = tmp0 + name + "/" + tstart_;
		String des = tmp1 + name + "/" + tstart_;
		if(backupi(Integer.valueOf(mis.get("vmid")), mis.get("sv_state").equals("true")?true:false, src)==0){
			try {
				this.ldm.getDir_VMWare(src, des, name, rc.getUname(), tstart_);;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void timeRecover(String bktime) throws SCDException {
		String dir = LoadDir();
		String dest = String.format("%s/%s_%s", getRegPath(), mis.get("bktime"), mis.get("target"));
		dest = dest.substring(0, dest.lastIndexOf('#'));
		System.out.println(dest);
		restorei(dir, dest);
	}

    /* function：获取系统默认的存储路径，用于存放新的虚拟机文件
    * Parameters:
    *  none
    * Return:
    *  void
    */
	private String getRegPath() {
		RemoteControl.Connect co=null;
		try {
			co=rc.open("vim-cmd solo/environment | grep /vmfs/volumes");
			while (true) {
				String line = co.readLine();
				if (line == null)
					break;
				String path = line.split("\\\"")[1];
				return path;
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			if(co!=null) co.close();
		}
		return null;
	}
}
