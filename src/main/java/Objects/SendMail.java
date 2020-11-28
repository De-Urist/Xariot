package Objects;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail implements Runnable{

	String mailFrom = "";
	String pass = "";
	String mailTo = "";
	String project = "";
	String username = "";
	String right = "";
	String modRight = "";
	String inviteId = "";
	
	public SendMail(String mailFrom, String pass, String mailTo, String project, String username, String right, String inviteId) {
		this.mailFrom = mailFrom;
		this.pass = pass;
		this.mailTo = mailTo;
		this.project = project;
		this.username = username;
		this.right = right;
		this.inviteId = inviteId;
	}
	
	public void run(){	
		//final String username = "ghoplite45@gmail.com";
		//final String password = "xmzqxgniqlnqtzqe";
		if(right.equals("1")) {
			modRight = "Read and write";
		}
		else {
			modRight = "Read only";
		}
		
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailFrom, pass);
			}
		  });
		
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailFrom));
			message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(mailTo));
			message.setSubject("System invite.");
			message.setText("You have been invited by the master user,"
				+ "\n\n in a project: " + project + ", "
				+ "\n\n with access right: " + modRight + ", "
				+ "\n\n as user: " + username +" ."
				+ "\n\n To accept the invitation click the following link once: "
				+ "\n\n http://localhost:8080/inv?dec=Accept&id=" + inviteId
				+ "\n\n To decline the invitation click the following link once: "
				+ "\n\n http://localhost:8080/inv?dec=Decline&id=" + inviteId);

			Transport.send(message);
			System.out.println("Email sent to: " + mailTo);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}