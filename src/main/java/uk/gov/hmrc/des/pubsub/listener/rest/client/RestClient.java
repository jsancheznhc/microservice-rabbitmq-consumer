package uk.gov.hmrc.des.pubsub.listener.rest.client;

import java.net.SocketTimeoutException;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmrc.des.pubsub.listener.amqp.client.AcknowledgementLogic;
import uk.gov.hmrc.des.pubsub.listener.amqp.client.ListenerMessageReceiver;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.InvalidRestRequestException;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.UnknownBackendResponseException;
import uk.gov.hmrc.des.pubsub.listener.core.model.RestRequest;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;

/**
 * Service designed to post REST calls to the backend (in this case would be
 * CORP ESB)
 * 
 * @author hcorrales
 *
 */
@Service
public class RestClient {

	RestTemplate restTemplate;

	private static String statusText_serviceUnavailable = "Service Unavailable";
	private static int statusCode_serviceUnavailable = 503;

	private static String statusText_forbidden = "Forbidden";
	private static int statusCode_forbidden = 403;

	
	@Value(value = "${" + MDMSConstants.CONFIG_REST_ENDPOINT + "}")
	String ENDPOINT;
	@Value(value = "${" + MDMSConstants.CONFIG_REST_CREDENTIALS + "}")
	String ESB_CREDENTIALS;
	@Value(value = "${" + MDMSConstants.CONFIG_REST_TIMEOUT + "}")
	int TIMEOUT;

	/**
	 * Posts the RestRequest object to the specified endpoint (in .properties file)
	 * 
	 * @param request
	 * @return MDMSConstants.Status object with corresponding value (SUCCESS, RETRY
	 *         or DLQ)
	 * @throws UnknownBackendResponseException
	 *             - If the httpstatus code from the response does not match a
	 *             Status
	 * @throws InvalidRestRequestException
	 *             - If the RestRequest object is invalid
	 * @throws RestClientException
	 *             - If there was an exception in the RestTemplate call
	 */
	public MDMSConstants.Status sendMessage(RestRequest request) throws UnknownBackendResponseException, InvalidRestRequestException, RestClientException {
		// First check the request object
		if (request == null || request.getBody() == null || request.getHeaders() == null) {
			throw new InvalidRestRequestException("The request object is invalid.");
		}		
		
		// Set the request headers
		HttpHeaders headers = new HttpHeaders();
		request.getHeaders().forEach((key, val) -> {
			
			headers.add(key, val);
		});
		//Check for existing Auth Header
		if(headers.containsKey(HttpHeaders.AUTHORIZATION))
		{
			headers.remove(HttpHeaders.AUTHORIZATION);
			headers.add(HttpHeaders.AUTHORIZATION, ESB_CREDENTIALS);
			
		}
		else
		{
			headers.add(HttpHeaders.AUTHORIZATION, ESB_CREDENTIALS);
		}
		
		// Update MDC Map 
		MDC.clear();
		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS", Status.DIAGNOSTIC.getCode()).debug("{}, ReFill Headers: {} into MDC for diagnostic",
				Status.DIAGNOSTIC.getDescription(), headers);
		MDC.setContextMap(headers.toSingleValueMap());
		//MDC.put(MDMSConstants.RETRIES, String.valueOf(retry));
		LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS_MDC", Status.DIAGNOSTIC.getCode()).debug("{}, MDC Headers: {}", Status.DIAGNOSTIC.getDescription(),
				MDC.getCopyOfContextMap());
		
		// Create request object
		HttpEntity<String> restRequest = new HttpEntity<String>(request.getBody(), headers);
		String requestEndpoint = ENDPOINT + request.getEndpoint();
		ResponseEntity<Object> response = null;

		try {
			switch(request.getMethod()) {
				case MDMSConstants.METHOD_PUT:
					response = this.getRestTemplate().exchange(requestEndpoint, HttpMethod.PUT, restRequest, Object.class);
					break;
				default:
					response = this.getRestTemplate().postForEntity(requestEndpoint, restRequest, Object.class);
					break;
			}
		} catch (HttpServerErrorException httpError) {
			if (httpError.getStatusText().equalsIgnoreCase(statusText_serviceUnavailable) && httpError.getRawStatusCode() == statusCode_serviceUnavailable) {
				return MDMSConstants.Status.RETRY;
			}
		} catch (HttpClientErrorException httpError) {
			if (httpError.getStatusText().equalsIgnoreCase(statusText_forbidden) && httpError.getRawStatusCode() == statusCode_forbidden) {
				return MDMSConstants.Status.DLQ;
			}
		} catch (Exception exc) {
			if(exc.getCause() instanceof SocketTimeoutException) {
				LoggerManager.getLogger(RestClient.class, "LBL_RESTCLIENT_TIMEOUT").error("Connection timeout");
				return MDMSConstants.Status.RETRY;
			}
			throw new RestClientException(exc.getMessage(), exc);
		}

		LoggerManager.getLogger(AcknowledgementLogic.class, "CHECK_HTTP_STATUS_ACK", Status.DIAGNOSTIC.getCode()).debug("{}, The response from rest service: {}",
				Status.DIAGNOSTIC.getDescription(), response);

		MDMSConstants.Status restResponse = null;
		// The message should be dead lettered in case of unknown/undefined HTTP codes received from Generic Subscriber component/backend
		restResponse = MDMSConstants.Status.fromCode(response != null ? response.getStatusCodeValue() : MDMSConstants.Status.DLQ.getCode());

		if (restResponse == null || restResponse.equals(MDMSConstants.Status.UNKNOWN)) {
			throw new UnknownBackendResponseException("HTTP Status code is null.");
		}
		return restResponse;
	}
	
	public MDMSConstants.Status sendMessageFallbackRest(RestRequest request) throws UnknownBackendResponseException, InvalidRestRequestException, RestClientException {
		LoggerManager.getLogger(AcknowledgementLogic.class, "CHECK_HTTP_STATUS_FALLBACK", Status.DIAGNOSTIC.getCode()).debug("{}, The response from rest service",
				Status.DIAGNOSTIC.getDescription());
		return Status.DLQ;
	}
	private RestTemplate getRestTemplate() {
		if (this.restTemplate == null) {
			HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
			LoggerManager.getLogger(RestClient.class, "LBL_RESTCLIENT_CONSTRUCTOR").debug("TIMEOUT:{}", TIMEOUT);
			factory.setConnectTimeout(TIMEOUT);
			factory.setReadTimeout(TIMEOUT);
			this.restTemplate = new RestTemplate(factory);
		}
		return this.restTemplate;
	}

}
