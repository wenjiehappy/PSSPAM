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
	 * Ĭ�ϲ�ʹ�������Ϣ��ȷ��ģʽ��Session.AUTO_ACKNOWLEDGE
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
     * @param transacted: �Ƿ�ʹ������:����Ϣ����������Ϣ�ṩ�ߣ�����Ϣ����������Ϣʱ����Ϣ�����ߵȴ���Ϣ�����ȷ�ϣ�û�л�Ӧ���׳��쳣����Ϣ���ͳ��������������
     * 					��ʹ����� ������������
     * @param ackMode: ��Ϣ��ȷ��ģʽ, ֵ������:
     * 							Session.AUTO_ACKNOWLEDGE:ָ����Ϣ�ṩ����ÿ���յ���Ϣʱ�Զ�����ȷ�ϡ���Ϣֻ��Ŀ�귢��һ�Σ�����������п�����Ϊ�������ʧ��Ϣ��
     * 							Session.CLIENT_ACKNOWLEDGE:����Ϣ������ȷ���յ���Ϣ��ͨ��������Ϣ��acknowledge()��������֪ͨ��Ϣ�ṩ���յ�����Ϣ��
     * 							Session.DUPS_OK_ACKNOWLEDGE:ָ����Ϣ�ṩ������Ϣ������û��ȷ�Ϸ���ʱ���·�����Ϣ������ȷ��ģʽ���ں��������յ��ظ�����Ϣ��
     * @param queueId�� ���ն��е�id
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
	 * @param timeout��������Ϣ�ĳ�ʱʱ�䣬Ϊ0�Ļ��򲻳�ʱ��receive������һ����Ϣ�����ǳ�ʱ�˻��������߱��رգ�����null
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
