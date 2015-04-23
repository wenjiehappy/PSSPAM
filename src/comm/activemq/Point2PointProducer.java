package comm.activemq;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
  
public class Point2PointProducer {
	
	private Connection connection;
	private Session session;
	private Destination destination;
	
	private String user;
    private String password;
    private String url;
    
    private Boolean transacted;
    private int ackMode;
    private String queueId ;
    
    private MessageProducer producer;
    
	/**
	 * 默认不使用事物，消息的确认模式：Session.AUTO_ACKNOWLEDGE
	 * @throws JMSException
	 */
    public Point2PointProducer( String queueId ) throws JMSException {
    	this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	this.transacted = false;
    	this.ackMode = Session.AUTO_ACKNOWLEDGE;
    	this.queueId = queueId;
    	
    	init(transacted, ackMode);
    }
    
    
    /**
     * 
     * @param transacted: 是否使用事务:当消息发送者向消息提供者（即消息代理）发送消息时，消息发送者等待消息代理的确认，没有回应则抛出异常，消息发送程序负责处理这个错误。
     * 					若使用事物， 则是阻塞发送
     * @param ackMode: 消息的确认模式, 值有三种:
     * 							Session.AUTO_ACKNOWLEDGE:指定消息提供者在每次收到消息时自动发送确认。消息只向目标发送一次，但传输过程中可能因为错误而丢失消息。
     * 							Session.CLIENT_ACKNOWLEDGE:由消息接收者确认收到消息，通过调用消息的acknowledge()方法（会通知消息提供者收到了消息）
     * 							Session.DUPS_OK_ACKNOWLEDGE:指定消息提供者在消息接收者没有确认发送时重新发送消息（这种确认模式不在乎接收者收到重复的消息）
     * @throws JMSException
     */
    public Point2PointProducer(String queueId, Boolean transacted, int ackMode) throws JMSException{
    	this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = transacted;
    	this.ackMode = ackMode;
    	this.queueId = queueId;
    	init(transacted, ackMode);
    }
    /**
     * 
     * @param queueId
     * @param persistent 消息是否持久化
     * @param timeToLive the default length of time in milliseconds from its dispatch time
     * 		  that a produced message should be retained by the message system.
     * @param msg
     * @throws JMSException
     */
	public void sendMessage(Boolean persistent, long timeToLive, Message msg) throws JMSException{
		if( null == session )
			throw new JMSException("session is null");
		
        if (persistent) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        }
        else {
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        }
        if (timeToLive != 0) {
            producer.setTimeToLive(timeToLive);
        }
        producer.send(msg);
        
        if (this.transacted) {
            session.commit();
        }
	}
	
	private void init(Boolean transacted, int ackMode) throws JMSException{
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
    	this.connection = connectionFactory.createConnection();
		this.connection.start();
		
		//Create the session
		this.session = connection.createSession(transacted, ackMode);
		destination = session.createQueue(queueId);
		producer = session.createProducer(destination);
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
	
	public void setSession(Boolean transacted, int ackMode) throws JMSException {
		try {
			this.session.close();
		} catch (JMSException e) {
			e.printStackTrace();
			this.session = null;
		}
		if( null == this.connection )
			throw new JMSException("connection is null");
		this.session = this.connection.createSession(transacted, ackMode);
		
		this.transacted = transacted;
		this.ackMode = ackMode;
	}
	public Session getSession(){
		return this.session;
	}
	public Boolean getTransacted(){
		return transacted;
	}
	public int getAckMode() {
		return ackMode;
	}
} 