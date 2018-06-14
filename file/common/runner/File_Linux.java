package common.runner;

/***  FileName: File_Linux.java
 *  Description: linux系统普通文件备份*/

import java.util.List;
import common.Mission;
import common.MissionRunnerBase;
import common.MissionServer;
import common.SystemEnv;
import common.Timer;
import common.Tools;
import common.monitor.*;
import common.remote.Client;
import common.remote.RemoteControl;

/*** Files_Linux 是 linux系统的普通文件进行备份和恢复操作进行封装*/

public class Files_Linux extends MissionRunnerBase {
	/*** 构造函数
	* @param mission: 包含当前任务必须的信息
	* @param env: 环境配置信息
	*/
	private Files_Linux(Mission mission, SystemEnv env) {
		super(mission, env, false);
		enableSuspend = true;
		rc = RemoteControl.getInstanceW(this.ip, this.user);
		Client c = rc.getWClient();
		//add monitor
		monitorAffairs = new MonitorAffair[3];
		monitorAffairs[0] = new MonitorAffair("mem", 120, new MemRemoteMonitor4Linux(mis.getId(), c));
		monitorAffairs[1] = new MonitorAffair("cpu", 120, new CPURemoteMonitor4Linux(mis.getId(), c));
		monitorAffairs[2] = new MonitorAffair("disk", 120, new DiskRemoteMonitor4Linux(mis.getId(), c));
	}

	/*** 静态成员函数，返回一个File_Linux的实例
	* @param mission: 包含当前任务必须的信息
	* @param env: 环境配置信息
	* @return Files_Linux类型：实例
	*/
	public static Files_Linux getInstance(Mission mission, SystemEnv env) {
		Files_Linux fw = new Files_Linux(mission, env);
		if(fw.rc == null) 
			return null;
		return fw;
	}
	
	/*** 执行Linux目录的备份
	* 调用getDirWithSync执行增量备份
	* rc : remoteControler
	* getHash() : taskID
	* mis.get("target") : the directory need to backup
	* tstart_ : task start time or current time
	* mis.get("grp"): 获取本任务所属的组别
	*/
	protected void runBackup() throws Exception {
		String longFile = this.mis.get("target");
		String[] longFiles = longFile.split("dmacelinkto");
		if(longFiles.length == 1){
			this.log.debug("LinuxFileBackup " + tstart_ + " : from " + ip + ":" 
				+ longFile + " ID-" + getHash());
			this.ldm.getDirWithSync(rc, this.mis.getId(), getHash(), longFile, this.tstart_, this.mis.get("grp"), this);
			this.counter++;
		} else {
			for(int i = 1; i < longFiles.length; i++) {
				this.log.debug("LinuxFileBackup " + tstart_ + " : from " + ip + ":"
					+ longFiles[i] + " ID-" + getHash(longFiles[i]) );
				this.ldm.getDirWithSync(rc, this.mis.getId(), getHash(longFiles[i]), longFiles[i], this.tstart_, this.mis.get("grp"), this);
				this.counter++;
			}}}
	
	/*** 把数据从master传送到HDFS
	* @param logId: 写日志需要的日志ID
	*/
	protected void toDFS(String logId) throws Exception {
		this.ldm.toDFS(logId);
	}
	
 	/*** 将数据恢复到某个时间点
	* 调用putDirWithSync将客户端目录恢复到某个时间节点
	* @param bktime: 所需要恢复到的时间点
 	* rc : remoteControler
 	* getSHash() : sourceTaskID
 	* mis.get("target") : recoverTarget
 	* mis.get("starget") : source origin remote target
 	* log : used by rc
 	 */
	protected void timeRecover(String bktime) throws Exception {
		String stargetFile = this.mis.get("starget");
		String targetFile = this.mis.get("target");
		String[] stargetFiles = stargetFile.split("dmacelinkto");
		String[] targetFiles = targetFile.split("dmacelinkto");
		if(stargetFiles.length == 1) {
			this.log.debug("LinuxFileRecover from "+ ip +":"+stargetFile
					+ " ID-" + getSHash() + " to " + mis.get("target") );
			this.ldm.putDirWithSync(rc, getSHash(), targetFile,  bktime, log);
		}else{
			for(int i = 1; i < stargetFiles.length; i++){
				this.log.debug("LinuxFileRecover from "+ ip +":"+stargetFiles[i]
						+ " ID-" + getSHash(stargetFiles[i]) + " to " + mis.get("target") );
				this.ldm.putDirWithSync(rc, getSHash(stargetFiles[i]), targetFiles[i],  bktime, log);
			}
		}}

	/*** 对正在进行的作业进行暂停，删除，继续等操作
	* @param newStatus: 对应的状态，分别为CONTINUE, SUSUPEND, DROP三个状态之一*/
	protected void setSuspend(int newStatus) {
		switch(newStatus) {
		case CONTINUE:missionContinue(); break;
		case SUSPEND: missionStop();break;
		case DROP: missionCancel();break;
		default:
		}
	}

	protected void missionContinue() {
		System.out.println("Call setTransferContinue");
		rc.setTransferContinue();
	}
	protected void missionStop() {
		System.out.println("Call setTransferStop");
		rc.setTransferStop();
	}
	protected void missionCancel() {
		rc.setTransferCancel();
	}
	public static void main(String args[]) throws Exception {
		SystemEnv env = new SystemEnv("system.conf");
		MissionServer ms = new MissionServer(env);
		ms.start();
		List<Mission> misList = Mission.missionLoader("/var/www/html/DMace/conf/rec_mission_dir_linux_scd.ini");
		ms.update(misList);
	}

}