package uk.gov.hmrc.des.pubsub.listener.core.exceptions;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import uk.gov.hmrc.des.pubsub.listener.core.config.MDMSConstants.Status;
import uk.gov.hmrc.des.pubsub.listener.core.tracer.LoggerManager;

public class GenericListenerException extends AmqpRejectAndDontRequeueException {	

	private static final long serialVersionUID = 2042345542948689440L;
	
	public GenericListenerException(String message) {
		super(message);
		LoggerManager.getLogger(GenericListenerException.class, "LBL_GENERIC_LISTENER_EXCEPTION").error("message: {}", message);
	}
	
	public GenericListenerException(Status status, String message) {
		super(message);
		LoggerManager.getLogger(GenericListenerException.class, "LBL_GENERIC_LISTENER_EXCEPTION", status.getCode()).error("{}, message: {}",status.getDescription(), message);
	}
}
