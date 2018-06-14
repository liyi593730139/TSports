package common.monitor;

/**
 *  FileName: AlertImpl.java
 *  Description: Implementation of Alert
*/

import common.Mail;
import common.MysqlCtrl;
import common.SystemEnv;
import common.Tools;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * AlertImpl 是对 Alert的一种实现
 * 分为taskAlert, systemAlert, monitorAlert三种
*/
public class AlertImpl implements  Alert{
    /*
    * 类的一个实例，单例模式
    */
    private static AlertImpl scdAlert_;

    /*
    * 数据库操作对象
    */
    private static MysqlCtrl mysql_;

    /*
    * 邮箱操作对象
    */
    private static Mail mail_;

    /*
    * 单例模式，返回单例
    */
	public static AlertImpl getInstance(){
		if (scdAlert_ == null) {
			scdAlert_ = new AlertImpl();
		}
		return scdAlert_;
    }

    /**
    * 实例化，获取环境配置，配置数据库操作和邮箱接口
    */
    private AlertImpl(){
        SystemEnv systemEnv_ = MysqlCtrl.getEnv();
        mysql_ = MysqlCtrl.instance();
        mail_ = new Mail(systemEnv_.get("email", "test@scdchina.com"),
                systemEnv_.get("emailPwd", "PKUsz123"), systemEnv_.get("smtpServer", "smtp.exmail.qq.com"));
    }

    /**
    * 对任务报警进行处理
    * 通过发送邮件给指定用户和通知前端显示实现任务报警
    * @param taskID: 出错任务对应的任务ID
    * @param type: 出错任务的类型
    * @param errorCode: 出错任务对应的错误码
    * @param errorInfo: 显示给用户的出错信息
    */
    public void taskAlert(String taskID, String type, String errorCode, String errorInfo){

        //根据任务ID获取对应用户的组别，然后对整个组别的人都发送邮件
        String sqlStr = String
                .format("select * from mission where hash = '%s'", taskID);
        ResultSet rs = mysql_.execQuery(sqlStr);
        int groupID;
        String clientip, missionName;

        try {
            //获取指定组别所有人的邮箱信息
            rs.next();
            groupID = rs.getInt("grp");
            clientip = rs.getString("clientip");
            missionName = rs.getString("missionname");
            sqlStr = String
                .format("select distinct email from tb_user where grp = '%d'", groupID);
            rs = mysql_.execQuery(sqlStr);
            Set<String> toWhom = new HashSet<String>();
            while(rs.next()) {
                String s = rs.getString("email");
                System.out.println(s);
                if(!s.equals("")){
                    toWhom.add(s);
                }
            }

            //发送邮件
            mail_.SendMail(toWhom, "SCD Backup : Task Error", errorInfo);

            //通过写入数据库的方式通知前端
            sqlStr = String
                .format("insert into `warning` (`hash`,`level`,`time`,`host`,`type`,`name`,`errorcode`,`describe`,`r`, `grp`) values('%s','%s','%s','%s','%s','%s','%s','%s','%s',%d)",
                        taskID, "warn", Tools.curTimeStr(), clientip,
                        type, missionName, errorCode, errorInfo, "0", groupID);
            mysql_.execUpdate(sqlStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * 对系统报警进行处理, 系统报警默认grp = 0
    * 通过发送邮件给指定用户和通知前端显示实现系统报警，系统报警grp=0，不需要先行查询数据库
    * @param type: 出错的类型
    * @param errorCode: 出错对应的错误码
    * @param errorInfo: 显示给用户的出错信息
    */
    public void systemAlert(String type, String errorCode, String errorInfo) {
        //发送邮件
        String sqlStr;
        try {
             //获取grp = 0所有人的邮箱信息
            sqlStr = String
                    .format("select distinct email from tb_user where grp = %d", 0);
            ResultSet rs = mysql_.execQuery(sqlStr);
            Set<String> toWhom = new HashSet<String>();
            while (rs.next()) {
                // 判断是否还有下一个数据
                String s = rs.getString("email");
                System.out.println(s);
                if (!s.equals("")) {
                    toWhom.add(s);
                }
            }

            //发送邮件
            this.mail_.SendMail(toWhom, "SCD Backup : System Error", errorInfo);

            //通过写入数据库的方式通知前端
            sqlStr = String
                    .format("insert into `warning` (`hash`,`level`,`time`,`host`,`type`,`name`,`errorcode`,`describe`,`r`,`grp`) values('%s','%s','%s','%s','%s','%s','%s','%s','%s',%d)",
                            "9527","warn", Tools.curTimeStr(), "127.0.0.1", //use 127.0.0.1 for system ip error
                            type, "System Error", errorCode, errorInfo, "0", 0);
            mysql_.execUpdate(sqlStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
    * 对监控报警进行处理, 监控报警默认grp = 1
    * @param taskID: 出错任务对应的ID
    * @param type: 出错的类型
    * @param errorCode: 出错对应的错误码
    * @param errorInfo: 显示给用户的出错信息
    */
    public void monitorAlert(String taskID, String type, String errorCode ,String errorInfo){
        try {
             //通过写入数据库的方式通知前端
            String sqlStr = String
                    .format("insert into `warning` (`hash`,`level`,`time`,`host`,`type`,`name`,`errorcode`,`describe`,`r`, `grp`) values('%s','%s','%s','%s','%s','%s','%s','%s','%s',%d)",
                            taskID, "warn", Tools.curTimeStr(), "127.0.0.2", //use 127.0.0.2 for monitor ip error
                            type, "Monitor Error", errorCode, errorInfo, "0", 1);
            mysql_.execUpdate(sqlStr);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        AlertImpl test = new AlertImpl();
        test.taskAlert("0", "disk","233", "test");
        test.systemAlert("disk","2333", "test");
    }

}
