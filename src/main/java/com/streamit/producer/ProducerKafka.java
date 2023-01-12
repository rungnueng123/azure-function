package com.streamit.producer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import com.streamit.Function;

public class ProducerKafka {
	
private static final Logger log;
	
	static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-7s] %5$s %n");
        log =Logger.getLogger(Function.class.getName());
    }

//	String bootstrapServers = "pii-kaf01u.se.scb.co.th:9093";
	String bootstrapServers = "192.168.20.224:9092";
	String groupId = "ap2343-merchant-dev";

	public Properties getProducerKafka() {
		// create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(CommonClientConfigs.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "100");
        return properties;
	}
	
	public Map<String, Object> produceLogKafka(KafkaProducer<String, String> producer, String data) {
		Map<String, Object> result 	= new HashMap<String, Object>();
		String status = "99";
		String message = "";
		log.info("Start produce log");
	    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(groupId, data);
	    try {
	    	RecordMetadata metadata = producer.send(producerRecord).get();
	    	status = "00";
		} catch (Exception  e) {
			e.printStackTrace();
			message = e.getMessage();
		}
	    log.info("End produce log");
	    result.put("status", status);
		result.put("message", message);
	    return result;
	}

}
