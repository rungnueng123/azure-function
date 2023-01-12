package com.streamit;

import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import com.azure.storage.queue.models.QueueItem;
import com.azure.storage.queue.models.QueueStorageException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Function {
	
	@FunctionName("QueueTriggerDemo")
    public void run(
    		@QueueTrigger(name = "message", queueName = "emailcustomqueue", connection = "AzureWebJobsStorage") String message,
        final ExecutionContext context) throws IOException {
		String request;
		Gson gson = new Gson();
		JsonObject convertedObject = gson.fromJson(message, JsonObject.class);
		request = gson.toJson(convertedObject.get("data"));
		ObjectMapper objectMapper = new ObjectMapper();
		EmailDto emailDto = null;
		try {
			emailDto = objectMapper.readValue(request, EmailDto.class);
			sendEmail(emailDto);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}	
    }
	
	public void sendEmail(EmailDto emailDto) {		
	    String smtpHostServer = "waffle.se.scb.co.th";
	    String smtpHostPort = "25";
	    String toEmail = emailDto.getToEmail();
	    String subject = emailDto.getSubject();
	    String emailBody = emailDto.getBody();
	    List<String> attachment = emailDto.getAttachment();
	    
	    Properties props = System.getProperties();
	    props.put("mail.smtp.host", smtpHostServer);
	    props.put("mail.smtp.port", smtpHostPort);

	    Session session = Session.getInstance(props, null);
	    
	    EmailUtil.sendEmail(session, toEmail, subject, emailBody, attachment);
	}
}
