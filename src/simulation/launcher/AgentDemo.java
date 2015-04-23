package simulation.launcher;

import java.util.List;
import java.util.Random;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import simulation.market.ProtocolString;
import simulation.network.Edge;
import simulation.network.Vertex;
import simulation.node.BaseAgent;
import simulation.node.Consuming;
import simulation.node.Controller;
import simulation.order.Order;
import simulation.order.Trade;

public class AgentDemo extends BaseAgent{
	
	public static String baseCommStr = "a";
	
	public AgentDemo(String angentID, Controller controller, Vertex vertex) {
		super(angentID, controller, vertex);
	}
	
	public static void main(String[] args) {
		Vertex v = new Vertex();
		v.vertexId = 10000;
		v.vertexWeight = 3;
		AgentDemo ad = new AgentDemo("10000", null, v);
		long s = System.currentTimeMillis();
		ad.runningLogic();
		System.out.println( System.currentTimeMillis() - s );
	}
	
	@Override
	public void runningLogic() {
		try {
			
//			System.out.println( " runningLogic agent ID " + getAgentID() );
			long s = 0;
			long ee = 0;
//			if( Integer.parseInt( getAgentID() ) % 10000 == 0 ){
//				s = System.currentTimeMillis();
//				System.out.println( getController().getNodeID() +" 1  tick: " + getController().getCurrentTick() + "   agentID" + getAgentID() + "   " + s );
//			}
			
			Consuming.consume( (int)getVertex().vertexWeight );
			
			/*
			Random ran = new Random();
			if( ran.nextInt( 3 ) == 0 ){
				Order or = new Order();
				or.setAgentId( getAgentID() );
				or.setCreateTime( System.currentTimeMillis() );
				or.setDirection( ran.nextInt(2) );
				or.setPrice( ran.nextDouble() * 10 );
				or.setVolume( (int)( ran.nextDouble() * 10 ) );
				
				try {
					sendOrderMsgMarket(or);
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
			*/
			
			/*
			List<Edge> edgeList = getController().getEdgeList( Integer.parseInt( getAgentID() ) );
			if( null != edgeList && edgeList.size() > 0  ){
				for( int j = 0; j < ProtocolString.rate; j++ ){
					if( edgeList.size() > 0 ){
						int ranShift = ran.nextInt( 5 );
						int index =  edgeList.size() - 1 - ranShift ;
						if( index >= 0 && index < edgeList.size() ){
							Edge ed = edgeList.get(index);
							if( null != ed ){
								StringBuilder comStr = new StringBuilder();
								for( int i = 0;i < ed.edgeweight * ProtocolString.edgeFactor; i++ ){
									comStr.append( baseCommStr );
								}
								try {
									sendCommunicationMsgToAgent( ed.endPoint.vertexId +"", comStr.toString() );
								} catch (JMSException e1) {
									e1.printStackTrace();
								}
							}
						}
					}
				}
			}
			*/
			/*
			if( "1".equals( getAgentID() ) ){
				edgeList = getController().getEdgeList( Integer.parseInt(getAgentID()) );
				int verId = ran.nextInt(40000) + 1;
				try {
					TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), verId+"" );
					txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
					txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
					txtMsg.setStringProperty( ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_DELETE_VERTEX );
					txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
					txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
					
					System.out.println( "CONTROL_CMD_DELETE_VERTEX:"+ txtMsg.getText() );
					
					getController().sendMsgPro(txtMsg);
				} catch (JMSException e) {
					e.printStackTrace();
				}
				if( getController().getCurrentTick() == 1 ){
					try {
//						TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), "6100,6942,1" );
//						txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
//						txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
//						txtMsg.setStringProperty( ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_CHANGE_LINK_WEIGHT);
//						txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
//						txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
//						
//						System.out.println( "CONTROL_CMD_CHANGE_LINK_WEIGHT:"+ txtMsg.getText() );
//						
//						getController().sendMsgPro(txtMsg);
						
						TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), "9" );
						txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						txtMsg.setStringProperty( ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_DELETE_VERTEX);
						txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
						txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
						
						System.out.println( "CONTROL_CMD_DELETE_VERTEX:"+ txtMsg.getText() );
						
						getController().sendMsgPro(txtMsg);
						
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
				
				if( getController().getCurrentTick() == 0 ){
					int index = ran.nextInt( edgeList.size() );
					Edge edge = edgeList.get(index);
					try {
						
						TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), "1,6,2" );
						txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						txtMsg.setStringProperty( ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_ADD_LINK );
						txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
						txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
						
						System.out.println( "add edge "+ txtMsg.getText() );
						
						getController().sendMsgPro(txtMsg);
						
						
//						TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), ""+edge.startPoint.vertexId+","+edge.endPoint.vertexId );
//						txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
//						txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
//						txtMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_DELETE_LINK);
//						txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
//						txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
//						
//						System.out.println( "delete edge "+ txtMsg.getText() );
//						
//						getController().sendMsgPro(txtMsg);
						
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
				else if( getController().getCurrentTick() == 1 ){
					try {
						TextMessage txtMsg = MessageUtil.createTextMessage( getController().getMsgProducer( Market.nodeID ).getSession(), "1#1,1;2,1;3,1" );
						txtMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						txtMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						txtMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_Add_VERTEX);
						txtMsg.setStringProperty( ProtocolString.F_SENDER , getController().getNodeID() );
						txtMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
						
						System.out.println( "add vertex "+ txtMsg.getText() );
						
						getController().sendMsgPro(txtMsg);
						
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
				
				
			}
			*/
			
//			if( Integer.parseInt( getAgentID() ) % 10000 == 0 ){
//				ee = System.currentTimeMillis();
//				System.out.println( getController().getNodeID() +" 2  tick: " + getController().getCurrentTick() + "   agentID" + getAgentID() + "   " + ( ee - s ) );
//				s = ee;
//			}
			
			Message msg = getFirstUnHandledMsg();
			while( null != msg ){
				try {
					String str = "agent: "+msg.getStringProperty(ProtocolString.F_RECIVER)+" "+msg.getStringProperty(ProtocolString.F_SENDER) +" ";
					String type = msg.getStringProperty(ProtocolString.F_TYPE);
					if( ProtocolString.TYPE_ORDER.equals(type) ){
						Order order = (Order) ( (ObjectMessage)msg ).getObject();
						//TODO
						//璁㈠崟娑堟伅锛屽浣曞鐞�
						str += order.convertSelfToString();
					}
					else if( ProtocolString.TYPE_TRADE.equals(type) ){
						Trade trade = (Trade) ( (ObjectMessage)msg ).getObject();
						//TODO
						//浜ゆ槗缁撴灉娑堟伅锛屽浣曞鐞�
						str += trade.convertSelfToString();
					}
					else{
						TextMessage txMsg = (TextMessage)msg;
						str += txMsg.toString();
//						if( ProtocolString.TYPE_CONTROL.equals(type) ){
//							//TODO
//							//鎺у埗娑堟伅锛屽浣曞鐞�
//							str += txMsg.getText();
//						}
//						else if( ProtocolString.TYPE_COMMUNICATION.equals(type) ){
//							//TODO
//							//浜ゆ祦娑堟伅锛屽浣曞鐞�
//							str += txMsg.getText();
//						}
					}
//					System.out.println( str );
				} catch (JMSException e) {
					e.printStackTrace();
				}
				msg = getFirstUnHandledMsg();
			}
			
//			if( Integer.parseInt( getAgentID() ) % 10000 == 0 ){
//				System.out.println( getController().getNodeID() +" 3  tick: " + getController().getCurrentTick() + "   agentID" + getAgentID() + "  " + ( System.currentTimeMillis() - s ) );
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
