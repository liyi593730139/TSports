package common;
import common.runner.VM_VMWare;
public class AgentInfo {
	public static String getList(String name,String ip,String osuser){
		if(name==null) return "Error: name is null !";
		if(name.startsWith("VM:VMWare")){
			return VM_VMWare.getallvms(ip, osuser);
		}
		return "Error: name is invalid";
	}
	public static String getDBList(String name,String ip,String osuser,String dbuser,String dbpwd){
		return "Error: ";
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String str=getList("VM:VMWare","219.223.198.190","root");
		System.out.println(str);
	}

}
