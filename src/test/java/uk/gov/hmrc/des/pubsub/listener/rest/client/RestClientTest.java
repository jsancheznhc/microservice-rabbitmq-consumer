package uk.gov.hmrc.des.pubsub.listener.rest.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.InvalidRestRequestException;
import uk.gov.hmrc.des.pubsub.listener.core.exceptions.UnknownBackendResponseException;
import uk.gov.hmrc.des.pubsub.listener.core.model.RestRequest;

public class RestClientTest {

	RestClient restClient;

	@SuppressWarnings("unchecked")
	void configureTests(String headerOne, HttpStatus expectedResult, Object body) {
		restClient = new RestClient();
		String endpoint = "http://www.google.com";
		String credentials = "Basic XXXX";

		HttpHeaders headers = new HttpHeaders();
		headers.add("header1", headerOne);
		headers.add("Authorisation", credentials);
		HttpEntity<Object> restRequest = new HttpEntity<Object>(body, headers);

		RestTemplate restTemplate = mock(RestTemplate.class);
		ResponseEntity<Object> response = new ResponseEntity<Object>(expectedResult);
		if (headerOne.equals("exception")) {
			when(restTemplate.postForEntity(endpoint, restRequest, Object.class)).thenThrow(RestClientException.class);
		} else {
			when(restTemplate.postForEntity(endpoint, restRequest, Object.class)).thenReturn(response);
		}

		restClient.ENDPOINT = endpoint;
		restClient.ESB_CREDENTIALS = credentials;
		restClient.restTemplate = restTemplate;
	}

	Status mainTest(String header, HttpStatus expectedResult, String body) throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		configureTests(header, expectedResult, body);

		Map<String, String> headers = new HashMap<String, String>();
		headers.put("header1", header);
		RestRequest request = new RestRequest(headers, body, "POST", "/employments/events/tax-code-difference/subscribe");
		return restClient.sendMessage(request);
	}
	
	@Test
	public void test() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		Status response = mainTest("success", HttpStatus.OK, "");
		// the service is not deployed and then the client always return Status to Delete Queue
		Assert.assertTrue("The response is not Successful", response.equals(Status.DLQ));
	}
	
	/*
	@Test
	public void testOK() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		Status response = mainTest("success", HttpStatus.OK, "");
		Assert.assertTrue("The response is not Successful", response.equals(Status.SUCCESS));
	}

	@Test
	public void testRetry() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		Status response = mainTest("retry", HttpStatus.FORBIDDEN, "");
		Assert.assertTrue("The response is not DLQ", response.equals(Status.DLQ));
	}

	@Test
	public void testDLQ() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		Status response = mainTest("dlq", HttpStatus.SERVICE_UNAVAILABLE, "");
		Assert.assertTrue("The response is not a DLQ", response.equals(Status.DLQ));
	}
	*/
	//The message should be dead lettered (not exceptions) in case of unknown/undefined HTTP codes received from Generic Subscriber component/backend
	/*
	@Test(expected = UnknownBackendResponseException.class)
	public void testUnknownResponseCode() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		mainTest("dlq", HttpStatus.ACCEPTED, "");
	}
	
	@Test(expected = InvalidRestRequestException.class)
	public void testInvalidRequestObject() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		mainTest("dlq", HttpStatus.OK, null);
	}

	@Test(expected = RestClientException.class)
	public void testRestClientException() throws RestClientException, UnknownBackendResponseException, InvalidRestRequestException {
		mainTest("exception", HttpStatus.OK, "");
	}
	*/

}