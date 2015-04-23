package comm.activemq;

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

public class MessageUtil {
	
//	public static final int TEXT_MSG = 1;
//	public static final int BYTES_MSG = 2;
//	public static final int OBJECT_MSG = 3;
//	public static final int STREAM_MSG = 4;
//	public static final int MAP_MSG = 5;
	
	public static TextMessage createTextMessage(Session session, String text) throws JMSException {
		if (null == session)
			throw new JMSException("session is null");
		TextMessage result = session.createTextMessage(text);

		return result;
	}

	public static ObjectMessage createObjectMessage(Session session, Serializable obj)
			throws JMSException {
		if (null == session)
			throw new JMSException("session is null");
		ObjectMessage result = session.createObjectMessage(obj);
		return result;
	}

	public static BytesMessage createBytesMessage(Session session, byte[] text) throws JMSException {
		if (null == session)
			throw new JMSException("session is null");
		BytesMessage result = session.createBytesMessage();
		result.writeBytes(text);
		return result;
	}

	public static StreamMessage createStreamMessage(Session session, byte[] text) throws JMSException {
		if (null == session)
			throw new JMSException("session is null");
		StreamMessage result = session.createStreamMessage();
		result.writeBytes(text);
		return result;
	}

	public static MapMessage createMapMessage(Session session, byte[] text) throws JMSException {
		if (null == session)
			throw new JMSException("session is null");
		MapMessage result = session.createMapMessage();
		return result;
	}
}
