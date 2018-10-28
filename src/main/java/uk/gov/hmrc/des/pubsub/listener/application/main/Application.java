package uk.gov.hmrc.des.pubsub.listener.application.main;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.annotation.PostConstruct;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.contrib.servopublisher.HystrixServoMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.rabbitmq.client.Channel;

import uk.gov.hmrc.des.pubsub.listener.amqp.client.AcknowledgementLogic;
import uk.gov.hmrc.des.pubsub.listener.amqp.client.ListenerHystrixAsync;
import uk.gov.hmrc.des.pubsub.listener.amqp.client.ListenerHystrixSync;
import uk.gov.hmrc.des.pubsub.listener.amqp.client.ListenerMessageReceiver;
import uk.gov.hmrc.des.pubsub.listener.core.config.EventTypeDesMethodMapProperty;
import uk.gov.hmrc.des.pubsub.listener.core.config.EventTypeDesURIMapProperty;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;
import uk.gov.hmrc.des.pubsub.listener.rest.client.RestClient;
import uk.gov.hmrc.des.pubsub.listener.rest.client.RestClientService;

@Configuration
@SpringBootApplication
@ComponentScan
@EnableCircuitBreaker
@EnableHystrix
@EnableHystrixDashboard
public class Application {

	Channel channel;

	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
			MessageListenerAdapter listenerAdapter) {
		final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setMessageListener(listenerAdapter);
		container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		LoggerManager.getLogger(Application.class, "LBL_SETTINGS_MAIN", Status.DIAGNOSTIC.getCode())
				.info("{}, Setting connectionFactory", Status.DIAGNOSTIC.getDescription());
		return container;
	}

	/*
	 * @Bean ListenerMessageReceiver receiver() { return new
	 * ListenerMessageReceiver(); }
	 * 
	 * @Bean MessageListenerAdapter listenerAdapter(ListenerMessageReceiver
	 * receiver) { return new MessageListenerAdapter(receiver, "receiveMessage"); }
	 * 
	 * @Bean RestClient restClient() { return new RestClient(); }
	 */

	// circuit breaker sync 2 on rest consumer
	
	  @Bean ListenerHystrixSync receiver() { return new ListenerHystrixSync(); }
	  
	  @Bean MessageListenerAdapter listenerAdapter(ListenerHystrixSync receiver) {
	  return new MessageListenerAdapter(receiver, "listener"); }
	 
	// END circuit breaker sync 2

	// circuit breaker async 2 on rest consumer
	/*
	@Bean
	ListenerHystrixAsync receiver() {
		return new ListenerHystrixAsync();
	}

	@Bean
	MessageListenerAdapter listenerAdapter(ListenerHystrixAsync receiver) {
		return new MessageListenerAdapter(receiver, "listener");
	}
	// END circuit breaker async 2
	*/
	  
	@Bean
	RestClientService restClientService() { 
		return new RestClientService(); 
	}
	
	@Bean
	AcknowledgementLogic ackLogic() {
		return new AcknowledgementLogic();
	}

	@Bean
	EventTypeDesURIMapProperty eventTypeProp() {
		return new EventTypeDesURIMapProperty();
	}

	@Bean
	EventTypeDesMethodMapProperty eventTypeDesMethodProp() {
		return new EventTypeDesMethodMapProperty();
	}

	public static void main(String[] args) throws InterruptedException {
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
		LoggerManager.getLogger(Application.class, "LBL_MAIN", Status.DIAGNOSTIC.getCode())
				.info("{}, Run application at {}", Status.DIAGNOSTIC.getDescription(), timeStamp);
		SpringApplication.run(Application.class, args);
	}

}
