package common.runner;

/***  FileName: File_Windows.java
 *  Description: windowsϵͳ��ͨ�ļ�����*/

import java.util.List;
import common.Mission;
import common.MissionRunnerBase;
import common.MissionServer;
import common.SystemEnv;
import common.Tools;
import common.remote.Client;
import common.remote.RemoteControl;
import common.monitor.*;

/*** Files_Windows �� windowsϵͳ����ͨ�ļ����б��ݺͻָ��������з�װ*/
public class Files_Windows extends MissionRunnerBase {
	/*** ���캯��
	* @param mission: ������ǰ����������Ϣ
	* @param env: ����������Ϣ*/
	private Files_Windows(Mission mission, SystemEnv env) {
		super(mission, env, false);
		enableSuspend = true;
		rc = RemoteControl.getInstanceW(this.ip, this.user);
		Client c = rc.getWClient();

		//add monitor
		monitorAffairs = new MonitorAffair[3];
		monitorAffairs[0] = new MonitorAffair("mem", 120, new MemRemoteMonitor4Win(mis.getId(), c));
		monitorAffairs[1] = new MonitorAffair("cpu", 120, new CPURemoteMonitor4Win(mis.getId(), c));
		monitorAffairs[2] = new MonitorAffair("disk", 120, new DiskRemoteMonitor4Win(mis.getId(), c));
	}

	/*** ��̬��Ա����������һ��File_Windows��ʵ��
	* @param mission: ������ǰ����������Ϣ
	* @param env: ����������Ϣ
	* @return Files_Windows���ͣ�ʵ��
	*/
	public static Files_Windows getInstance(Mission mission, SystemEnv env) {
		Files_Windows fw = new Files_Windows(mission, env);
		if (fw.rc == null) 
			return null;
		return fw;
	}

	/*** ����WindowsĿ¼�ı���
	* ����getDirWithSyncִ����������
	* rc : remoteControler
	* getHash() : taskID
	* mis.get("target") : the directory need to backup
	* tstart_ : task start time or current time
	* mis.get("grp"): ��ȡ���������������
	*/
	protected void runBackup() throws Exception {
		String longFile = this.mis.get("target");
		String[] longFiles = longFile.split("dmacelinkto");
		if(longFiles.length == 1){
			this.log.debug("LinuxFileBackup " + tstart_ + " : from " + ip +":" 
					+ longFile + " ID-" + getHash() );
			this.ldm.getDirWithSync(rc, this.mis.getId(), getHash(), longFile, this.tstart_, this.mis.get("grp"), this);
		}else{
			for(int i = 1; i < longFiles.length; i++){
				this.log.debug("LinuxFileBackup " + tstart_ + " : from " + ip +":" 
						+ longFiles[i] + " ID-" + getHash(longFiles[i]) );
				this.ldm.getDirWithSync(rc, this.mis.getId(), getHash(longFiles[i]), longFiles[i], this.tstart_, this.mis.get("grp"), this);
			}
		}
	}
	
	/*** �����ݴ�master���͵�HDFS
	* @param logId: д��־��Ҫ����־ID
	*/
	protected void toDFS(String logId) throws Exception {
		this.ldm.toDFS(logId);
	}
	
	/*** �����ݻָ���ĳ��ʱ���
	* ����putDirWithSync���ͻ���Ŀ¼�ָ���ĳ��ʱ��ڵ�
	* @param bktime: ����Ҫ�ָ�����ʱ���
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
			}}}

	/*** �����ڽ��е���ҵ������ͣ��ɾ���������Ȳ���
	* @param newStatus: ��Ӧ��״̬���ֱ�ΪCONTINUE, SUSUPEND, DROP����״̬֮һ
 	 */
	protected void setSuspend(int newStatus) {
		switch (newStatus){
			case CONTINUE:missionContinue(); break;
			case SUSPEND: missionStop();break;
			case DROP: missionCancel();break;
			default:
		}
	}

	protected void missionContinue() {
		rc.setTransferContinue();
	}
	protected void missionStop() {
		rc.setTransferStop();
	}
	protected void missionCancel() {
		rc.setTransferCancel();
	}
	
	public static void main(String args[]) throws Exception {
		SystemEnv env = new SystemEnv("system.conf");
		env.print();
		MissionServer ms = new MissionServer(env);
		ms.start();
		List<Mission> misList = Mission.missionLoader("/var/www/html/DMace/conf/rec_mission_dir_windows_cjw.ini");
		ms.update(misList);
	}
}