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
     * @param transacted: �Ƿ�ʹ������:����Ϣ����������Ϣ�ṩ�ߣ�����Ϣ����������Ϣʱ����Ϣ�����ߵȴ���Ϣ�����ȷ�ϣ�û�л�Ӧ���׳��쳣����Ϣ���ͳ��������������
     * 					��ʹ����� ������������
     * @param ackMode: ��Ϣ��ȷ��ģʽ, ֵ������:
     * 							Session.AUTO_ACKNOWLEDGE:ָ����Ϣ�ṩ����ÿ���յ���Ϣʱ�Զ�����ȷ�ϡ���Ϣֻ��Ŀ�귢��һ�Σ�����������п�����Ϊ�������ʧ��Ϣ��
     * 							Session.CLIENT_ACKNOWLEDGE:����Ϣ������ȷ���յ���Ϣ��ͨ��������Ϣ��acknowledge()��������֪ͨ��Ϣ�ṩ���յ�����Ϣ��
     * 							Session.DUPS_OK_ACKNOWLEDGE:ָ����Ϣ�ṩ������Ϣ������û��ȷ�Ϸ���ʱ���·�����Ϣ������ȷ��ģʽ���ں��������յ��ظ�����Ϣ��
     * @param topicId�� �����id
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
	 * @param persistent ��Ϣ�Ƿ�־û�
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
