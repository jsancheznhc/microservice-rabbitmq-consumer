package uk.gov.hmrc.des.pubsub.listener.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "uk.gov.hmrc.mdms")
@Component
public class EventTypeDesMethodMapProperty {

    private final Map<String, String> eventTypeDesMethod = new HashMap<>();

	public Map<String, String> getEventTypeDesMethod() {
		return eventTypeDesMethod;
	}
 
}
