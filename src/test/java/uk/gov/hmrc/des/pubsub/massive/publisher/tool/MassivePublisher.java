package uk.gov.hmrc.des.pubsub.massive.publisher.tool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class MassivePublisher {
	public static final String _queue = "mirr.q.nps.update_individual_bbsi_detail.main";
	public static final String _exchange = "pubsub.nps.uibd.topic";
	public static final String _routingKey = "#.update_individual_bbsi_detail";
	public static final String _host = "localhost";
	public static final int _port = 5672;
	public static final String _user = "wso2_rabbit_corp";
	public static final String _pass = "qAEFZ5c3";
	public static final String _virtual_host = "DES";
	public static final String MESSAGE = "{\"key\":\"value\"}";
	public static final int _num_to_publish = 100;
	public static final String _event = "BBSI_INDIVIDUAL";
	public static final String _originator = "TPI";

	public static void main(String[] args) {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(_host);
		factory.setPort(_port);
		factory.setUsername(_user);
		factory.setPassword(_pass);
		factory.setVirtualHost(_virtual_host);
		BasicProperties props = new BasicProperties();
		//props.builder().deliveryMode(1);
		try {
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();
			//channel.queueDeclare(_queue, false, false, false, null);
			Map<String, Object> headers = new HashMap<String, Object>();
			headers.put("Content-Type", "application/json");
			headers.put("Originator-Source", _originator);
			headers.put("Event-Type", _event);
			IntStream.range(0, _num_to_publish)
					.forEach(i -> {
						try {
							channel.basicPublish("", _queue, props.builder().headers(headers).deliveryMode(1).build(), MESSAGE.getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
			// close
			channel.close();
			connection.close();
		} catch (IOException | TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
