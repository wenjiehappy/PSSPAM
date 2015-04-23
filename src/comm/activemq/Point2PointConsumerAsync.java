package comm.activemq;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

public abstract class Point2PointConsumerAsync implements ExceptionListener, MessageListener {
	
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Destination destination;
	
	private MessageConsumer consumer;
	private String queueId;
	
	private String user;
	private String password;
	private String url;
	
	private boolean transacted = false;
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	
	/**
	 * 默认不使用事物，消息的确认模式：Session.AUTO_ACKNOWLEDGE
	 * @throws JMSException
	 */
	public Point2PointConsumerAsync(String queueId) throws JMSException {
		this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = false;
    	this.ackMode = Session.AUTO_ACKNOWLEDGE;
    	this.queueId = queueId;
    	
    	init(this.transacted, this.ackMode, this.queueId);
	}
	
	/**
     * 
     * @param transacted: 是否使用事务:当消息发送者向消息提供者（即消息代理）发送消息时，消息发送者等待消息代理的确认，没有回应则抛出异常，消息发送程序负责处理这个错误。
     * 					若使用事物， 则是阻塞发送
     * @param ackMode: 消息的确认模式, 值有三种:
     * 							Session.AUTO_ACKNOWLEDGE:指定消息提供者在每次收到消息时自动发送确认。消息只向目标发送一次，但传输过程中可能因为错误而丢失消息。
     * 							Session.CLIENT_ACKNOWLEDGE:由消息接收者确认收到消息，通过调用消息的acknowledge()方法（会通知消息提供者收到了消息）
     * 							Session.DUPS_OK_ACKNOWLEDGE:指定消息提供者在消息接收者没有确认发送时重新发送消息（这种确认模式不在乎接收者收到重复的消息）
     * @param queueId： 接收队列的id
     * @throws JMSException
     */
	public Point2PointConsumerAsync(Boolean transacted, int ackMode, String queueId) throws JMSException {
		this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = transacted;
    	this.ackMode = ackMode;
    	this.queueId = queueId;
    	
    	init(this.transacted, this.ackMode, this.queueId);
	}
	
	private void init(Boolean transacted, int ackMode, String queueId) throws JMSException{
		connectionFactory = new ActiveMQConnectionFactory(user, password, url);
		connection = connectionFactory.createConnection();
		connection.setExceptionListener(this);
        connection.start();
        session = connection.createSession(transacted, ackMode);
        destination = session.createQueue(queueId);
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
	}
	
	@Override
	public void onMessage(Message message) {
		consumeMessage(message);
	}
	
	public abstract void consumeMessage( Message message );
	
	/**
	 * @param timeout：接收消息的超时时间，为0的话则不超时，receive返回下一个消息，但是超时了或者消费者被关闭，返回null
	 * @return
	 * @throws JMSException
	 */
	public Message receiveMsg( Long timeout ) throws JMSException{
		if( null == consumer )
			throw new JMSException("consumer is null");
		if( timeout > 0 )
			return consumer.receive( timeout );
		else
			return consumer.receive();
	}
	
	public void destroy() {
		this.destination = null;
		try {
			this.session.close();
			this.connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
}
