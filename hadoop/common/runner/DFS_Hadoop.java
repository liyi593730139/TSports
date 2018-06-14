//////DFS_Hadoop.java
package common.runner;

import common.Mission;
import common.MissionRunnerBase;
import common.MissionServer;
import common.SystemEnv;
import common.Timer;
import common.Tools;
import common.monitor.MonitorAffair;

public class DFS_Hadoop extends MissionRunnerBase {
	String lastClearTime="";
	private DFS_Hadoop(Mission mission, SystemEnv env) {
		super(mission, env, false);
		//add monitors
		monitorAffairs = new MonitorAffair[0];
		// TODO Auto-generated constructor stub
	}
	public static DFS_Hadoop getInstance(SystemEnv env) {
	//	if(Tools.timeInSec(env.get("dfsPeriod",""), 0)==0) return null;
	//	System.out.println(Tools.timeInSec(env.get("dfsPeriod",""), 0));
		Mission mis=new Mission();
		mis.put("bkperiod", env.get("dfsPeriod","2000 d"));
		mis.put("starttime", env.get("dfsStartTime",null));
		mis.put("action","backup");
		mis.addType("dfs:hadoop:0");
		DFS_Hadoop dfs=new DFS_Hadoop(mis,env);
		return dfs;
	}

	@Override
	public void runBackup() throws Exception{
		String curTime=tstart_.substring(0, 8);
		if(curTime.equals(lastClearTime)==false){
			ldm.move4to5();
			ldm.move3to4();
			lastClearTime=curTime;
			System.out.println("remove");
		}
		System.out.println("backup to dfs @ "+tstart_);
	//	tstart_.substring(0, 8);
	//
		this.ldm.toDFS(tstart_);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SystemEnv env = new SystemEnv("system.conf");
		env.print();
		MissionServer ms=new MissionServer(env);
		ms.start();
	}

}
