package com.streamit;

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
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import com.streamit.config.DataSourceConfig;
import com.streamit.dto.email.EmailDto;
import com.streamit.dto.log.InquiryLogDto;
import com.streamit.dto.log.MessageLogDto;
import com.streamit.dto.log.ResponseBodyDto;
import com.streamit.producer.ProducerKafka;
import com.streamit.utils.EmailUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Session;

import org.apache.kafka.clients.producer.KafkaProducer;

public class Function {
	
	private static final Logger log;
	private static String USER_NAME = "noreply@waffle.se.scb.co.th";
	
	static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-7s] %5$s %n");
        log = Logger.getLogger(Function.class.getName());
    }
	
	@FunctionName("QueueTrigger")
    public void QueueTrigger(
    		@QueueTrigger(name = "message", queueName = "emailcustomqueue", connection = "AzureWebJobsStorage") String message,
        final ExecutionContext context) throws IOException {
		context.getLogger().info("Trigger Queue");
		String request;
		Gson gson = new Gson();
		JsonObject convertedObject = gson.fromJson(message, JsonObject.class);
		request = gson.toJson(convertedObject.get("data"));
		ObjectMapper objectMapper = new ObjectMapper();
		EmailDto emailDto = null;
		try {
			emailDto = objectMapper.readValue(request, EmailDto.class);
			sendEmail(emailDto, context);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}	
    }
	
	@FunctionName("PiiLog")
    public HttpResponseMessage PiiLog(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws Exception {

        //connect kafka
        ProducerKafka producerKafka = new ProducerKafka();
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(producerKafka.getProducerKafka());
        
        //connect db
    	Connection conn = getConnectionDb();
    	//inquiry db
        InquiryLogDto inquiryLogDto = inquiryActivityLog();
        PreparedStatement readStatement = conn.prepareStatement(inquiryLogDto.getSelectListPiiActivityLogInquiryDAO());
        long millis = System.currentTimeMillis();  
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(millis);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        Date dateStart = c.getTime();
        readStatement.setString(1, sdf.format(dateStart) + " 00:00:00");
        readStatement.setString(2, sdf.format(dateStart) + " 23:59:59");
        ResultSet rs = readStatement.executeQuery();
        Boolean executeBatch = true;
        if (!rs.next()) {
        	executeBatch = false;
        }
        List<MessageLogDto> messages = new ArrayList<MessageLogDto>();
        if (executeBatch) {
        	//pattern kafka = AppID | FuncID | EventDate | UserName | IP Address | Search Criteria | Previous Value (update) | New Value (update) | Record Key value (adding and delete)
        	StringBuilder pattern = new StringBuilder();
            pattern.append(rs.getString("APPLICATION_NAME")); pattern.append("|");
            pattern.append(rs.getString("FUNCTION_CODE")); pattern.append("|");
            pattern.append(rs.getString("TRANSACTION_DATE_FORMAT")); pattern.append("|");
            pattern.append(rs.getString("SOURCE_USERNAME")); pattern.append("|");
            pattern.append(rs.getString("SOURCE_ADDRESS")); pattern.append("|");
            pattern.append(rs.getString("SEARCH_CRITERIA")); pattern.append("|");
            pattern.append(rs.getString("CHANGE_FROM")); pattern.append("|");
            pattern.append(rs.getString("CHANGE_TO")); pattern.append("|");
            pattern.append(rs.getString("RECORD_KEY_VALUE"));
            MessageLogDto messageLogDto = new MessageLogDto(
            		rs.getString("ID"),
            		pattern.toString()
            );
            messages.add(messageLogDto);
        	while (rs.next()) {
            	pattern = new StringBuilder();
                pattern.append(rs.getString("APPLICATION_NAME")); pattern.append("|");
                pattern.append(rs.getString("FUNCTION_CODE")); pattern.append("|");
                pattern.append(rs.getString("TRANSACTION_DATE_FORMAT")); pattern.append("|");
                pattern.append(rs.getString("SOURCE_USERNAME")); pattern.append("|");
                pattern.append(rs.getString("SOURCE_ADDRESS")); pattern.append("|");
                pattern.append(rs.getString("SEARCH_CRITERIA")); pattern.append("|");
                pattern.append(rs.getString("CHANGE_FROM")); pattern.append("|");
                pattern.append(rs.getString("CHANGE_TO")); pattern.append("|");
                pattern.append(rs.getString("RECORD_KEY_VALUE"));
                messageLogDto = new MessageLogDto(
                		rs.getString("ID"),
                		pattern.toString()
                );
                messages.add(messageLogDto);
            }
        }

        rs.close();
        if (executeBatch) {
        	for (int i = 0; i < messages.size(); i++) {
        		//update status to pending
		        PreparedStatement updateStatement = conn.prepareStatement(inquiryLogDto.getUpdateStatusPiiActivityLogInquiryDAO());
		        millis=System.currentTimeMillis();  
		        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		        date = new Date(millis);
		        updateStatement.setString(1, sdf.format(date));
		        updateStatement.setString(2, "P"); 
		        updateStatement.setString(3, messages.get(i).getId());
		        updateStatement.executeUpdate();
        	}
        }

        //send massage kafka
        if (executeBatch) {
        	for (int i = 0;i < messages.size();i++) {
        		Map<String, Object> result = producerKafka.produceLogKafka(producer, messages.get(i).getMessage());
        		PreparedStatement updateStatement = conn.prepareStatement(inquiryLogDto.getUpdateStatusPiiActivityLogInquiryDAO());
        		millis=System.currentTimeMillis();  
		        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		        date = new Date(millis); 
        		if (result.get("status").equals("00")) {
        			//update status to success 
			        updateStatement.setString(1, sdf.format(date));
			        updateStatement.setString(2, "S"); 
			        updateStatement.setString(3, messages.get(i).getId());
			        updateStatement.executeUpdate();
        		} else {
        			//update status to fail
        			updateStatement.setString(1, sdf.format(date));
			        updateStatement.setString(2, "F"); 
			        updateStatement.setString(3, messages.get(i).getId());
			        updateStatement.executeUpdate();
        		}
        	}
        }
        producer.close();
        conn.close();
        ResponseBodyDto body = new ResponseBodyDto(
        		"Success",
        		"200"
        );
        return request.createResponseBuilder(HttpStatus.OK).body(body).build();
    }
	
	public void sendEmail(EmailDto emailDto, ExecutionContext context) {
		context.getLogger().info("Send Mail Start");
	    String smtpHostServer = "waffle.se.scb.co.th";
	    String smtpHostPort = "25";
	    String toEmail = emailDto.getToEmail();
	    String subject = emailDto.getSubject();
	    String emailBody = emailDto.getBody();
	    List<String> attachment = emailDto.getAttachment();
	    
	    Properties props = System.getProperties();
	    props.put("mail.smtp.host", smtpHostServer);
	    props.put("mail.smtp.port", smtpHostPort);
	    props.put("mail.debug", "true");
	    Session session = Session.getInstance(props, null);
	    session.setDebug(true);
	    
	    EmailUtil.sendEmail(USER_NAME, context, session, toEmail, subject, emailBody, attachment);
	}
	
	public Connection getConnectionDb() throws Exception {
    	log.info("Connecting to the database");
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        Connection conn = dataSourceConfig.execute();
        log.info("Database connection test: " + conn.getCatalog());
        return conn;
    }
    
    public InquiryLogDto inquiryActivityLog() throws Exception {
    	InputStream in = Function.class.getClassLoader().getResourceAsStream("inquiry-activity-log.json");
        Reader reader = new InputStreamReader(in, "UTF-8");
        return new Gson().fromJson(reader, InquiryLogDto.class);
    }
}
