package comm.activemq;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

public class TopicPublisher {
	
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Topic topic;
	private MessageProducer publisher;
	
    private String user;
	private String password;
	private String url;
	private String topicId;
	
	private boolean transacted = false;
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	
    public static void main(String[] args) {
//		new TopicPublisher().init();
	}
    
    public TopicPublisher(String topicId) throws JMSException {
    	this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = false;
    	this.ackMode = Session.AUTO_ACKNOWLEDGE;
    	this.topicId = topicId;
    	init(transacted, ackMode, this.topicId);
	}
    
    /**
     * 
     * @param transacted: 是否使用事务:当消息发送者向消息提供者（即消息代理）发送消息时，消息发送者等待消息代理的确认，没有回应则抛出异常，消息发送程序负责处理这个错误。
     * 					若使用事物， 则是阻塞发送
     * @param ackMode: 消息的确认模式, 值有三种:
     * 							Session.AUTO_ACKNOWLEDGE:指定消息提供者在每次收到消息时自动发送确认。消息只向目标发送一次，但传输过程中可能因为错误而丢失消息。
     * 							Session.CLIENT_ACKNOWLEDGE:由消息接收者确认收到消息，通过调用消息的acknowledge()方法（会通知消息提供者收到了消息）
     * 							Session.DUPS_OK_ACKNOWLEDGE:指定消息提供者在消息接收者没有确认发送时重新发送消息（这种确认模式不在乎接收者收到重复的消息）
     * @param topicId： 主题的id
     * @throws JMSException
     */
	public TopicPublisher(Boolean transacted, int ackMode, String topicId) throws JMSException {
		this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = transacted;
    	this.ackMode = ackMode;
    	this.topicId = topicId;
    	init(this.transacted, this.ackMode, this.topicId);
	}
	
	/**
	 * 
	 * @param transacted
	 * @param ackMode
	 * @param topicId
	 * @throws JMSException
	 */
	private void init(Boolean transacted, int ackMode, String topicId) throws JMSException{
		connectionFactory = new ActiveMQConnectionFactory(user, password, url);
		connection = connectionFactory.createConnection();
		connection.start();
		
		session = connection.createSession(this.transacted, this.ackMode);
		topic = session.createTopic(topicId);
		publisher = session.createProducer(topic);
	}
	
	/**
	 * 
	 * @param persistent 消息是否持久化
	 * @param timeToLive timeToLive the default length of time in milliseconds from its dispatch time
     * 		  that a produced message should be retained by the message system.
	 * @param msg
	 * @throws JMSException 
	 */
	public void publishMsg(Boolean persistent, long timeToLive, Message msg ) throws JMSException{
		if (persistent) {
			publisher.setDeliveryMode(DeliveryMode.PERSISTENT);
        }
        else {
        	publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        }
		if (timeToLive > 0) {
			publisher.setTimeToLive(timeToLive);
        }
		publisher.send(msg);
		
		if ( this.transacted ) {
            session.commit();
        }
	}
	
	public void destroy() {
		this.topic = null;
		try {
			this.publisher.close();
			this.session.close();
			this.connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public Session getSession() {
		return session;
	}
	public String getTopicId() {
		return topicId;
	}
	public void setTopic(String topicId) throws JMSException {
		this.topicId = topicId;
		this.publisher.close();
		
		this.publisher = null;
		this.topic = null;
		
		this.topic = session.createTopic(topicId);
		this.publisher = session.createProducer(topic);
	}
	
}
