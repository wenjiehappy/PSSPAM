package simulation.node;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;

import simulation.market.Market;
import simulation.market.ProtocolString;
import simulation.network.Vertex;
import simulation.order.Order;

import comm.activemq.MessageUtil;
import comm.activemq.Point2PointProducer;

public abstract class BaseAgent implements Runnable, Serializable{
	
	private static final long serialVersionUID = -103257667931454827L;
	
	private Controller controller;
	private String angentID;
	private List<Message> toBeHandledMsgList = new LinkedList<Message>() ;
	private Vertex vertex;
	public BaseAgent(String angentID, Controller controller,Vertex vertex){
		this.controller = controller;
		this.angentID = angentID;
		this.vertex = vertex;
	}
	
	public String getAgentID(){
		return angentID;
	}
	
	public Controller getController(){
		return this.controller;
	}
	
	public Vertex getVertex() {
		return vertex;
	}

	public void setVertex(Vertex vertex) {
		this.vertex = vertex;
	}

	public Boolean receiveMsg( Message msg ){
		synchronized (toBeHandledMsgList) {
			toBeHandledMsgList.add(msg);
			return true;
		}
	}
	
	public Message getFirstUnHandledMsg(){
		Message result = null;
		synchronized (toBeHandledMsgList) {
			if( toBeHandledMsgList.size() > 0 ){
				result = toBeHandledMsgList.get(0);
				toBeHandledMsgList.remove(0);
			}
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		return Integer.parseInt( angentID );
	}
	
	@Override
	public boolean equals(Object obj) {
		BaseAgent agent = (BaseAgent) obj;
		if( null == agent )
			return false;
		if( this.angentID.equals(agent.angentID) )
			return true;
		return false;
	}
	
	public void sendCommunicationMsgToAgent(String receiverId, String commMsg) throws JMSException{
		
		Vertex ver = new Vertex();
		ver.vertexId = Integer.parseInt( receiverId );
		
		String nodeId = controller.getAN_Table().get(ver);
		if( null != nodeId ){
			Point2PointProducer p = controller.getMsgProducer( nodeId );
			if( null != p ){
				Message msg = MessageUtil.createTextMessage( p.getSession(), commMsg);
				if( null != msg ){
					msg.setStringProperty(ProtocolString.F_RECIVER, receiverId);
					msg.setStringProperty(ProtocolString.F_SENDER, angentID);
					msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_COMMUNICATION);
					
					controller.sendMsg( msg );
				}
			}
		}
	}
	
	public void sendOrderMsgToAgent(String receiverId, Order order) throws JMSException{
		Vertex ver = new Vertex();
		ver.vertexId = Integer.parseInt( receiverId );
		Point2PointProducer p = controller.getMsgProducer( controller.getAN_Table().get(ver) );
		
		Message msg = MessageUtil.createObjectMessage( p.getSession(), order );
		if( null != msg ){
			msg.setStringProperty(ProtocolString.F_RECIVER, receiverId);
			msg.setStringProperty(ProtocolString.F_SENDER, angentID);
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_ORDER);
			
			controller.sendMsg( msg );
		}
	}
	
	public void sendControlMsgToMarket( String msgBody ) throws JMSException{
		
		Message msg = MessageUtil.createTextMessage( controller.getMarketP().getSession(), msgBody);
		if( null != msg ){
			msg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
			msg.setStringProperty(ProtocolString.F_SENDER, angentID);
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
			
			controller.sendMsg( msg );
		}
	}
	
	public void sendOrderMsgMarket( Order order ) throws JMSException{
		
		Message msg = MessageUtil.createObjectMessage( controller.getMarketP().getSession(), order );
		if( null != msg ){
			msg.setStringProperty(ProtocolString.F_RECIVER,  Market.nodeID);
			msg.setStringProperty(ProtocolString.F_SENDER, angentID);
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_ORDER);
			
			controller.sendMsg( msg );
		}
	}
	
	public void sendNetworkChangeMsgToMarket( String msgBody, String controlCmd ) throws JMSException{
		
		Message msg = MessageUtil.createTextMessage( controller.getMarketP().getSession(), msgBody);
		if( null != msg ){
			msg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
			msg.setStringProperty(ProtocolString.F_SENDER, angentID);
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
			
			controller.sendMsg( msg );
		}
	}
	
	@Override
	public void run() {
		runningLogic();
	}
	public abstract void runningLogic();
	
	public void destory() {
	}
}
