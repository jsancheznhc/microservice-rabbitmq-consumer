/**
 * 
 */
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

import uk.gov.hmrc.des.pubsub.listener.amqp.client.ListenerMessageReceiver;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants;
import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.model.RestRequest;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;

/**
 * @author jsanchez
 *
 */
@Service
public class RestClientService {
	@Value(value = "${" + MDMSConstants.CONFIG_REST_ENDPOINT + "}")
	String ENDPOINT;
	@Value(value = "${" + MDMSConstants.CONFIG_REST_CREDENTIALS + "}")
	String ESB_CREDENTIALS;
	@Value(value = "${" + MDMSConstants.CONFIG_REST_TIMEOUT + "}")
	int TIMEOUT;

	private RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		LoggerManager.getLogger(RestClientService.class, "LBL_REST_TEMPLATE_CONSTRUCTOR").debug("Creating requestFactory with TIMEOUT:{}", TIMEOUT);
		factory.setConnectTimeout(TIMEOUT);
		factory.setReadTimeout(TIMEOUT);
		restTemplate = new RestTemplate(factory);
		return restTemplate;
	}

	public MDMSConstants.Status consume(RestRequest request) throws RestClientException {
		if (validate(request)) {
			// prepare the headers
			HttpHeaders headers = buildHeaders(request);
			// Update MDC Map
			MDC.clear();
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS", Status.DIAGNOSTIC.getCode()).debug(
					"{}, ReFill Headers: {} into MDC for diagnostic", Status.DIAGNOSTIC.getDescription(), headers);
			MDC.setContextMap(headers.toSingleValueMap());
			// MDC.put(MDMSConstants.RETRIES, String.valueOf(retry));
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_HEADERS_MDC", Status.DIAGNOSTIC.getCode())
					.debug("{}, MDC Headers: {}", Status.DIAGNOSTIC.getDescription(), MDC.getCopyOfContextMap());

			// Create request object
			HttpEntity<String> restRequest = new HttpEntity<String>(request.getBody(), headers);
			String requestEndpoint = ENDPOINT + request.getEndpoint();
			ResponseEntity<Object> response = null;
			try {
				switch (request.getMethod()) {
				case MDMSConstants.METHOD_PUT:
					response = getRestTemplate().exchange(requestEndpoint, HttpMethod.PUT, restRequest, Object.class);
					break;
				default:
					response = getRestTemplate().postForEntity(requestEndpoint, restRequest, Object.class);
					break;
				}

			} catch (HttpServerErrorException | HttpClientErrorException httpError) {
				LoggerManager.getLogger(RestClientService.class, "LBL_RESTCLIENT_ERROR").error("Http Error, code: {}", httpError.getRawStatusCode());
				return MDMSConstants.Status.fromCode(httpError.getRawStatusCode());
			} catch (Exception exc) {
				if (exc.getCause() instanceof SocketTimeoutException) {
					LoggerManager.getLogger(RestClientService.class, "LBL_RESTCLIENT_TIMEOUT").error("Connection timeout");
					return MDMSConstants.Status.RETRY;
				}
				throw new RestClientException(exc.getMessage(), exc);
			}
			// The message should be dead lettered in case of unknown/undefined HTTP codes
			// received from Generic Subscriber component/backend
			MDMSConstants.Status restResponse = MDMSConstants.Status
					.fromCode(response != null ? response.getStatusCodeValue() : MDMSConstants.Status.DLQ.getCode());
			LoggerManager.getLogger(ListenerMessageReceiver.class, "LBL_RESTCLIENT_RESPONSE", Status.DIAGNOSTIC.getCode())
			.debug("{}, Response: {}", Status.DIAGNOSTIC.getDescription(), restResponse);
			return restResponse;
			// return Status.SUCCESS;
		} else {
			return Status.BAD_REQUEST;
		}
	}

	private boolean validate(RestRequest request) {
		if (request == null || request.getBody() == null || request.getHeaders() == null) {
			// throw new InvalidRestRequestException("The request object is invalid.");
			return false;
		}
		return true;
	}

	private HttpHeaders buildHeaders(RestRequest request) {
		HttpHeaders headers = new HttpHeaders();
		request.getHeaders().forEach((key, val) -> {
			headers.add(key, val);
		});
		// Check for existing Auth Header
		if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
			headers.remove(HttpHeaders.AUTHORIZATION);
			headers.add(HttpHeaders.AUTHORIZATION, ESB_CREDENTIALS);

		} else {
			headers.add(HttpHeaders.AUTHORIZATION, ESB_CREDENTIALS);
		}

		return headers;
	}
}
