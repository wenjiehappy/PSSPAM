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
	 * Ĭ�ϲ�ʹ�������Ϣ��ȷ��ģʽ��Session.AUTO_ACKNOWLEDGE
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
     * @param transacted: �Ƿ�ʹ������:����Ϣ����������Ϣ�ṩ�ߣ�����Ϣ����������Ϣʱ����Ϣ�����ߵȴ���Ϣ�����ȷ�ϣ�û�л�Ӧ���׳��쳣����Ϣ���ͳ��������������
     * 					��ʹ����� ������������
     * @param ackMode: ��Ϣ��ȷ��ģʽ, ֵ������:
     * 							Session.AUTO_ACKNOWLEDGE:ָ����Ϣ�ṩ����ÿ���յ���Ϣʱ�Զ�����ȷ�ϡ���Ϣֻ��Ŀ�귢��һ�Σ�����������п�����Ϊ�������ʧ��Ϣ��
     * 							Session.CLIENT_ACKNOWLEDGE:����Ϣ������ȷ���յ���Ϣ��ͨ��������Ϣ��acknowledge()��������֪ͨ��Ϣ�ṩ���յ�����Ϣ��
     * 							Session.DUPS_OK_ACKNOWLEDGE:ָ����Ϣ�ṩ������Ϣ������û��ȷ�Ϸ���ʱ���·�����Ϣ������ȷ��ģʽ���ں��������յ��ظ�����Ϣ��
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
     * @param persistent ��Ϣ�Ƿ�־û�
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