package common;

/**
 *  FileName: Mail.java
 *  Description: 发送邮件的操作封装
*/

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.Transport;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;

/**
 * MailInfo 是 对发送邮件需要的必要信息的封装
*/
class MailInfo {
	/*
	*	用户名
	*/
	String user;

	/*
	*	密码
	*/
	String password;

	/*
	* 接收邮件的pop3Receiver
	*/
	String pop3Receiver;

	/*
	* 发送邮件的smtpSender
	*/
	String smtpSender;

	public String getUser(){
		return user;
	}

	public void setUser(String user){
		this.user = user;
	}

	public String getPassword(){
		return password;
	}

	public void setPassword(String password){
		this.password = password;
	}

	public String getSmtpSender(){
		return smtpSender;
	}

	public void setSmtpSender(String smtpSender){
		this.smtpSender = smtpSender;
	}

	public String getPop3Receiver(){
		return pop3Receiver;
	}

	public void setPop3Receiver(String pop3Receiver){
		this.pop3Receiver = pop3Receiver;
	}

	/**
    * 类的构造函数
    * @param user: 用户，以该用户的名义发送邮件
    * @param password: 帐号对应的密码
    * @param smtpSender: 发邮件所需要的服务器
    */
	public MailInfo(String user, String password, String smtpSender){
		this.user = user;
		this.password = password;
		this.smtpSender = smtpSender;
	}

	/**
    * 类的构造函数
    * @param user: 用户，以该用户的名义发送邮件
    * @param password: 帐号对应的密码
    * @param smtpSender: 发邮件所需要的服务器
    * @param pop3Receiver: 接收方的服务器
    */
	public MailInfo(String user, String password, String smtpSender, String pop3Receiver) {
		this.user = user;
		this.password = password;
		this.smtpSender = smtpSender;
		this.pop3Receiver = pop3Receiver;
	}

}

/**
 * Mail 封装了发送邮件所需要的所有操作
*/
public class Mail {
	/*
	* 包含发送邮件所需的信息
	*/
	MailInfo mailInfo;

	/*
	* 类的默认构造函数
	*/
	public Mail() {
		mailInfo = new MailInfo("test@scdchina.com", "PKUsz123", "smtp.exmail.qq.com");
	}

	/*
	* 类的构造函数
	*/
	public Mail(String user, String password, String smtpSender){
		mailInfo = new MailInfo(user, password, smtpSender);
	}

	/**
	* 修改发送邮件的配置信息
	* @param user: 帐号
	* @param password: 密码
	* @smtpSender: 发送邮件的服务器
	*/
	public void ModifyMailInfo(String user, String password, String smtpSender){
		mailInfo.setUser(user);
		mailInfo.setPassword(password);
		mailInfo.setSmtpSender(smtpSender);
	}

	/**
	* 发送邮件
	* 设置文件的主题，正文，接受者，发送格式之类的，然后发送
	* @param toWhom: 所有接收邮件者的集合，因为可能一封邮件群发给一堆人
	* @param eTitle: 邮件的主题
	* @param eBody: 正文内容
	* @return boolean: true表示发送成功, false表示失败
	*/
	public boolean SendMail(Set<String> toWhom, String eTitle, String eBody) {
		String host = mailInfo.getSmtpSender();

		try {
			//设置smtp属性
			Properties props = new Properties();
            props.setProperty("mail.smtp.host", host);
            props.setProperty("mail.smtp.auth", "true");
            Session session = Session.getInstance(props);
            session.setDebug(false);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailInfo.getUser()));

            //增加接受者
            for (String recipient : toWhom) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            //设置邮件的头文件和发送日期
            message.setSubject(eTitle);
            message.setSentDate(new Date());

            Multipart mul = new MimeMultipart();
            BodyPart mdp = new MimeBodyPart();

            //设置内容和字符集
            mdp.setContent(eBody, "text/html;charset=utf-8");
            mul.addBodyPart(mdp);
            message.setContent(mul);
            message.saveChanges();

            Transport transport = session.getTransport("smtp");

            /*
            * 使用邮箱的用户名和密码连上邮件服务器，发送邮件时，发件人需要提交邮箱的用户名和密码给smtp服务器，
            * 用户名和密码都通过验证之后才能够正常发送邮件给收件人。
            */
            transport.connect(host, mailInfo.getUser(), mailInfo.getPassword());

            //发送邮件
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();  //关闭通道
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
	}

	public static void main(String[] args) {
		 Mail m = new Mail("test@scdchina.com", "PKUsz123","smtp.exmail.qq.com");
		 Set<String> s = new HashSet<String>();
		 s.add("test@scdchina.com");
		 boolean isSuc = m.SendMail(s, "来自God的问候", "System Error,系统错误");
		 if(isSuc)
			 System.out.println("Everything ok");
	}
}
