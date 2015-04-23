package comm.activemq;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class TestTopicAsync {
	public static void main(String[] args) {
		int size = 4;
		TopicPublisher pub = null;
		TopicSubscriberAsync[][] sub = new TopicSubscriberAsync[size][size];
		String topic = "myTopic";
		
		for( int i = 0; i < size; i++ ){
			for( int j = 0; j < size; j++ ){
				try {
					sub[i][j] = new TopicSubscriberAsync(topic) {
						@Override
						public void consumeMessage(Message msg) {
							if( null != msg && msg instanceof TextMessage ){
								try {
									System.out.println( ( (TextMessage)msg ).getText() );
								} catch (JMSException e) {
									e.printStackTrace();
								}
							}
						}

						@Override
						public void onException(JMSException exception) {
							// TODO Auto-generated method stub
						}
					};
					/*Message msg = sub[i][j].receiveMsg(10);
					if( null == msg ){
						System.out.println(i+"  "+j+"  null" );
					}
					if( null != msg && msg instanceof TextMessage ){
						System.out.println(i+"  "+j+"  "+ ( (TextMessage)msg ).getText() );
					}*/
				} catch (JMSException e) {
					e.printStackTrace();
				} 
			}
		}

		//只能接收到订阅以后发布的公告，之前的公告是接收不到的
		try {
			pub = new TopicPublisher(topic);
			Message msg = MessageUtil.createTextMessage(pub.getSession(), topic);
			pub.publishMsg(false, 0, msg);
		} catch (JMSException e) {
			e.printStackTrace();
			System.exit(0);
		}
		if( null != pub )
			pub.destroy();
		for( int i = 0; i < size; i++ ){
			for( int j = 0; j < size; j++ ){
				sub[i][j].destroy();
			}
		}
	}
}
