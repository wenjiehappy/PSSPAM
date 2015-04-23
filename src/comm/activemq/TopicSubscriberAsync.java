package comm.activemq;

import javax.jms.Connection;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;

public abstract class TopicSubscriberAsync implements ExceptionListener, MessageListener {
	
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private Topic topic;
	private MessageConsumer topicConsumer;
	
    private String user;
	private String password;
	private String url;
	private String topicId;
	
	private boolean transacted = false;
	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	
	
	public static void main(String[] args) {
//		try {
//			new TopicSubscriber().init();
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
	}
	
	public TopicSubscriberAsync(String topicId) throws JMSException {
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
     * @param transacted: �Ƿ�ʹ������:����Ϣ����������Ϣ�ṩ�ߣ�����Ϣ������������Ϣʱ����Ϣ�����ߵȴ���Ϣ������ȷ�ϣ�û�л�Ӧ���׳��쳣����Ϣ���ͳ��������������
     * 					��ʹ����� ������������
     * @param ackMode: ��Ϣ��ȷ��ģʽ, ֵ������:
     * 							Session.AUTO_ACKNOWLEDGE:ָ����Ϣ�ṩ����ÿ���յ���Ϣʱ�Զ�����ȷ�ϡ���Ϣֻ��Ŀ�귢��һ�Σ�����������п�����Ϊ�������ʧ��Ϣ��
     * 							Session.CLIENT_ACKNOWLEDGE:����Ϣ������ȷ���յ���Ϣ��ͨ��������Ϣ��acknowledge()��������֪ͨ��Ϣ�ṩ���յ�����Ϣ��
     * 							Session.DUPS_OK_ACKNOWLEDGE:ָ����Ϣ�ṩ������Ϣ������û��ȷ�Ϸ���ʱ���·�����Ϣ������ȷ��ģʽ���ں��������յ��ظ�����Ϣ��
     * @param topicId�� �����id
     * @throws JMSException
     */
	public TopicSubscriberAsync(Boolean transacted, int ackMode, String topicId) throws JMSException {
		this.user = ActiveMqParam.USER;
    	this.password = ActiveMqParam.PASSWORD;
    	this.url = ActiveMqParam.BROKER_URL;
    	
    	this.transacted = transacted;
    	this.ackMode = ackMode;
    	this.topicId = topicId;
    	init(this.transacted, this.ackMode, this.topicId);
	}
	
	/**
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
		topicConsumer = session.createConsumer(topic);
		topicConsumer.setMessageListener(this);
	}
	
	@Override
	public void onMessage(Message message) {
		consumeMessage(message);
	}
	public abstract void consumeMessage( Message message );
	
	public void destroy() {
		this.topic = null;
		try {
			this.topicConsumer.close();
			this.session.close();
			this.connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}