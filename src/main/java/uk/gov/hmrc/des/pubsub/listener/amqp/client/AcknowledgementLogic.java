package uk.gov.hmrc.des.pubsub.listener.amqp.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.GenericListenerException;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

@Service
public class AcknowledgementLogic {
	
	
	@Value("${uk.gov.hmrc.mdms.deadLetterExchange}")	
	private String deadLetterExchange;
	
	@Value("${uk.gov.hmrc.mdms.deadLetterRoutingKey}")	
	private String deadLetterRoutingkey;
 
	public void ackLogic(Message message, Status HTTP, Channel channel) 
			throws GenericListenerException, IOException, Exception
	{	
		
		//Check Status for correct acknowledgement
 		LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK",Status.DIAGNOSTIC.getCode()).debug("{}, Reading header to process response to correct queue", Status.DIAGNOSTIC.getDescription());		
 		switch (HTTP) {
		
		case SUCCESS:
			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_SUCCESS",HTTP.getCode()).info("{}, Sending successful baiscAck to RabbitMQ", HTTP.getDescription());	
 			channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); 
			break;				
		case RETRY:
 			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_RETRY",HTTP.getCode()).info("{}, Connection reset or Timeout calling backend or Backend rejected response, Sending basicReject to RabbitMQ to retry", HTTP.getDescription());
	        channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
	        break;	  
		case DLQ:
			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_DLQ",HTTP.getCode()).info("{}, Error response received, Sending basicAck to RabbitMQ", HTTP.getDescription());
			AMQP.BasicProperties.Builder basicPropertiesDlq = buildMessageHeader(message);
			channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
			System.out.println("ACK send!!");
			channel.basicPublish(deadLetterExchange, deadLetterRoutingkey, basicPropertiesDlq.build(), message.getBody());
			System.out.println("publish DELETE QUEUE");
            break;
		case FISNISHED_RETRIES:
			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_DLQ",HTTP.getCode()).info("{}, Sending basicAck to RabbitMQ", HTTP.getDescription());
			AMQP.BasicProperties.Builder basicProperties = buildMessageHeader(message);
         	channel.basicPublish(deadLetterExchange, deadLetterRoutingkey, basicProperties.build(), message.getBody());        		
        	channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            break;
		case UNKNOWN:
			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_DLQ",HTTP.getCode()).info("{}, Error response received, Sending basicAck to RabbitMQ", HTTP.getDescription());
			AMQP.BasicProperties.Builder basicPropertiesUnknown = buildMessageHeader(message);
         	channel.basicPublish(deadLetterExchange, deadLetterRoutingkey, basicPropertiesUnknown.build(), message.getBody());        		
        	channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            break;
 		default:
			LoggerManager.getLogger(AcknowledgementLogic.class,"CHECK_HTTP_STATUS_ACK_DEFAULT",Status.UNKNOWN.getCode()).info("{}, Sending basicNack to RabbitMQ", Status.UNKNOWN.getDescription());	
			channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
			break;
		}
	}
	
	/**
	 * buildMessageHeader method sets the header values into MessageProperties.
	 * @param message
	 * @return
	 */
	private AMQP.BasicProperties.Builder buildMessageHeader(Message message) {
		
		AMQP.BasicProperties.Builder basicProperties = new AMQP.BasicProperties.Builder();		
		Map<String, Object> headerObject = message.getMessageProperties().getHeaders();			
		Map<String, Object> messageProps = new HashMap<String, Object>(); 
		for (Map.Entry<String, Object> entry : headerObject.entrySet()) {
		    String key = entry.getKey();
		    Object value = entry.getValue();
		    messageProps.put(key,value);
		}
		messageProps.put("message_id", message.getMessageProperties().getMessageId());
		basicProperties.contentType(message.getMessageProperties().getContentType()).
		        deliveryMode(message.getMessageProperties().getReceivedDeliveryMode().name().equalsIgnoreCase("PERSISTENT") ?  2 : 1).
		        priority(message.getMessageProperties().getPriority()).headers(messageProps);
		return basicProperties;
	}	
}
