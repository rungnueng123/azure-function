package com.streamit;

import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.QueueItem;
import com.azure.storage.queue.models.QueueStorageException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueOutput;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query" 
     */
	
	@FunctionName("QueueTriggerDemo")
    public void run(
    		@QueueTrigger(name = "message", queueName = "emailcustomqueue", connection = "AzureWebJobsStorage") String message,
        final ExecutionContext context) {
		String request;
		Gson gson = new Gson();
		JsonObject convertedObject = gson.fromJson(message, JsonObject.class);
		request = gson.toJson(convertedObject.get("data"));
        HttpURLConnection connection = null; 
		
		try {
		    //Create connection
		    URL url = new URL("https://merchantportal-office365la.azurewebsites.net:443/api/office365la/triggers/manual/invoke?api-version=2022-05-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=6bDQNEtmoTwN9_zhV1KI-v6PN6mHTItCW39UdHxGAvw");
		    connection = (HttpURLConnection) url.openConnection();
		    connection.setRequestMethod("POST");
		    connection.setRequestProperty("Content-Type", 
		        "application/json");
		    connection.setUseCaches(false);
		    connection.setDoOutput(true);

		    //Send request
		    DataOutputStream wr = new DataOutputStream (
		        connection.getOutputStream());
		    wr.writeBytes(request);
		    wr.close();

		    //Get Response  
		    InputStream is = connection.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
		    String line;
		    while ((line = rd.readLine()) != null) {
		      response.append(line);
		      response.append('\r');
		    }
		    rd.close();
		  } catch (Exception e) {
		    e.printStackTrace();
		  } finally {
		    if (connection != null) {
		      connection.disconnect();
		    }
		  }
		
		
		
		
		
//		String messageData = gson.toJson(convertedObject.get("data"), JsonObject.class);
//		JsonObject messageObj = gson.fromJson(messageData, JsonObject.class);
//		
//		
//		final String fromEmail = "rungnueng.luangkamchorn@stream.co.th"; //requires valid gmail id
//		final String password = "RuLu4547"; // correct password for gmail id
//		final String toEmail = messageObj.get("to").toString(); // can be any email id 
//		
//		System.out.println("TLSEmail Start");
//		Properties props = new Properties();
//		props.put("mail.smtp.host", "smtp.gmail.com"); //SMTP Host
//		props.put("mail.smtp.port", "587"); //TLS Port
//		props.put("mail.smtp.auth", "true"); //enable authentication
//		props.put("mail.smtp.starttls.enable", "true"); //enable STARTTLS
//		
//                //create Authenticator object to pass in Session.getInstance argument
//		Authenticator auth = new Authenticator() {
//			//override the getPasswordAuthentication method
//			protected PasswordAuthentication getPasswordAuthentication() {
//				return new PasswordAuthentication(fromEmail, password);
//			}
//		};
//		Session session = Session.getInstance(props, auth);
//		
//		EmailUtil.sendEmail(session, toEmail,"TLSEmail Testing Subject", "TLSEmail Testing Body");
		
		

//		String smtpHostServer = "smtp.example.com";
//	    String emailID = messageObj.get("to").toString();
//	    String subject = messageObj.get("subject").toString();
//	    String body = messageObj.get("html").toString();
//	    
//	    Properties props = System.getProperties();
//
//	    props.put("mail.smtp.host", smtpHostServer);
//
//	    Session session = Session.getInstance(props, null);
//	    
//	    EmailUtil.sendEmail(session, emailID,subject, body);
		
		
		
		
    }
}
