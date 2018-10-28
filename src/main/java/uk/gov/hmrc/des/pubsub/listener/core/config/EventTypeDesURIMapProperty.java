package uk.gov.hmrc.des.pubsub.listener.core.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "uk.gov.hmrc.mdms")
@Component
public class EventTypeDesURIMapProperty {
	
    private final Map<String, String> eventType = new HashMap<>();

	public Map<String, String> getEventType() {
		return eventType;
	}

}
