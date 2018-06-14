package common.runner;

import java.util.List;
import java.util.Scanner;
import common.Mission;
import common.MissionRunnerBase;
import common.MissionServer;
import common.SystemEnv;
import common.Tools;
import common.monitor.*;
import common.remote.*;
import java.io.File;
import java.io.FileWriter;

public class VM_HyperV extends MissionRunnerBase {
	Client c=null;
	private String cmdpath= "Hyperv";
	String sambapath;
	String sharePath;

	private VM_HyperV(Mission mission, SystemEnv env) throws Exception{
		super(mission, env, false);
		sambapath=env.get("sambePath", ".");
		sharePath=env.get("sharePath", null);//"\\\\219.223.193.88\\share";

		if(sharePath==null) throw new Exception("not found sharePath in system.conf");
		rc=RemoteControl.getInstanceW(this.ip,this.user);
		c=rc.getWClient();//new Client(this.ip,50000);
		if(c==null) throw new Exception("connect HyperV error: "+this.ip);
		c.run(cmdpath+"\\initial.bat");

		monitorAffairs = new MonitorAffair[3];
		monitorAffairs[0] = new MonitorAffair("mem", 120, new MemRemoteMonitor4Win(mis.getId(), c));
		monitorAffairs[1] = new MonitorAffair("cpu", 120, new CPURemoteMonitor4Win(mis.getId(), c));
		monitorAffairs[2] = new MonitorAffair("disk", 120, new DiskRemoteMonitor4Win(mis.getId(), c));
	}

	public static VM_HyperV getInstance(Mission mission, SystemEnv env) {
		try{
			VM_HyperV ow=new VM_HyperV(mission, env);
			return ow;
		}catch(Exception ex){
			System.out.println("error");
			ex.printStackTrace();
		}
		return null;
	}

	String getInner(StringBuffer context,String label){
		int a=context.indexOf("<"+label+">");
		int b=context.indexOf("</"+label+">");
		if(a<0 || b<0 || b<a) return null;
		return context.substring(a+label.length()+2,b).trim();
	}
	String readXml(String path,String label)throws Exception{
		Scanner scan=new Scanner(new File(path));
		StringBuffer sb=new StringBuffer();
		while(scan.hasNext()){
			sb.append(scan.nextLine());
		}
		scan.close();
		return getInner(sb,label);
	}
	String getBktimeFromXml(String path)throws Exception{
		String s=readXml(path,"versionidentifier");
		if(s!=null && s.length()==16){
			try{
				String ss[]=s.replace('/', '-').replace(':', '-').split("-");
				String time=ss[2]+ss[0]+ss[1]+ss[3]+ss[4]+"00";
				return time;
			}catch(Exception ex){
			}
		}
		throw new Exception("Not found valid time, backup error");
	}
	String xmlBkTime(String fx){
		String bt=String.format("%s/%s/%s-%s:%s",
			fx.substring(4, 6),
			fx.substring(6, 8),
			fx.substring(0, 4),
			fx.substring(8, 10),
			fx.substring(10, 12)
			);
		return bt;
	}

	void writeXml(String path,String name,String backuptime,String backuptarget,String canrecover,String snapshotid)throws Exception{
		if(name==null) throw new Exception("invalid name");
		if(backuptime==null) backuptime="testbackuptime";
		if(backuptarget==null) backuptarget="testbackuptarget";
		if(canrecover==null) canrecover="testcanrecover";
		if(snapshotid==null) snapshotid="testsnapshot";
		String text=
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
		"<backupconf>\n"+
		"  <name>"+name+"</name>\n"+
		"  <backuptime>"+backuptime+"</backuptime>\n"+
		"  <backuptarget>"+backuptarget+"</backuptarget>\n"+
		"  <versionidentifier>no backupversion</versionidentifier>\n"+
		"  <canrecover>"+canrecover+"</canrecover>\n"+
		"  <snapshotid>"+snapshotid+"</snapshotid>\n"+
		"</backupconf>";
		File f=new File(path);
		FileWriter fw=new FileWriter(f);
		fw.write(text);
		fw.close();
		f.setWritable(true, false);
		System.out.println(path);
	}

	public static void main(String args[]) throws Exception {

		SystemEnv env = new SystemEnv("system.conf");
		env.print();
		MissionServer ms = new MissionServer(env);
		ms.start();
		List<Mission> misList = Mission.missionLoader("ini/hypervrcv.ini");
	//	misList = Mission.missionLoader("ini/hypervrcv.ini");
		ms.update(misList);
	}
	protected String getSavePath(String target, String ip) {
		if (ip == null)
			ip = this.ip;
		String a = String.format("%s/%s/", this.sambapath, Tools.hash(target+ip.trim()));
		return a.replace("//", "/");
	}
	protected String getSambaPath(String target, String ip) {
		if (ip == null)
			ip = this.ip;
		String a = String.format("%s\\%s\\", sharePath, Tools.hash(target+ip.trim()));
		return a;
	}
	@Override
	protected void runBackup() throws Exception {
		String name=getHash();
		String shareDir=this.sharePath+"\\"+name+"\\";
		String localDir = this.sambapath+"/"+name+"/";
		String target=this.mis.get("target");
		String powershell="powershell -file ";

		String cmd = String.format("%s%s\\backup.ps1 %s %s %s",powershell,this.cmdpath, target, shareDir,this.cmdpath);

		System.out.println(cmd);
		Tools.mkdir(localDir,true);
		Tools.chmod(localDir, "777");
		writeXml(localDir+"backupconf.xml",target,null,null,null,null);
		log.debug(cmd);
		c.run(cmd);
		this.updateToSql(90, -1, -1);
		String bktime=tstart_;//getBktimeFromXml(localDir+"backupconf.xml");//this.readXml(localDir+"backupconf.xml", "versionidentifier");
		ldm.getDir_Samba(localDir, name, bktime);
		System.out.println(bktime);
	}

	@Override
	protected void timeRecover(String bktime) throws Exception {
		String starget=this.mis.get("starget");
		if(starget==null) starget=this.mis.get("target");
		String localDir = getSavePath(starget, this.mis.get("sip"));

		String name=getSHash();//this.ip;//"Anonymous";
		System.out.println(localDir);
		System.out.println(name);
		String sd[]=ldm.LoadDir2(name, bktime,localDir);
		Exception exc=null;
		try {
			bktime = Tools.splitPath(sd[0])[1];
			System.out.println("bktime " + bktime);
			String target=this.mis.get("target");
			String powershell = "powershell -file ";
			String shareDir = getSambaPath(starget, this.mis.get("sip"))+bktime;


			String bktime_xml=getBktimeFromXml(localDir+"/"+bktime+"/backupconf.xml");		//	System.out.println(bktime_xml+" -------------------- "+localDir+"/"+bktime+"/backupconf.xml");
			String cmd = String.format("%s%s\\recover.ps1 %s %s %s %s",powershell,this.cmdpath, target, shareDir,this.cmdpath,
					this.xmlBkTime(bktime_xml));

			System.out.println(cmd);

			//Tools.sleep(30);

			log.debug(cmd);
			this.c.run(cmd);
		} catch (Exception ex) {
			exc = ex;
			ex.printStackTrace();
		}

		ldm.Reget(sd);
		System.out.println("reput");
		if(exc!=null) throw exc;
	}
}
