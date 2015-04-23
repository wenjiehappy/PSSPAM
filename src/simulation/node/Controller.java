package simulation.node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import simulation.launcher.AgentDemo;
import simulation.market.Market;
import simulation.market.ProtocolString;
import simulation.network.Edge;
import simulation.network.Partition;
import simulation.network.Vertex;
import simulation.order.Order;
import simulation.order.Trade;

import comm.activemq.MessageUtil;
import comm.activemq.Point2PointConsumerAsync;
import comm.activemq.Point2PointConsumerblock;
import comm.activemq.Point2PointProducer;
import comm.activemq.TopicSubscriberAsync;

public class Controller extends Point2PointConsumerAsync{
	
	private String nodeID;
	public static final String nodeCfgFile = "config.node";
	private HashMap<String,BaseAgent> agentsMap ;	//Record the id and the corresponding Agent
	private List<BaseAgent> agentList;
	private Map<Vertex, String> AN_Table;	//A-N Table
	private Map<Vertex, List<Edge>> graph;
	private List<Vertex> vertexList;
	
	private List<Message> toBeSentMsgList ;		//to list of messages to be sent from Agents
	
	public static int sendNum = 10;
	
	private SendMsgThread[] sendMsgThread ;
	private Map<String, Point2PointProducer> producerMap ;
	private Point2PointProducer marketP;
	private int roundPerTick ;
	private int tickNum ;
	private int currentRound ;
	private Integer currentTick ;
	
	private Long orderMessages = 0L;	//order & trade
	private Long orderMessageSize = 0L;
	private Long controlMessages = 0L;
	private Long controlMessageSize = 0L;
	private Long communicationMessages = 0L;
	private Long communicationMessageSize = 0L;
	private Long innerMessages = 0L;
	private Long innerMessageSize = 0L;
	private Long interMessages = 0L;
	private Long interMessageSize = 0L;
	
	private Boolean canStartNextTick = false;
	public boolean threadEnd = false;
	private List<String> nodeIdList;
	private ThreadPoolExecutor agentThreadPool;
	
	public static double alpha = 0.001;
	public static double beita = 0.2;
	public static double gama = 0.00001;
	public static int m = 10;
	public static int a = 3;
	public static int b = 200;
	
	public static Boolean evolve = true;
	
	private TopicSubscriberAsync topicSub = new TopicSubscriberAsync( Market.marketTopicId ) {	//subscribe the msg from market
		@Override
		public void onException(JMSException arg0) {
			arg0.printStackTrace();
		}
		
		@Override
		public void consumeMessage(Message message) {
			
			try {
				String sender = message.getStringProperty(ProtocolString.F_SENDER);
				String type = message.getStringProperty( ProtocolString.F_TYPE );
//				System.out.println( nodeID + "      " + type );
				if( ProtocolString.TYPE_ANTABLE.equals(type) ){
					TextMessage txMsg = (TextMessage) message;
					String tableStr = txMsg.getText();
					
					String[] entrys = tableStr.split("#");
					for( int i = 0; i < entrys.length; i++ ){
						String entry = entrys[i];
						String[] tableItem = entry.split(";");
						Vertex ver = getVertexOfList( Integer.parseInt( tableItem[0] ) );
						AN_Table.put(ver, tableItem[1]);
						if( nodeID.equals( tableItem[1] ) ){
							BaseAgent agent = new AgentDemo( tableItem[0], Controller.this, ver );
							synchronized (agentList) {
								synchronized (agentsMap) {
									agentList.add(agent);
									agentsMap.put(agent.getAgentID(), agent);
								}
							}
						}
					}
					
					synchronized (controlMessages) {
						controlMessages++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += txMsg.getText().length()*2*8;
					}
					
					sendCurrentTickToMarket();
					
				}
				else if( ProtocolString.TYPE_MARKET_REFINED.equals(type) ){
					TextMessage txMsg = (TextMessage) message;
					String tableStr = txMsg.getText();
					
					try {
						String[] entrys = tableStr.split("#");
//						System.err.println("entrys.length: " + entrys.length );
						for( int i = 0; i < entrys.length; i++ ){
							String entry = entrys[i];
							String[] tableItem = entry.split(";");
							Vertex ver = getVertexOfList( Integer.parseInt( tableItem[0] ) );
							if( null != ver ){
								String newNodeId = tableItem[1].trim();
								
								String oldNodeId = null;
								synchronized (AN_Table) {
									oldNodeId = AN_Table.get(ver);
								}
									
								if( nodeID.equals(newNodeId) && !nodeID.equals(oldNodeId) ){
									BaseAgent agent = new AgentDemo( ver.vertexId+"", Controller.this, ver );
									synchronized (agentList) {
										synchronized (agentsMap) {
											agentList.add(agent);
											agentsMap.put(agent.getAgentID(), agent);
										}
									}
								}
								if( nodeID.equals(oldNodeId) && !nodeID.equals(newNodeId) ){
									synchronized (agentList) {
										synchronized (agentsMap) {
											BaseAgent agent = agentsMap.get( ver.vertexId+"" );
//											agentThreadPool.remove(agent);
											agentList.remove(agent);
											agentsMap.remove(agent.getAgentID());
										}
									}
								}
								synchronized (AN_Table) {
									AN_Table.put(ver, newNodeId);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace( System.out );
						System.out.println( AN_Table.size() +"   "+vertexList.size() );
					}
					
					synchronized (controlMessages) {
						controlMessages++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += txMsg.getText().length()*2*8;
					}
					
					TextMessage reFineMsg = MessageUtil.createTextMessage( marketP.getSession(), "");
					reFineMsg.setJMSPriority(ProtocolString.networkMsgPriority);
					reFineMsg.setStringProperty( ProtocolString.F_TYPE , ProtocolString.TYPE_Controller_REFINED);
					reFineMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
					reFineMsg.setStringProperty(ProtocolString.F_SENDER, nodeID);
					marketP.sendMessage(false, 0, reFineMsg);
					
				}
				else if( ProtocolString.TYPE_CONTROL.equals( type ) ){
					
					synchronized (controlMessages) {
						controlMessages++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += ( (TextMessage) message ).getText().length()*2*8;
					}
					
					String cmd = message.getStringProperty(ProtocolString.CONTROL_CMD);
					if( ProtocolString.CONTROL_CMD_NEXT_TICK.equals( cmd ) ){
						TextMessage txMsg = (TextMessage) message;
//						System.out.println( nodeID+" ddddddddddd  nextTick   "+ txMsg.getText() );
						Integer cu = Integer.parseInt( txMsg.getText() );
//						System.out.println( cu );
						synchronized (currentTick) {
							currentTick = cu;
						}
						synchronized (canStartNextTick) {
							canStartNextTick = true;
						}
//						System.out.println( nodeID+ " received "+currentTick );
					}
					else if( ProtocolString.CONTROL_CMD_Add_VERTEX.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();

//						System.err.println( agentList.size() );
						
						String[] verStr = msgBody.split(",");
						String nodeId = verStr[0];
						for( int i = 1; i < verStr.length; i++ ){
							Vertex ver = new Vertex();
							ver.vertexId = Integer.parseInt( verStr[i].trim() );
							ver.vertexWeight = 3;
							synchronized (vertexList) {
								vertexList.add(ver);
							}
							if( nodeID.equals( nodeId ) ){
								BaseAgent agent = new AgentDemo(verStr[i], Controller.this, ver);
								synchronized (agentList) {
									synchronized (agentsMap) {
										agentList.add(agent);
										agentsMap.put( verStr[i].trim() , agent);
									}
								}
							}
							AN_Table.put(ver, nodeId);
						}
						
//						System.err.println("after:"+ agentList.size() );
						
						/*
						//						System.out.println(txMsg.getText());
						String[] splitS = msgBody.split("#");
						Vertex ver = new Vertex();
						List<Edge> edgeList = new ArrayList<Edge>();
						String nodeId = splitS[0].split(",")[0].trim();
						synchronized (graph) {
							synchronized (vertexList) {
								ver.vertexId = Integer.parseInt( splitS[0].split(",")[1].trim() );
								vertexList.add(ver);
								graph.put(ver, edgeList);
							}
						}
						
						ver.vertexWeight = Double.parseDouble( splitS[0].split(",")[2].trim() );
						
						if( splitS.length > 1 && null != splitS[1] ){
							String[] edges = splitS[1].split(";");
							for( int i = 0; i < edges.length; i++ ){
								String[] edge = edges[i].split(",");

								Vertex end = getVertexOfList( Integer.parseInt( edge[0] ) );
								Edge ed = new Edge();
								ed.startPoint = ver;
								ed.endPoint = end;
								ed.edgeweight = Double.parseDouble( edge[1] );
								
								edgeList.add( ed );
								
								List<Edge> inEdgeList = graph.get(end);
								Edge edd = new Edge();
								edd.startPoint = end;
								edd.endPoint = ver;
								edd.edgeweight = ed.edgeweight;
								
								inEdgeList.add( edd );
							}
						}
						synchronized (graph) {
							graph.put(ver, edgeList);
						}
						synchronized (AN_Table) {
							AN_Table.put(ver, nodeId);
						}
						
//						System.err.println( nodeID+"   add vertex:"+msgBody );
						
						if( Controller.this.nodeID.equals(nodeId) ){
							BaseAgent agent = new AgentDemo(ver.vertexId+"", Controller.this, ver);
							synchronized (agentList) {
								synchronized (agentsMap) {
									agentList.add(agent);
									agentsMap.put(agent.getAgentID(), agent);
								}
							}
						}
						*/
					}
					else if( ProtocolString.CONTROL_CMD_ADD_LINK.equals( cmd ) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						String[] splitS = msgBody.split(",");
						int startPoint = Integer.parseInt( splitS[0].trim() );
						int endPoint = Integer.parseInt( splitS[1].trim() );
						double edgeWeight = Double.parseDouble( splitS[2].trim() );
						if( startPoint != endPoint ){
							Vertex ver = getVertexOfList( startPoint );
							Vertex endVer = getVertexOfList( endPoint );
							
							if( null != ver && null != endVer){
								List<Edge> edgeList = null;
								synchronized (graph) {
									edgeList = graph.get(ver);
								}
								
								Edge edge = new Edge();
								edge.startPoint = ver;
								edge.endPoint = endVer;
								edge.edgeweight = edgeWeight;
								edgeList.add(edge);
								
								List<Edge> endEdgeList = null;
								synchronized (graph) {
									endEdgeList = graph.get(endVer);
								}
								
								Edge edd = new Edge();
								edd.startPoint = endVer;
								edd.endPoint = ver;
								edd.edgeweight = edgeWeight;
								endEdgeList.add(edd);
							}
						}
						
					}
					else if( ProtocolString.CONTROL_CMD_DELETE_VERTEX.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						
						int vertexNum = Integer.parseInt( msgBody.trim() );
						Vertex v = getVertexOfList( vertexNum );
						synchronized ( vertexList ) {
							vertexList.remove(v);
						}
						
						synchronized (graph) {
							graph.remove(v);
							Iterator<Vertex> verI = graph.keySet().iterator();
							while( verI.hasNext() ){
								List<Edge> inEdgeList = graph.get( verI.next() );
								for( int i = 0; i < inEdgeList.size(); i++ ){
									Edge edge = inEdgeList.get(i);
									if( v.vertexId == edge.endPoint.vertexId  ){
										inEdgeList.remove(i);
										break;
									}
								}
							}
						}
						
						String oldNodeId = null;
						synchronized (AN_Table) {
							oldNodeId = AN_Table.get( v );
						}
						synchronized (AN_Table) {
							AN_Table.remove(v);
						}
						
//						System.err.println( nodeID+"    delete vertex:"+msgBody+"   "+oldNodeId+"   "+vertexNum );
						
						if( nodeID.equals( oldNodeId ) ){
							synchronized (agentList) {
								synchronized (agentsMap) {
									BaseAgent agent = agentsMap.get( vertexNum+"" );
									agentThreadPool.remove(agent);
									agentList.remove(agent);
									agentsMap.remove( vertexNum+"" );
								}
							}
						}
						
					}
					else if( ProtocolString.CONTROL_CMD_DELETE_LINK.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						int end = Integer.parseInt( msgBody.split(",")[1].trim() );
						Vertex ver = getVertexOfList(start);
						Vertex endVertex = getVertexOfList( end );
						
						List<Edge> edgeList = null;
						synchronized (graph) {
							edgeList = graph.get(ver);
						}
						for( int i = 0; i < edgeList.size(); i++ ){
							if( edgeList.get(i).endPoint.vertexId == end ){
								edgeList.remove(i);
								break;
							}
						}
						synchronized (graph) {
							edgeList = graph.get( endVertex );
						}
						for( int i = 0; i < edgeList.size(); i++ ){
							if( edgeList.get(i).endPoint.vertexId == start ){
								edgeList.remove(i);
								break;
							}
						}
						
					}
					else if( ProtocolString.CONTROL_CMD_CHANGE_LINK_WEIGHT.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						int end = Integer.parseInt( msgBody.split(",")[1].trim() );
						double weight = Double.parseDouble( msgBody.split(",")[2].trim() );
						
						Vertex ver = getVertexOfList(start);
						Vertex endVer = getVertexOfList(end);
						
						List<Edge> edgeList = null;
						synchronized (graph) {
							edgeList = graph.get(ver);
						}
						for( int i = 0; i < edgeList.size(); i++ ){
							Edge edge = edgeList.get(i);
							if( edge.endPoint.vertexId == end ){
								edge.edgeweight = weight;
								break;
							}
						}
						
						synchronized (graph) {
							edgeList = graph.get(endVer);
						}
						for( int i = 0; i < edgeList.size(); i++ ){
							Edge edge = edgeList.get(i);
							if( edge.endPoint.vertexId == start ){
								edge.edgeweight = weight;
								break;
							}
						}
					}
					else if( ProtocolString.CONTROL_CMD_CHANGE_VERTEX_WEIGHT.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						double weight = Double.parseDouble( msgBody.split(",")[1].trim() );
						Vertex ver = getVertexOfList(start);
						ver.vertexWeight = weight;
					}
				}
				else if( ProtocolString.TYPE_DESTROY.equals(type) ){
					
					synchronized (controlMessages) {
						controlMessages++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += ( (TextMessage) message ).getText().length()*2*8;
					}
					
					System.out.println( "controlMessages:          "+controlMessages );
					System.out.println( "orderMessages:            "+orderMessages );
					System.out.println( "controlMessageSize:       "+controlMessageSize );
					System.out.println( "orderMessageSize:         "+orderMessageSize );
					System.out.println( "communicationMessages:    "+communicationMessages );
					System.out.println( "communicationMessageSize: "+communicationMessageSize);
					System.out.println( "innerMessages:            "+innerMessages);
					System.out.println( "innerMessageSize:         "+innerMessageSize);
					System.out.println( "interMessages:            "+interMessages);
					System.out.println( "interMessageSize:         "+interMessageSize);
					
					System.out.close();
					
					Controller.this.destroy();
				}
				
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	};
	
	
	private Vertex getVertexOfList(int vertexId){
		Vertex v = new Vertex();
		v.vertexId = vertexId;
		synchronized (vertexList) {
			int index = vertexList.indexOf(v);
			if( -1 == index )
				return null;
			return vertexList.get(index);
		}
	}
	
	/*---CONSTRUCTOR---*/
	public Controller( String nodeID, Boolean transacted, int ackMode, int roundPerTick, int tickNum, List<String> nodeIdList ) throws JMSException{
		super(transacted, ackMode, nodeID);
		init(roundPerTick, tickNum, nodeIdList);
	}
	
	protected void sendCurrentTickToMarket() {
		try {
			
			TextMessage startMsg = MessageUtil.createTextMessage( marketP.getSession(), this.currentTick +"" );
			startMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
			startMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_CURRENT_TICK);
			startMsg.setStringProperty(ProtocolString.F_SENDER, Controller.this.nodeID);
			startMsg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
			
			System.out.println(nodeID+"   send   "+ currentTick +"   time:"+System.currentTimeMillis() );
			
			sendMsgOut(false, 0, startMsg);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
	}

	public Controller( String nodeID, int roundPerTick, int tickNum, List<String> nodeIdList) throws JMSException{
		super(nodeID);
		init(roundPerTick, tickNum, nodeIdList);
	}
	
	private void init(int roundPerTick, int tickNum, List<String> nodeIdList) throws JMSException{
		this.roundPerTick = roundPerTick;
		this.tickNum = tickNum;
		this.currentRound = 0;
		this.currentTick = -1;
		agentsMap = new HashMap<String,BaseAgent>();
		this.graph = new HashMap<Vertex, List<Edge>>();
		AN_Table = new HashMap<Vertex, String>();
		toBeSentMsgList = new LinkedList<Message>();
		this.nodeIdList = nodeIdList;
		this.nodeID = nodeIdList.get(0);
		producerMap = new HashMap<String, Point2PointProducer>();
		for( int i = 0; i < nodeIdList.size(); i++ ){
			String nodeId = nodeIdList.get(i);
			Point2PointProducer p = new Point2PointProducer(nodeId);
			producerMap.put(nodeId, p);
		}
		marketP = new Point2PointProducer(Market.nodeID);
		
		sendMsgThread = new SendMsgThread[sendNum];
		for( int i = 0; i < sendNum; i++ ){
			sendMsgThread[i] = new SendMsgThread(toBeSentMsgList);
		}
		this.agentList = new ArrayList<BaseAgent>();
		
		this.agentThreadPool = new ThreadPoolExecutor(ProtocolString.threadNum, ProtocolString.threadNum, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		for( int i = 0; i < sendNum; i++ )
			sendMsgThread[i].start();		//start the sending thread
		
		try {
			System.setOut( new PrintStream(new File("contrlOut")) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public void sendMsg(Message msg) throws JMSException{
		synchronized (toBeSentMsgList) {
			toBeSentMsgList.add(msg);
		}
	}
	
	public void sendMsgPro(Message msg) throws JMSException{
		synchronized (toBeSentMsgList) {
			toBeSentMsgList.add(0, msg);
		}
	}
	
	public Point2PointProducer getMarketP() {
		return marketP;
	}

	public List<Edge> getEdgeList( int vertexId ){
		Vertex v = new Vertex();
		v.vertexId = vertexId;
		List<Edge> edgeList = null;
		synchronized (graph) {
			edgeList = graph.get(v);
		}
		return edgeList;
	}
	
	public int getCurrentRound() {
		return currentRound;
	}

	public Integer getCurrentTick() {
		return currentTick;
	}
	
	public List<Edge> getEdgeListOfAgent( String agentId ){
		int aId = Integer.parseInt( agentId );
		Vertex ver = new Vertex();
		ver.vertexId = aId;
		List<Edge> edgeList = null;
		synchronized (graph) {
			edgeList = graph.get(ver);
		}
		return edgeList;
	}
	
	/**
	 * @param nodeID -- target node id
	 * @param persistent -- persistent or not
	 * @param timeToLive
	 * @param msg -- message to send
	 * @throws JMSException
	 */
	private void sendMsgOut( Boolean persistent, long timeToLive, Message msg) throws JMSException{
		if( null != msg ){
			String receiver = msg.getStringProperty(ProtocolString.F_RECIVER);
//			String sender = msg.getStringProperty(ProtocolString.F_SENDER);
			if( null != receiver ){
				String queueId = null;
				if( Market.nodeID.equals( receiver ) ){
					queueId = Market.nodeID;
					marketP.sendMessage( persistent, timeToLive, msg );
					return;
				}
				else{
					Vertex ver = new Vertex();
					ver.vertexId = Integer.parseInt(receiver);
					synchronized (AN_Table) {
						queueId = AN_Table.get( ver );
					}
					
					if( null == queueId ){
						System.out.println("\n\n receiver  "+  receiver  +"  \n\n");
						synchronized ( graph ) {
							graph.remove( ver );
						}
						return ;
					}
				}
				if( !queueId.equals(nodeID) ){
					Point2PointProducer p = producerMap.get(queueId);
					if( null != p )
						p.sendMessage( persistent, timeToLive, msg );
				}
				else{
					synchronized (communicationMessages) {
						communicationMessages ++;
					}
					synchronized (communicationMessageSize) {
						communicationMessageSize += ( (TextMessage)msg ).getText().length()*2*8;
					}
					synchronized (innerMessages) {
						innerMessages ++;
					}
					synchronized (innerMessageSize) {
						innerMessageSize += ( (TextMessage)(msg) ).getText().length() *2*8;
					}
					BaseAgent ba = getAgent(receiver);
					if( null != ba )
						ba.receiveMsg(msg);
				}
			}
		}
	}
	
	@Override
	public void consumeMessage(Message message) {
		try {
			String receiver = message.getStringProperty(ProtocolString.F_RECIVER);
			String sender = message.getStringProperty(ProtocolString.F_SENDER);
			String type = message.getStringProperty(ProtocolString.F_TYPE);
			
			if( ProtocolString.TYPE_ORDER.equals( type ) ){
				Order or = (Order)( (ObjectMessage)message ).getObject();
				synchronized (orderMessages) {
					orderMessages ++;
				}
				synchronized (orderMessageSize) {
					orderMessageSize += or.getAgentId().length()*2*8 + 3*32+2*64;
				}
//				synchronized (interMessageSize) {
//					interMessageSize += or.getAgentId().length()*2*8 + 3*32+2*64;
//				}
			}
			else if( ProtocolString.TYPE_TRADE.equals(type) ){
				Trade tra = (Trade)( (ObjectMessage)message ).getObject();
				synchronized (orderMessages) {
					orderMessages ++;
				}
				synchronized (orderMessageSize) {
					orderMessageSize += tra.getBuyId().length()*2*8 + tra.getSellId().length()*2*8+3*32+2*64;
				}
//				synchronized (interMessageSize) {
//					interMessageSize += tra.getBuyId().length()*2*8 + tra.getSellId().length()*2*8+3*32+2*64;
//				}
			}
			else if( ProtocolString.TYPE_CONTROL.equals(type) ){
				synchronized ( controlMessages ) {
					controlMessages ++;
				}
				synchronized (controlMessageSize) {
					controlMessageSize += ( (TextMessage)message ).getText().length()*2*8;
				}
//				synchronized (interMessageSize) {
//					interMessageSize += ( (TextMessage)message ).getText().length()*2*8;
//				}
			}
			else if( ProtocolString.TYPE_COMMUNICATION.equals(type) ){
				synchronized (communicationMessages) {
					communicationMessages ++;
				}
				synchronized (communicationMessageSize) {
					communicationMessageSize += ( (TextMessage)message ).getText().length()*2*8;
				}
				if( !nodeID.equals(sender) && !Market.nodeID.equals(sender) ){
					synchronized (interMessages) {
						interMessages ++;
					}
					synchronized (interMessageSize) {
						interMessageSize += ( (TextMessage)message ).getText().length()*2*8;
					}
				}
				else{
					
				}
			}
			else{
				synchronized (communicationMessages) {
					communicationMessages ++;
				}
				synchronized (communicationMessageSize) {
					communicationMessageSize += ( (TextMessage)message ).getText().length()*2*8;
				}
				if( !nodeID.equals(sender) && !Market.nodeID.equals(sender) ){
					synchronized (interMessages) {
						interMessages ++;
					}
					synchronized (interMessageSize) {
						interMessageSize += ( (TextMessage)message ).getText().length()*2*8;
					}
				}
				else if( nodeID.equals(sender) ){
					synchronized (innerMessages) {
						innerMessages++;
					}
					synchronized (innerMessageSize) {
						innerMessageSize += ( (TextMessage)message ).getText().length()*2*8;
					}
				}
			}
			
			BaseAgent receAgent = null;
			synchronized ( agentsMap ) {
				receAgent = agentsMap.get(receiver);
			}
			if( null != receAgent ){
				receAgent.receiveMsg(message);
			}
			else{
				String receiveNode = null;
				synchronized (AN_Table) {
					receiveNode = AN_Table.get(receiver); 
				}
				if(receiveNode!=null)
					sendMsg(message);
			}
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void destroy() {
		
		synchronized (graph) {
			this.graph.clear();
		}
		synchronized (agentList) {
			synchronized ( agentsMap ) {
				this.agentList.clear();
				agentsMap.clear();
			}
		}
		synchronized (toBeSentMsgList) {
			this.toBeSentMsgList.clear();
		}
		
		synchronized (AN_Table) {
			AN_Table.clear();
		}
		
		
//		this.sendMsgThread.destroy();
		threadEnd = true;
		
		this.agentThreadPool.shutdown();
		while( agentThreadPool.getActiveCount() != 0 );
		
		marketP.destroy();
		for( int i = 0; i < nodeIdList.size(); i++ ){
			Point2PointProducer p = producerMap.get( nodeIdList.get(i) );
			p.destroy();
		}
		producerMap.clear();
		this.nodeIdList.clear();
		
		super.destroy();
		this.topicSub.destroy();
	}
	
	public BaseAgent getAgent(String agentID){
		synchronized ( agentsMap ) {
			return agentsMap.get(agentID);
		}
	}
	
	public String getNodeID(){
		return nodeID;
	}
	
	public Point2PointProducer getMsgProducer( String nodeId ) {
		if( Market.nodeID.equals( nodeId ) )
			return marketP;
		Point2PointProducer pd = null;
		synchronized (producerMap) {
			pd = producerMap.get(nodeId);
		}
		return pd;
	}
	
	public Map<Vertex, String> getAN_Table() {
		return AN_Table;
	}

	public void setAN_Table(Map<Vertex, String> aN_Table) {
		AN_Table = aN_Table;
	}
	
	public int getRoundPerTick() {
		return roundPerTick;
	}

	public void setRoundPerTick(int roundPerTick) {
		this.roundPerTick = roundPerTick;
	}

	public int getTickNum() {
		return tickNum;
	}

	public void setTickNum(int tickNum) {
		this.tickNum = tickNum;
	}
	
	private long start = 0;
	private long end = 0;
	private Random ran = new Random();
	public void startNode(String graphFileName){
//		System.out.println(" node "+nodeID +" start ");
		
//		try {
//			TextMessage startMsg = MessageUtil.createTextMessage( msgProducer.getSession(), this.currentTick +"" );
//			startMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
//			
//		} catch (JMSException e) {
//			e.printStackTrace();
//		}
		
//		System.out.println( System.currentTimeMillis() );
		
		this.vertexList = Partition.readGraph(graphFileName, graph);
		
		long tickStart = System.currentTimeMillis();
		long tickEnd = 0;
		
		for( int i = 0; i < tickNum; i++ ){
//			System.out.println( nodeID +"     "+currentTick);
			
			synchronized (currentTick) {
				if( currentTick >= tickNum )
					break;
			}
			
			while( true ){
				synchronized (canStartNextTick) {
					if( canStartNextTick )
						break;
				}
			}
			
//			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick start" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
			
			for( int j = 0; j < roundPerTick; j++ ){	//此地逻辑有些问题，只能所有的agent都运行完第一round之后才开始运行第二个round
				
//				tickEnd = System.currentTimeMillis();
//				System.out.println("tickticktickticktickticktick 1:" + ( tickEnd - tickStart ) );
//				tickStart = tickEnd;
				
				List<BaseAgent> ranAgents = null;
				synchronized (agentList) {
					ranAgents = shuffle(this.agentList);
					for( int k = 0; k < ranAgents.size(); k++ ){
						agentThreadPool.execute( ranAgents.get(k) );
					}
				}
				ranAgents.clear();
				
//				tickEnd = System.currentTimeMillis();
//				System.out.println("tickticktickticktickticktick 2:" + ( tickEnd - tickStart ) );
//				tickStart = tickEnd;
				
				if( Controller.evolve ){
					
					/*
					if( this.currentTick % 20 == 10 ){
						System.err.println("tick:"+currentTick+"  nodeID:"+nodeID);
						if( "node98".equals(nodeID) ){
							StringBuilder addStr = new StringBuilder();
							int sum = 0;
							for( int v = 0; sum <= m && v < vertexList.size(); v++ ){
								if( ran.nextInt(4) == 0 ){
									Vertex ver = getVertexOfList( v ) ;
									String nodeId = AN_Table.get( ver );
									if( "node96".equals(nodeId) ){
										sum ++;
										if( addStr.length() == 0 )
											addStr.append( ver.vertexId +",100" );
										else
											addStr.append( ";"+ ver.vertexId +",100" );
									}
								}
							}
							sendNetworkMsgToMarket( "2#"+addStr, ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID );
						}
						
						if( "node99".equals(nodeID) ){
							StringBuilder addStr = new StringBuilder();
							int sum = 0;
							for( int v = 0; sum <= m && v < vertexList.size(); v++ ){
								if( ran.nextInt(4) == 0 ){
									Vertex ver = getVertexOfList( v ) ;
									String nodeId = AN_Table.get( ver );
									if( "node97".equals(nodeId) ){
										sum ++;
										if( addStr.length() == 0 )
											addStr.append( ver.vertexId +",100" );
										else
											addStr.append( ";"+ ver.vertexId +",100" );
									}
								}
							}
							sendNetworkMsgToMarket( "2#"+addStr, ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID );
						}
						
						if( "node96".equals(nodeID) ){
							StringBuilder addStr = new StringBuilder();
							int sum = 0;
							for( int v = 0; sum <= m && v < vertexList.size(); v++ ){
								if( ran.nextInt(4) == 0 ){
									Vertex ver = getVertexOfList( v ) ;
									String nodeId = AN_Table.get( ver );
									if( "node99".equals(nodeId) ){
										sum ++;
										if( addStr.length() == 0 )
											addStr.append( ver.vertexId +",100" );
										else
											addStr.append( ";"+ ver.vertexId +",100" );
									}
								}
							}
							sendNetworkMsgToMarket( "2#"+addStr, ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID );
						}
						
						if( "node97".equals(nodeID) ){
							StringBuilder addStr = new StringBuilder();
							int sum = 0;
							for( int v = 0; sum <= m && v < vertexList.size(); v++ ){
								if( ran.nextInt(4) == 0 ){
									Vertex ver = getVertexOfList( v ) ;
									String nodeId = AN_Table.get( ver );
									if( "node98".equals(nodeId) ){
										sum ++;
										if( addStr.length() == 0 )
											addStr.append( ver.vertexId +",100" );
										else
											addStr.append( ";"+ ver.vertexId +",100" );
									}
								}
							}
							sendNetworkMsgToMarket( "2#"+addStr, ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID );
						}
					}
					*/
					
					if( this.currentTick % 20 == 10 ){
						if( "node98".equals(nodeID) || "node99".equals(nodeID) ){
							sendNetworkMsgToMarket( "3#10,100", ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID );
						
						}
						
//						if( "node96".equals(nodeID) || "node97".equals(nodeID) ){
//							for( int k = 0; k < ProtocolString.rate; k++ ){
//								BaseAgent deAgent = null;
//								synchronized (agentList) {
//									deAgent = agentList.get( this.currentTick );
//								}
//								if( null != deAgent ){
//									sendNetworkMsgToMarket(deAgent.getAgentID(), ProtocolString.CONTROL_CMD_DELETE_VERTEX, nodeID);
//								}
//							}
//						}
					}
					
					/*
					synchronized (agentList) {
						for( int a = 1; a <= 5; a++ ){
							BaseAgent agent = agentList.get( this.currentTick + a*10 );
							if( null != agent ){
								synchronized (graph) {
									List<Edge> edgeList = graph.get( agent.getVertex() );
									if( null != edgeList && edgeList.size() > 0 ){
										Edge edge = edgeList.get(0);
										sendNetworkMsgToMarket(edge.startPoint.vertexId+","+edge.endPoint.vertexId, ProtocolString.CONTROL_CMD_DELETE_LINK, nodeID);
									}
								}
							}
						}
					}
					*/
					
					/*
					if( Math.random() < 0.3 ){
						StringBuilder addStr = new StringBuilder();
						for( int v = 0;v < m; v++ ){
							int ii = ran.nextInt( agentList.size() );
							if( v == 0 )
								addStr.append(ii+","+ (ran.nextInt(b)+1) );
							else
								addStr.append(";"+ii+","+ (ran.nextInt(b)+1) );
						}
						sendNetworkMsgToMarket( (ran.nextInt(a)+1)+"#"+addStr, ProtocolString.CONTROL_CMD_Add_VERTEX, nodeID);
						
						int index = 0;
						BaseAgent deAgent = null;
						synchronized (agentList) {
							index = ran.nextInt( agentList.size() );
							deAgent = agentList.get(index);
						}
						if( null != deAgent ){
							sendNetworkMsgToMarket(deAgent.getAgentID(), ProtocolString.CONTROL_CMD_DELETE_VERTEX, nodeID);
						}
					}
					
					synchronized (agentList) {
						for( int v = 0; v < agentList.size(); v++ ){
							BaseAgent agent = agentList.get(v);
							if( Math.random() <= gama ){
								sendNetworkMsgToMarket(agent.getAgentID()+","+(ran.nextInt(a)+1), ProtocolString.CONTROL_CMD_CHANGE_VERTEX_WEIGHT, nodeID);
							}
							List<Edge> edgeList = null;
							synchronized (graph) {
								edgeList = graph.get( agent.getVertex() );
							}
							if( null != edgeList ){
								for( int k = 0; k < edgeList.size(); k++ ){
									Edge edge = edgeList.get(k);
									if( null != edge && Math.random() <= alpha ){
										if( Math.random() <= beita ){
											sendNetworkMsgToMarket(edge.startPoint.vertexId+","+edge.endPoint.vertexId, ProtocolString.CONTROL_CMD_DELETE_LINK, nodeID);
										}
										else{
											sendNetworkMsgToMarket(edge.startPoint.vertexId+","+edge.endPoint.vertexId+","+( ran.nextInt(b)+1 ), ProtocolString.CONTROL_CMD_CHANGE_LINK_WEIGHT, nodeID);
										}
									}
								}
							}
						}
					}
					*/
				}
				
//				final Iterator<String> it = agentsMap.keySet().iterator();
//				while( it.hasNext() ){
//					final String nn = it.next();
//					new Thread(){
//						public void run() {
//							synchronized (it) {
//								agentsMap.get( nn ).runningLogic();
//							}
//						}
//					}.start();
//				}
			}
			
//			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick 3:" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
			
			while( agentThreadPool.getActiveCount() != 0 );
			
			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick 4:" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
			
//			int inner = 0;
//			int inter = 0;
//			int nul = 0;
//			synchronized (toBeSentMsgList) {
//				for( int t = 0; t < toBeSentMsgList.size() ; t++ ){
//					Message msg = toBeSentMsgList.get(t);
//					if( null != msg ){
//						try {
//							String re = msg.getStringProperty( ProtocolString.F_RECIVER );
//							if( Market.nodeID.equals( re ) )
//								inter ++;
//							else{
//								Vertex ver = new Vertex();
//								ver.vertexId = Integer.parseInt( re );
//								String queId = null;
//								synchronized (AN_Table) {
//									queId = AN_Table.get( ver );
//								}
//								if( null == queId )
//									nul ++;
//								else if( nodeID.equals(queId) )
//									inner ++;
//								else 
//									inter ++;
//							}
//						} catch (JMSException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//			System.out.println( "currentTick:" +currentTick+ "  inner:"+inner);
//			System.out.println( "currentTick:" +currentTick+ "  inter:"+inter);
//			System.out.println( "currentTick:" +currentTick+ "  null:"+nul);
			
//			end = System.currentTimeMillis();
//			System.out.println("calculating the messages time:"+  (end - start));
//			start = end;
//			System.out.println( "currentTick:  " +currentTick+"  " + toBeSentMsgList.size() + " messages to be sent ");
//			
			while ( true ) {
				synchronized (toBeSentMsgList) {
					if( toBeSentMsgList.isEmpty() ){
						break;
					}
				}
			}
			
//			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick 5:" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
			
			sendCurrentTickToMarket();
			
//			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick 6:" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
			
			synchronized (canStartNextTick) {
				canStartNextTick = false;
			}
			
//			tickEnd = System.currentTimeMillis();
//			System.out.println("tickticktickticktickticktick 7:" + ( tickEnd - tickStart ) );
//			tickStart = tickEnd;
		}
	}
	
	public void sendNetworkMsgToMarket(String msgBody, String cmdType, String senderNodeId){
		try {
			
//			System.err.println( msgBody + "\n  type:"+cmdType+"  nodeId:"+senderNodeId );
			
			TextMessage msg = MessageUtil.createTextMessage( marketP.getSession(), msgBody );
			msg.setJMSPriority( ProtocolString.networkMsgPriority );
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
			msg.setStringProperty( ProtocolString.CONTROL_CMD, cmdType );
			msg.setStringProperty( ProtocolString.F_SENDER , senderNodeId );
			msg.setStringProperty(ProtocolString.F_RECIVER, Market.nodeID);
			
			marketP.sendMessage(false, 0, msg);
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	private List<BaseAgent> shuffle( List<BaseAgent> agentList ){
		List<BaseAgent> temp = new LinkedList<BaseAgent>();
		temp.addAll(agentList);
		List<BaseAgent> result = new ArrayList<BaseAgent>();
		Random ran = new Random( ProtocolString.seed );
		while( !temp.isEmpty() ){
			int index = ran.nextInt( temp.size() );
			result.add( temp.get(index) );
			temp.remove(index);
		}
		return result;
	}
	
	class ReceiveMsgThread extends Thread{
		private Point2PointConsumerblock consumer;
		public ReceiveMsgThread( Point2PointConsumerblock consumer ) {
			this.consumer = consumer;
		}
		@Override
		public void run() {
			super.run();
			while (true) {
				try {
					Message msg = this.consumer.receiveMsg(5);
					if( null != msg ){
						consumeMessage(msg);
					}
				} catch (JMSException e) {
					e.printStackTrace();
				}
				
//				synchronized (threadEnd) {
					if( threadEnd ){
						this.consumer.destroy();
						break;
					}
//				}
			}
		}
	}
	
	class SendMsgThread extends Thread{
		
//		private List<Message> toBeSentMsgList; 
		
		public SendMsgThread( List<Message> toBeSentMsgList ) {
			if ( null == toBeSentMsgList )
				toBeSentMsgList = new LinkedList<Message>();
//			this.toBeSentMsgList = toBeSentMsgList;
		}
		
		@Override
		public void run() {
			super.run();
			Message msg = null;
			while( true ){
				synchronized (toBeSentMsgList) {
					if( toBeSentMsgList.size() > 0 ){
						msg = toBeSentMsgList.get(0);
						toBeSentMsgList.remove(0);
					}
					else{
						msg = null;
					}
				}
				if( null != msg ){
					try {
						sendMsgOut( false, 0, msg );
						
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
				if( threadEnd )
					break;
			}
//			System.out.println("thread  "+  hashCode() +"  stop" );
		}
	}

	@Override
	public void onException(JMSException arg0) {
		arg0.printStackTrace();
	}
}