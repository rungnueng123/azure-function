package com.streamit.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

public class EmailUtil {

	public static void sendEmail(Session session, String toEmail, String subject, String body, List<String> attachment){
		try
	    {
	      MimeMessage msg = new MimeMessage(session);
	      //set message headers
	      msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
	      msg.addHeader("format", "flowed");
	      msg.addHeader("Content-Transfer-Encoding", "8bit");

	      msg.setFrom(new InternetAddress("stream@host.com"));

	      msg.setReplyTo(InternetAddress.parse(toEmail, false));

	      msg.setSubject(subject, "UTF-8");

	      msg.setSentDate(new Date());

	      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
	      
	      if (checkHaveAttachment(attachment)) {
	    	  BodyPart messageBodyPart = new MimeBodyPart();
	    	  messageBodyPart.setText(body);
	    	  Multipart multipart = new MimeMultipart();
	    	  multipart.addBodyPart(messageBodyPart);
	    	  messageBodyPart = new MimeBodyPart();
		      for (int i = 0; i<attachment.size(); i++) {
		         ByteArrayDataSource byteArrayDataSource = convertUrlToByteArrayDataSource(attachment.get(i));
		         if (byteArrayDataSource != null) {
			         messageBodyPart.setDataHandler(new DataHandler(byteArrayDataSource));
			         messageBodyPart.setFileName(String.valueOf(new Date()));
			         multipart.addBodyPart(messageBodyPart);
		         }
	          }
		      msg.setContent(multipart);
	      } else {
	    	  msg.setText(body, "UTF-8");
	      }
    	  Transport.send(msg);
	    }
	    catch (Exception e) {
	    	e.printStackTrace();
	    }
	}
	
	public static ByteArrayDataSource convertUrlToByteArrayDataSource(String pathUrl) throws Exception {
			URL url = new URL(pathUrl);
			String contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(url.getFile());
			ByteArrayDataSource source;
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			try (InputStream inputStream = url.openStream()) {
	            int n = 0;
	            byte [] buffer = new byte[ 2048 ];
	            while (-1 != (n = inputStream.read(buffer))) {
	                output.write(buffer, 0, n);
	            }
	            output.toByteArray();
				source = new ByteArrayDataSource( output.toByteArray(), contentType );
	        }
		return source;
	}
	
	public static boolean checkHaveAttachment(List<String> attachment) {
		boolean isHaveAttach = false;
		if (attachment != null) {
			if (attachment.size() > 0) {
				isHaveAttach = true;
			}
		}
		return isHaveAttach;
	}
	
}
