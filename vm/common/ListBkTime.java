package common;

import java.util.List;
import common.remote.LocalDirM;

public class ListBkTime {
	public static void main(String argv[]){
		String misFile=//"ini/vmwarebk.ini";
		"ini/some.ini";
		if(argv.length==1) misFile=argv[0];
		else{
			System.out.println("Arguments Error: You should have only one argument, such as");
			System.out.println("java -jar ListBkTime.jar ***.ini");
		}
		SystemEnv env = new SystemEnv("system.conf");
		LocalDirM ldm=LocalDirM.getInstance(env.get("savePath", "data/"));
		List<Mission> misList = Mission.missionLoader(misFile);
		for(Mission m:misList){
			System.out.println(m.toString());
			ldm.list(m.getDataHash());
			System.out.println();
		}
	}
}
