package uk.gov.hmrc.des.pubsub.listener.amqp.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.rabbitmq.client.Channel;

import uk.gov.hmrc.des.pubsub.listener.core.config.EventTypeDesMethodMapProperty;
import uk.gov.hmrc.des.pubsub.listener.core.config.EventTypeDesURIMapProperty;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.GenericListenerException;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.InvalidRestRequestException;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.UnknownBackendResponseException;
import uk.gov.hmrc.des.pubsub.listener.core.model.RestRequest;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;
import uk.gov.hmrc.des.pubsub.listener.rest.client.RestClient;

/**
 * RabbitMq listener class to processes messages from the mainQueue. Message is
 * sent to ESB using restclient. Re-processes message based on HTTP response
 * from ESB.
 * 
 * @author RC00269
 */
@Service
public class ListenerMessageReceiver implements RabbitListenerConfigurer {

	private static final String PUB_SUB_LISTENER_ID = "pubSubListenerId";

	private static final String X_DEATH = "x-death";

	private static final String MESSAGE_STATUS_REJECTED = "rejected";

	private static final String MESSAGE_REASON = "reason";

	private static final String API_EVENT_TYPE = "Event-Type";

	@Value("${uk.gov.hmrc.mdms.max.retry}")
	private String maxRetry;

	@Value("${uk.gov.hmrc.mdms.deadLetterExchange}")
	private String deadLetterExchange;

	@Value("${uk.gov.hmrc.mdms.deadLetterRoutingKey}")
	private String deadLetterRoutingkey;

	@Value("${uk.gov.hmrc.mdms.mps}")
	private long messagesPerSecond;

	@Value("${uk.gov.hmrc.mdms.mps.active}")
	private boolean messagesPerSecondActive;

	@Autowired
	RestClient restClient;

	@Autowired
	AcknowledgementLogic acklogic;

	Status statusValue;
	
	@Autowired
	private EventTypeDesURIMapProperty eventTypeDesURI;
	
	@Autowired
	private EventTypeDesMethodMapProperty eventTypeDesMethod;

	int callNum;
	/**
	 * MessaggeConverter to convert message to and from JSON.
	 * 
	 * @return MappingJackson2MessageConverter
	 */
	@Bean
	public MappingJackson2MessageConverter jackson2Converter() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		return converter;
	}

	/**
	 * sets the jackson2Converter as the messageConverter.
	 * 
	 * @return DefaultMessageHandlerMethodFactory
	 */
	@Bean
	public DefaultMessageHandlerMethodFactory myHandlerMethodFactory() {
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setMessageConverter(jackson2Converter());
		return factory;
	}

	/**
	 * Sets the DefaultMessageHandlerMethodFactory.
	 */
	@Override
	public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
		registrar.setMessageHandlerMethodFactory(myHandlerMethodFactory());
	}

	public ListenerMessageReceiver() {
	}

	/**
	 * receiveMessage method listens for messages from the main queue. applies retry
	 * logic with maxRetry set in properties. calls the ESB using rest client.
	 * 
	 * @param message
	 * @param channel
	 * @throws GenericListenerException
	 * @throws IOException
	 * @throws Exception
	 */
	@HystrixCommand(groupKey="pubsub", threadPoolKey="pubsub", commandKey = "pubsub", fallbackMethod = "sendMessageFallback")
	@RabbitListener(id = PUB_SUB_LISTENER_ID, queues = "#{'${uk.gov.hmrc.mdms.workQueueName}'.split(',')}")
	public void receiveMessage(final Message message, Channel channel) throws GenericListenerException, IOException, Exception {
		
		// clear MDC for every message and fill again
		MDC.clear();
		
		//Get first point in time
		/* TODO delete it
		long start = 0, interval = 0; 
		if (messagesPerSecondActive) {
			start = System.currentTimeMillis();
			interval = 1000 / messagesPerSecond;
		}
		*/
		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_CONSUME_TIME", Status.DIAGNOSTIC.getCode()).info("{}, Message Received at {}: ",
				Status.DIAGNOSTIC.getDescription(), messageTime());

		Map<String, Object> headerObject = message.getMessageProperties().getHeaders();

		Map<String, String> headerString = null;
		Long retry = new Long(0);

		if (headerObject.containsKey(X_DEATH)) {
			retry = getRetryCount(headerObject);
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_COUNT_VALUE", Status.DIAGNOSTIC.getCode()).info("{}, Retry count value: {}",
					Status.DIAGNOSTIC.getDescription(), retry);
			headerObject.remove(X_DEATH);
			headerString = parseHeader(headerObject); 
		} else {
			headerString = parseHeader(headerObject);
		}

		int retryCount = retry.intValue();

		// fill values of headers message on MDC (Mapped Diagnostic Context)
		fillMDC(headerString, retry);

		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE", Status.DIAGNOSTIC.getCode()).debug("{}, messagebody retrieved :{}",
				Status.DIAGNOSTIC.getDescription(), new String(message.getBody(), StandardCharsets.UTF_8));

		String requestUri = getDesURI(headerString);		
		String requestMethod = getDesMethod(headerString);
		
		if (StringUtils.isEmpty(requestUri) || StringUtils.isEmpty(requestMethod)) {
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_PROPERTY_ERROR", Status.DIAGNOSTIC.getCode())
				.error("{}, Error in URI/METHOD properties: URI [{}] - METHOD [{}]. Sending to DLQ.", Status.DIAGNOSTIC.getDescription(), requestUri, requestMethod);
			acklogic.ackLogic(message, Status.DLQ, channel);
			return;
		}
		
		// Reached the max retry attempt
		if (retryCount > Integer.parseInt(maxRetry)) {
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_RETRY_COUNT", Status.FISNISHED_RETRIES.getCode())
					.info("{}, Retry Count value [{}] greater than max retry [{}] configured, send DLQ status", Status.FISNISHED_RETRIES.getDescription(), retryCount, maxRetry);
			// publish to dead letter exchange
			acklogic.ackLogic(message, Status.FISNISHED_RETRIES, channel);
			
		} else {
		
			// get bytes from body of message and create String Object to send message by
			// rest client
			RestRequest restRequest = new RestRequest(headerString, new String(message.getBody(), StandardCharsets.UTF_8), requestMethod, requestUri);
			LoggerManager.getLogger(ListenerMessageReceiver.class, "XXXXX").info("CALL REST NUM [{}], channel [{}]",++callNum, channel.getChannelNumber());
			statusValue = callRestClient(restRequest);
			
			/* TODO delete it
			if (messagesPerSecondActive) {
				long intervalDiff = (System.currentTimeMillis()-start);
				long sleepTime = interval - intervalDiff;
				if (sleepTime > 0) {
					Thread.sleep(sleepTime);
				}
			}
			*/
			LoggerManager.getLogger(ListenerMessageReceiver.class, "XXXXX").info("CALL ACK NUM [{}], channel [{}]",callNum, channel.getChannelNumber());
			acklogic.ackLogic(message, statusValue, channel);
			
		}
		
	}
	
	public void sendMessageFallback(final Message message, Channel channel) throws GenericListenerException, IOException, Exception {
		LoggerManager.getLogger(ListenerMessageReceiver.class, "XXXX_FALLBACK").info("CAll [{}], channel [{}]", callNum, channel.getChannelNumber());
		acklogic.ackLogic(message, Status.RETRY, channel);
	}
	
	/**
	 * parseHeader converts the Map headerObject to be used by the RestClient.
	 * 
	 * @param headerObject
	 * @return
	 * @throws GenericListenerException
	 */
	private Map<String, String> parseHeader(Map<String, Object> headerObject) throws GenericListenerException {
		return headerObject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()!= null ? (String) e.getValue() : "undefined"));
	}

	/**
	 * Method for fill MDC map with headers of message
	 * 
	 * @param headers
	 */
	private void fillMDC(Map<String, String> headers, long retry) {
		MDC.clear();
		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS", Status.DIAGNOSTIC.getCode()).debug("{}, Fill Headers: {} into MDC for diagnostic",
				Status.DIAGNOSTIC.getDescription(), headers);
		MDC.setContextMap(headers);
		MDC.put(MDMSConstants.RETRIES, String.valueOf(retry));
		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS_MDC", Status.DIAGNOSTIC.getCode()).debug("{}, MDC Headers: {}", Status.DIAGNOSTIC.getDescription(),
				MDC.getCopyOfContextMap());
	}

	/**
	 * method to call the ESB using spring restTemplate
	 * 
	 * @param restRequest
	 * @return
	 * @throws GenericListenerException
	 * @throws IOException
	 * @throws Exception
	 */
	private Status callRestClient(RestRequest restRequest) throws GenericListenerException, IOException, Exception {
		try {
			statusValue = restClient.sendMessage(restRequest);
		} catch (RestClientException | UnknownBackendResponseException | InvalidRestRequestException e) {
			/*
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_REST_CLIENT_CALL", Status.UNKNOWN.getCode()).error("{}, exception in restclient ",
					Status.UNKNOWN.getDescription(), e);
			*/
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RECIEVE_MESSAGE_REST_CLIENT_CALL", Status.UNKNOWN.getCode()).error("{}, exception in restclient ",
					Status.UNKNOWN.getDescription());
		}
		// The message should be dead lettered in case of unknown/undefined HTTP codes received from Generic Subscriber component/backend
		return Status.fromCode(statusValue!=null ? statusValue.getCode() : Status.DLQ.getCode());
	}

	/**
	 * Return currentDateTime
	 * 
	 * @return
	 */
	private String messageTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return (dateFormat.format(date)); // 2016/11/16 12:08:43
	}

	/**
	 * Method to get the number of time the message is rejected.
	 * 
	 * @param headers
	 * @return
	 */
	private Long getRetryCount(Map<String, Object> headers) {
		@SuppressWarnings("unchecked")
		List<HashMap<String, Object>> xDeath = (List<HashMap<String, Object>>) headers.get(X_DEATH);
		Long count = new Long(0);
		if (xDeath != null) {
			for (HashMap<String, Object> items : xDeath) {
				if (MESSAGE_STATUS_REJECTED.equals(items.get(MESSAGE_REASON))) {
					count++;
				}
			}
			return count;
		}
		return null;
	}
	
	/**
	 * eventTypeDesURIMap method maps message eventType with DES-URI 
	 * @param message
	 */
	private String getDesURI(Map<String, String> headerString) {		
		return getPropertyEventTypeValue(headerString, eventTypeDesURI.getEventType());
	}

	/**
	 * eventTypeDesMethodMap method maps message eventType with DES-METHOD
	 * @param message
	 * @return
	 */
	private String getDesMethod(Map<String, String> headerString) {
		return getPropertyEventTypeValue(headerString, eventTypeDesMethod.getEventTypeDesMethod());
	}

	private String getPropertyEventTypeValue(Map<String, String> headerString, Map<String, String> eventTypeMap) {
		String messageEventType = headerString.get(API_EVENT_TYPE);
		String returnValue = eventTypeMap.get(messageEventType);
		if (!StringUtils.isEmpty(returnValue)) {
			return returnValue;
		}
		return null;
	}

}
