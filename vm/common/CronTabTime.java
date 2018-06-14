package common;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CronTabTime {
	Set<Integer> min=new HashSet<Integer>(60);
	Set<Integer> hour=new HashSet<Integer>(25);
	Set<Integer> dayInMonth=new HashSet<Integer>(32);
	Set<Integer> month=new HashSet<Integer>(13);
	Set<Integer> dayInWeek=new HashSet<Integer>(8);

	CronTabTime(String str){
		String timers[]=str.split("\\s+");
		for(int i=0;i<timers.length;i++){
			Set<Integer> current=null;
			switch(i){
			case 0:current=min;break;
			case 1:current=hour;break;
			case 2:current=dayInMonth;break;
			case 3:current=month;break;
			case 4:current=dayInWeek;break;
			}
			String t1[]=timers[i].split(",");
			for(String st:t1){
				String t[]=st.split("/");
				int diver=1;
				if(t.length>1) diver=Integer.valueOf(t[1]);
				int min=0,max=0;
				if("*".equals(t[0])){
					switch(i){
					case 0:max=59;break;
					case 1:max=23;break;
					case 2:max=31;min=1;break;
					case 3:max=12;min=1;break;
					case 4:max=6;break;
					}
				}else{
					String t2[]=t[0].split("-");
					if(t2.length==1) min=max=Integer.valueOf(t2[0]);
					else{
						min=Integer.valueOf(t2[0]);
						max=Integer.valueOf(t2[1]);
					}
				}
				while(min<=max){
					current.add(min);
					min+=diver;
				}
			}
			System.out.println(current);
		}
	}
	boolean match(Calendar c1) {
		if(month.contains(c1.get(Calendar.MONTH) + 1 )==false) return false;
		if(dayInMonth.contains(c1.get(Calendar.DAY_OF_MONTH))==false) return false;
		if(dayInWeek.contains((c1.get(Calendar.DAY_OF_WEEK)+6)%7)==false) return false;
		if(hour.contains(c1.get(Calendar.HOUR_OF_DAY))==false) return false;
		if(min.contains(c1.get(Calendar.MINUTE))==false) return false;

		String tstr = String.format("%04d-%02d-%02d %02d:%02d:%02d",
				c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1,
				c1.get(Calendar.DAY_OF_MONTH), c1.get(Calendar.HOUR_OF_DAY),
				c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));
		System.out.println(tstr);
		return true;
	}
	public static String curTimeStr(long timeStampInSec) {
		Calendar c1 = Calendar.getInstance();
		c1.setTimeInMillis(timeStampInSec * 1000);
		String tstr = String.format("%04d-%02d-%02d %02d:%02d:%02d",
				c1.get(Calendar.YEAR), c1.get(Calendar.MONTH) + 1,
				c1.get(Calendar.DAY_OF_MONTH), c1.get(Calendar.HOUR_OF_DAY),
				c1.get(Calendar.MINUTE), c1.get(Calendar.SECOND));
		return tstr;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CronTabTime ctt=new CronTabTime("*/10,1-9 1-24/2 1-5/1,4,8 */5 5");
		ctt.match(Calendar.getInstance());
	}
}
