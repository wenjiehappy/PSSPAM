//package comm.activemq;
//
//import java.util.Random;
//import javax.jms.JMSException;
//import javax.jms.Message;
//import javax.jms.TextMessage;
//
//public class TestDemoBlock {
//	public static void main(String[] args) {
//		int size = 4;
//		int[][] array = new int[size][size];
//		Point2PointProducer[][] pro = new Point2PointProducer[size][size];
//		Point2PointConsumerblock[][] con = new Point2PointConsumerblock[size][size];
////		Point2PointConsumerAsync[][] con = new Point2PointConsumerAsync[size][size];
//		for( int i = 0; i < size; i++ ){
//			for( int j = 0; j < size; j++ ){
//				try {
//					pro[i][j] = new Point2PointProducer();
//					con[i][j] = new Point2PointConsumerblock( i+"_"+j );
////					con[i][j] = new Point2PointConsumerAsync(i+"_"+j) {
////						@Override
////						public void onException(JMSException exception) {
////							//TODO
////						}
////						@Override
////						public void consumeMessage(Message message) {
////							if( null != message && message instanceof TextMessage ){
////								try {
////									System.out.println( ( (TextMessage)message ).getText() );
////								} catch (JMSException e) {
////									e.printStackTrace();
////								}
////							}
////						}
////					};
//				} catch (JMSException e) {
//					e.printStackTrace();
//					System.exit(0);
//				}
//				if( i == j )
//					array[i][j] = 0;
//				else
//					array[i][j] = ( new Random() ).nextInt(2);
//				System.out.print(array[i][j]+"  ");
//			}
//			System.out.println();
//		}
//		
//		for( int i = 0; i < size; i++ ){
//			for( int j = 0; j < size; j++ ){
//				if( 1 == array[i][j] ){
//					try {
//						Message msg = MessageUtil.createTextMessage(pro[i][j].getSession(), "text from "+i+" to "+j);
//						if( null != msg )
//							pro[i][j].sendMessage(i+"_"+j, false, 0, msg);
//						System.out.println("send:  "+i+"  "+j);
//					} catch (JMSException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//		
//		for( int i = 0; i < size; i++ ){
//			for( int j = 0; j < size; j++ ){
//				try {
//					Message msg = con[i][j].receiveMsg(100);
//					if( null != msg && msg instanceof TextMessage )
//						System.out.println( ( (TextMessage)msg ).getText() );
//				} catch (JMSException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		//
//		for( int i = 0; i < size; i++ ){
//			for( int j = 0; j < size; j++ ){
//				pro[i][j].destroy();
//				con[i][j].destroy();
//			}
//		}
//	}
//}
