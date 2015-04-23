package comm.activemq;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class TestTopicBlock {
	public static void main(String[] args) {
		int size = 4;
		TopicPublisher pub = null;
		TopicSubscriberBlock[][] sub = new TopicSubscriberBlock[size][size];
//		TopicSubscriberAsync[][] sub = new TopicSubscriberAsync[size][size];
		String topic = "myTopic";
		
		for( int i = 0; i < size; i++ ){
			for( int j = 0; j < size; j++ ){
				try {
					sub[i][j] = new TopicSubscriberBlock(topic);
					Message msg = sub[i][j].receiveMsg(10);
					if( null == msg ){
						System.out.println(i+"  "+j+"  null" );
					}
					if( null != msg && msg instanceof TextMessage ){
						System.out.println(i+"  "+j+"  "+ ( (TextMessage)msg ).getText() );
					}
				} catch (JMSException e) {
					e.printStackTrace();
				} 
			}
		}
		
		try {
			pub = new TopicPublisher(topic);
			Message msg = MessageUtil.createTextMessage(pub.getSession(), topic);
			pub.publishMsg(false, 0, msg);
		} catch (JMSException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		//ֻ�ܽ��յ������Ժ󷢲��Ĺ��棬֮ǰ�Ĺ����ǽ��ղ�����
		for( int i = 0; i < size; i++ ){
			for( int j = 0; j < size; j++ ){
				try {
					Message msg = sub[i][j].receiveMsg(10);
					if( null == msg ){
						System.out.println(i+"  "+j+"  null" );
					}
					if( null != msg && msg instanceof TextMessage ){
						System.out.println(i+"  "+j+"  "+ ( (TextMessage)msg ).getText() );
					}
				} catch (JMSException e) {
					e.printStackTrace();
				}
				sub[i][j].destroy();
			}
		}
		if( null != pub )
			pub.destroy();
	}
}
