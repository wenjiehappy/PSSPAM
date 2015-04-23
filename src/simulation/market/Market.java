package simulation.market;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import simulation.network.Edge;
import simulation.network.Partition;
import simulation.network.Vertex;
import simulation.order.Order;
import simulation.order.Trade;
import comm.activemq.MessageUtil;
import comm.activemq.Point2PointConsumerAsync;
import comm.activemq.Point2PointProducer;
import comm.activemq.TopicPublisher;

public class Market extends Point2PointConsumerAsync {
	
//	public static final String marketConfigFileName = "/home/shawn/program/java/PSSPAM/config.market";
	public static final String marketConfigFileName = "config.market";
	public static double percentage = 0;
	public static double loadImbalance = 1.03;
	private String graphFilePath;
	private List<Message> receivedOrderList ;
	private static Stock tradeStock = new Stock();
	public static String  partitionParams;
	private Long orderMessages = 0L;
	private Long orderMessageSize = 0L;
	private Long controlMessages = 0L;
	private Long controlMessageSize = 0L;
	
//	private TradeBuffer tradeBuffer ;
	
	private TopicPublisher topicPub ;
	private Map<Vertex, String> AN_Table;	//A-N Table
	private Map<Vertex, List<Edge>> graph;
	private List<Vertex> vertexList;
	
	private List<String> nodeIdList ;
	
	private Map<String, Point2PointProducer> producerMap ;
	
	public static final String nodeID = "#" ;
	public static final String marketTopicId = "##" ;
	
	private Set<Vertex> changed = new HashSet<Vertex>();
	
	private int roundPerTick ;
	private int tickNum ;
	private int currentRound ;
	private Integer currentTick ;
	
	private Map<String,Integer> nodeCurrentTick;
	
	private DeelOrderThread[] deelOrderThreads = new DeelOrderThread[ ProtocolString.threadNum ] ;
	public Boolean threadEnd = false;
	
	private Map<String, Double> weightOfNode = new HashMap<String, Double>();
	private Double totalWeight = 0D;
	
	private Integer syNum = 0;
	
	public static Boolean multiLevelPar = true; 
	public static Boolean refinePar = true;
	
	public Market( int roundPerTick, int tickNum, String graphFilePath, List<String> nodeIdList ) throws JMSException {
		super(nodeID);
		init(roundPerTick, tickNum, graphFilePath, nodeIdList);
	}
	
	public Market( Boolean transacted, int ackMode, int roundPerTick, int tickNum, String graphFilePath, List<String> nodeIdList) throws JMSException {
		super(transacted, ackMode, nodeID);
		init(roundPerTick, tickNum,graphFilePath, nodeIdList);
	}
	
	public void init( int roundPerTick, int tickNum, String graphFilePath, List<String> nodeIdList ) throws JMSException{
		
		this.roundPerTick = roundPerTick;
		this.tickNum = tickNum;
		this.receivedOrderList = new LinkedList<Message>();
		this.graph = new HashMap<Vertex, List<Edge>>();
		this.nodeCurrentTick = new HashMap<String,Integer>();
		this.AN_Table = new HashMap<Vertex, String>();
		this.nodeIdList = nodeIdList;
		this.graphFilePath = graphFilePath;
		this.currentTick = -1;
		
		producerMap = new HashMap<String, Point2PointProducer>();
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			String nodeId = nodeIdList.get(i);
			nodeCurrentTick.put( nodeId, currentTick - 1 );
			Point2PointProducer p = new Point2PointProducer( nodeId );
			producerMap.put(nodeId, p);
		}
		
		topicPub = new TopicPublisher( Market.marketTopicId );
		
		for( int i = 0; i < ProtocolString.threadNum; i++ ){
			deelOrderThreads[i] = new DeelOrderThread();
			deelOrderThreads[i].start();
		}
		
		this.producerMap = new HashMap<String, Point2PointProducer>();
//		this.tradeBuffer = new TradeBuffer();
		
		try {
			System.setOut( new PrintStream(new File("out")) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getNodeIdList() {
		return nodeIdList;
	}
	
	private long total = 0;
	
	
	private long initTime = 0;
	@Override
	public void consumeMessage(Message message) {
		try {
			String type = message.getStringProperty(ProtocolString.F_TYPE);
			if( null != type ){
				if( ProtocolString.TYPE_Controller_REFINED.equals( type ) ){
					
					synchronized (controlMessages) {
						controlMessages++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += ( (TextMessage)message ).getText().length()*2*8;
					}
					
					synchronized (syNum) {
						syNum ++;
//						System.err.println("syNum:"+syNum+" nodeId:"+message.getStringProperty(ProtocolString.F_SENDER));
						if( syNum < nodeIdList.size() )
							return ;
					}
					synchronized (syNum) {
						syNum = 0;
					}
					
//					System.err.println("INdirect next:"+currentTick);
					
					TextMessage contrlMsg = MessageUtil.createTextMessage(topicPub.getSession(), "");
					contrlMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
					contrlMsg.setText(this.currentTick+"");
					contrlMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
					contrlMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_NEXT_TICK);
					contrlMsg.setJMSPriority( ProtocolString.synchronizationMsgPriority );
					topicPub.publishMsg(false, 0, contrlMsg);
					
				}
				else if( ProtocolString.TYPE_CONTROL.equals(type) ){
					
					synchronized (controlMessages) {
						controlMessages ++;
					}
					synchronized (controlMessageSize) {
						controlMessageSize += ( (TextMessage)message ).getText().length()*2*8;
					}
					//TODO
					String cmd = message.getStringProperty(ProtocolString.CONTROL_CMD);
					if( ProtocolString.CONTROL_CMD_CURRENT_TICK.equals(cmd) ){
//						System.err.println(nodeID +"  market received  "+ message.getStringProperty(ProtocolString.F_SENDER) +"  "+currentTick);
						String sender = message.getStringProperty( ProtocolString.F_SENDER );
						TextMessage txMsg = (TextMessage) message;
						synchronized (nodeCurrentTick) {
							nodeCurrentTick.put(sender, Integer.parseInt( txMsg.getText().trim() ));
						}
						if( inTheSameTick() ){
							TextMessage contrlMsg = MessageUtil.createTextMessage(topicPub.getSession(), "");
							contrlMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
//							synchronized (currentTick) {
//								this.currentTick = this.currentTick + 1;
//								if( currentTick < tickNum ){
//									contrlMsg.setText(this.currentTick+"");
//								}
//								else{
//									
//								}
//							}
							this.currentTick = this.currentTick + 1;
							end = System.currentTimeMillis();
							System.out.println( "tick   " + this.currentTick + "    " + (end - s) );
							total += (end-s);
							s = end;
							if( currentTick == 0 )
								initTime = System.currentTimeMillis();
							
							if( currentTick < tickNum ){
								
								if( Market.refinePar ){
									
									Boolean ref = false;
									synchronized (weightOfNode) {
										double maxWeigt = Market.loadImbalance * totalWeight/nodeIdList.size();
										for( int i = 0; i < nodeIdList.size(); i++ ){
											if( weightOfNode.get( nodeIdList.get(i) ) > maxWeigt ){
												ref = true;
												System.err.println( weightOfNode.get( nodeIdList.get(i) +"    "+totalWeight +"   max:"+maxWeigt +" ex:"+weightOfNode.get(nodeIdList.get(i)) ) );
											}
										}
									}
									
									if( ref || ( 1.0*changed.size() / ( 1.0*vertexList.size() ) >= percentage ) ){
										System.err.println( "change:   "+ changed.size() +"   rate:"+ (1.0*changed.size() / ( 1.0*vertexList.size() ) )+"   per:"+percentage );
										synchronized (graph) {	
											synchronized (AN_Table) {
												synchronized (changed) {
													synchronized (weightOfNode) {
//														System.err.println("currentTick:"+ currentTick+ "  change:   "+ changed.size() +"   rate:"+ (1.0*changed.size() / ( 1.0*vertexList.size() ) )+"   per:"+percentage );
														
														Map<Vertex, String> refine = new HashMap<Vertex, String>();
														
//														Iterator<Vertex> anIte = graph.keySet().iterator();
//														double edgeCut = 0;
//														while ( anIte.hasNext() ) {
//															Vertex ver = anIte.next();
//															List<Edge> edgeList = graph.get( ver );
//															for( int i = 0; i < edgeList.size(); i++ ){
//																Edge e = edgeList.get(i);
//																if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
//																	edgeCut += e.edgeweight;
//															}
//														}
//														System.err.println( "before: edgecut:" + edgeCut + "  graphSize:"+graph.size() + "  antableSize:"+AN_Table.size() );
														
														long le = System.currentTimeMillis();
														try {
															refine = growingAlgorithm( loadImbalance );
														} catch (Exception e) {
															e.printStackTrace();
														}
														
//														System.err.println( "growing time:"+ ( System.currentTimeMillis() - le)  );
//														
//														anIte = graph.keySet().iterator();
//														edgeCut = 0;
//														while ( anIte.hasNext() ) {
//															Vertex ver = anIte.next();
//															List<Edge> edgeList = graph.get( ver );
//															for( int i = 0; i < edgeList.size(); i++ ){
//																Edge e = edgeList.get(i);
//																if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
//																	edgeCut += e.edgeweight;
//															}
//														}
//														
//														System.err.println( "after: edgecut:" + edgeCut + "  graphSize:"+graph.size() + "  antableSize:"+AN_Table.size() );
														
														StringBuffer refineStr = new StringBuffer();
														Set<Vertex> keySet = refine.keySet();
														if( null != keySet ){
															Iterator<Vertex> refIte = keySet.iterator();
															while( refIte.hasNext() ){
																Vertex ver = refIte.next();
																if( refineStr.length() == 0 ){
																	refineStr.append( ver.vertexId+";"+refine.get(ver) );
																}
																else{
																	refineStr.append( "#"+ ver.vertexId+";"+refine.get(ver) );
																}
															}
															
															TextMessage reFineMsg = MessageUtil.createTextMessage(topicPub.getSession(), refineStr.toString());
															reFineMsg.setJMSPriority(ProtocolString.networkMsgPriority);
															reFineMsg.setStringProperty( ProtocolString.F_TYPE , ProtocolString.TYPE_MARKET_REFINED);
															reFineMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
															reFineMsg.setStringProperty(ProtocolString.F_RECIVER, "");
															
//															System.err.println( "refine:  "+refineStr.substring(0, 20)+"  lenth:"+refineStr.length() );
															
															topicPub.publishMsg(false, 0, reFineMsg);
															return ;
														}
													}
												}
											}
										}
									}
								}
								
								contrlMsg.setText(this.currentTick+"");
								contrlMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
								contrlMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_NEXT_TICK);
								contrlMsg.setJMSPriority( ProtocolString.synchronizationMsgPriority );
								topicPub.publishMsg(false, 0, contrlMsg);
							}
							else{
								System.out.println("controlmessages: " + controlMessages);
								System.out.println("orderMessages:   " + orderMessages);
								System.out.println("controlMessageSize:  " + controlMessageSize);
								System.out.println("orderMessageSize:    " + orderMessageSize);
								
								System.out.println("total: "+total);
								System.out.println( "Time:"+( System.currentTimeMillis() - initTime ) );
								contrlMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_DESTROY);
								contrlMsg.setJMSPriority( ProtocolString.destroyPriority );
								topicPub.publishMsg(false, 0, contrlMsg);
								Market.this.destroy();
								return ;
							}
						}
					}
					else if( ProtocolString.CONTROL_CMD_NEXT_TICK.equals(cmd) ){
						
					}
					else if( ProtocolString.CONTROL_CMD_Add_VERTEX.equals(cmd) ){
						
						String sender = message.getStringProperty(ProtocolString.F_SENDER);
						
						List<Vertex> newAdded = new ArrayList<Vertex>();
						synchronized (graph) {
							synchronized (vertexList) {
								int fin = vertexList.get( vertexList.size() - 1 ).vertexId + 1;
								for( int i = 0; i < ProtocolString.rate; i++ ){
									Vertex ver = new Vertex();
									ver.vertexId = fin + i;
									ver.vertexWeight = 3;
									vertexList.add( ver );
									graph.put(ver, new ArrayList<Edge>() );
									newAdded.add(ver);
								}
							}
						}
//						System.err.println("\n\nrate:  "+ProtocolString.rate );
						String backStr = sender;
						
						synchronized (changed) {
							for( int i = 0; i < newAdded.size(); i++ ){
								changed.add( newAdded.get(i) );
								backStr += ","+ newAdded.get(i).vertexId;
							}
						}
						
						synchronized (AN_Table) {
							for( int i = 0; i < newAdded.size(); i++ ){
								AN_Table.put(newAdded.get(i), sender);
							}
						}
//						System.err.println( backStr );
						/*
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						String[] splitS = msgBody.split("#");
						Vertex ver = new Vertex();
						List<Edge> edgeList = new ArrayList<Edge>();
						
						synchronized (graph) {
							synchronized (vertexList) {
								ver.vertexId = vertexList.get( vertexList.size() - 1 ).vertexId + 1;
								vertexList.add(ver);
								graph.put(ver, edgeList);
							}
						}
						synchronized (changed) {
							changed.add(ver);
						}
						
						ver.vertexWeight = Double.parseDouble( splitS[0] );
						if( splitS.length > 1 && null != splitS[1] ){
							String[] edges = splitS[1].split(";");
							for( int i = 0; i < edges.length; i++ ){
								String[] edge = edges[i].split(",");
								if( edge.length == 2 ){
									Vertex end = getVertexOfList( Integer.parseInt( edge[0] ) );
									Edge ed = new Edge();
									ed.startPoint = ver;
									ed.endPoint = end;
									ed.edgeweight = Double.parseDouble( edge[1] );
									
									edgeList.add( ed );
									
									synchronized (graph) {
										List<Edge> inEdgeList = graph.get(end);
										Edge edd = new Edge();
										edd.startPoint = end;
										edd.endPoint = ver;
										edd.edgeweight = ed.edgeweight;
										
										inEdgeList.add( edd );
									}
									
									synchronized (changed) {
										changed.add(end);
									}
								}
							}
						}
						
						synchronized (graph) {
							graph.put(ver, edgeList);
						}
						synchronized (AN_Table) {
							AN_Table.put(ver, sender);
						}
						
						
						synchronized (weightOfNode) {
							weightOfNode.put( sender, weightOfNode.get(sender) + ver.vertexWeight );
						}
						synchronized (totalWeight) {
							totalWeight += ver.vertexWeight;
						}
						*/
//						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), sender+","+ver.vertexId +","+txMsg.getText() );
						
						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), backStr );
						
//						System.err.println( "market add vertex: "+outMsg.getText() );
						
						outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_Add_VERTEX);
						outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
						outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
						outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						
						topicPub.publishMsg(false, 0, outMsg);
						
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
								synchronized (graph) {
									List<Edge> edgeList = graph.get(ver);
									Edge edge = new Edge();
									edge.startPoint = ver;
									edge.endPoint = endVer;
									edge.edgeweight = edgeWeight;
									edgeList.add(edge);
									
									List<Edge> endEdgeList = graph.get(endVer);
									Edge edd = new Edge();
									edd.startPoint = endVer;
									edd.endPoint = ver;
									edd.edgeweight = edgeWeight;
									endEdgeList.add(edd);
								}
								
								synchronized (changed) {
									changed.add(ver);
									changed.add(endVer);
								}
								
								TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), txMsg.getText() );
								outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
								outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_ADD_LINK);
								outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
								outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
								outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
								topicPub.publishMsg(false, 0, outMsg);
								
							}
						}
					}
					else if( ProtocolString.CONTROL_CMD_DELETE_VERTEX.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String sender = message.getStringProperty( ProtocolString.F_SENDER );
						String msgBody = txMsg.getText();
						
						int vertexNum = Integer.parseInt( msgBody.trim() );
						Vertex v = getVertexOfList( vertexNum );
						synchronized ( vertexList ) {
							vertexList.remove(v);
						}
						synchronized (changed) {
							changed.remove(v);
						}
						synchronized (graph) {
							synchronized (changed) {
								List<Edge> edgeList = graph.get(v);
								for( int i = 0; i < edgeList.size(); i++ ){
									changed.add( edgeList.get(i).endPoint );
								}
							}
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
						synchronized (AN_Table) {
							AN_Table.remove(v);
						}
						
						synchronized (weightOfNode) {
							weightOfNode.put( sender, weightOfNode.get(sender) - v.vertexWeight );
						}
						synchronized (totalWeight) {
							totalWeight -= v.vertexWeight;
						}
						
						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), txMsg.getText() );
						outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_DELETE_VERTEX);
						outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
						outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
						outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						topicPub.publishMsg(false, 0, outMsg);
					}
					else if( ProtocolString.CONTROL_CMD_DELETE_LINK.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						int end = Integer.parseInt( msgBody.split(",")[1].trim() );
						Vertex ver = getVertexOfList(start);
						Vertex endVertex = getVertexOfList( end );
						
						synchronized (graph) {
							List<Edge> edgeList = graph.get(ver);
							for( int i = 0; i < edgeList.size(); i++ ){
								if( edgeList.get(i).endPoint.vertexId == end ){
									edgeList.remove(i);
									break;
								}
							}
							edgeList = graph.get( endVertex );
							for( int i = 0; i < edgeList.size(); i++ ){
								if( edgeList.get(i).endPoint.vertexId == start ){
									edgeList.remove(i);
									break;
								}
							}
						}
						
						synchronized (changed) {
							changed.add(ver);
							changed.add( endVertex);
						}
						
						
						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), txMsg.getText() );
						outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_DELETE_LINK);
						outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
						outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
						outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						topicPub.publishMsg(false, 0, outMsg);
						
					}
					else if( ProtocolString.CONTROL_CMD_CHANGE_LINK_WEIGHT.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						int end = Integer.parseInt( msgBody.split(",")[1].trim() );
						double weight = Double.parseDouble( msgBody.split(",")[2].trim() );
						
						Vertex ver = getVertexOfList(start);
						Vertex endVer = getVertexOfList(end);
						
						synchronized (graph) {
							List<Edge> edgeList = graph.get(ver);
							for( int i = 0; i < edgeList.size(); i++ ){
								Edge edge = edgeList.get(i);
								if( edge.endPoint.vertexId == end ){
									edge.edgeweight = weight;
									break;
								}
							}
							edgeList = graph.get(endVer);
							for( int i = 0; i < edgeList.size(); i++ ){
								Edge edge = edgeList.get(i);
								if( edge.endPoint.vertexId == start ){
									edge.edgeweight = weight;
									break;
								}
							}
						}
						
						synchronized (changed) {
							changed.add(ver);
							changed.add(endVer);
						}
						
						
						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), txMsg.getText() );
						outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_CHANGE_LINK_WEIGHT);
						outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
						outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
						outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						topicPub.publishMsg(false, 0, outMsg);
					}
					else if( ProtocolString.CONTROL_CMD_CHANGE_VERTEX_WEIGHT.equals(cmd) ){
						TextMessage txMsg = (TextMessage) message;
						String msgBody = txMsg.getText();
						String sender = message.getStringProperty( ProtocolString.F_SENDER );
						int start = Integer.parseInt( msgBody.split(",")[0].trim() );
						double weight = Double.parseDouble( msgBody.split(",")[1].trim() );
						Vertex ver = getVertexOfList(start);
						double oldWeight = ver.vertexWeight;
						ver.vertexWeight = weight;
						
						synchronized (totalWeight) {
							totalWeight = totalWeight - oldWeight + weight;
						}
						
						synchronized (weightOfNode) {
							weightOfNode.put(sender, weightOfNode.get(sender) - oldWeight + weight );
						}
						
						synchronized (changed) {
							changed.add(ver);
						}
						
						
						TextMessage outMsg = MessageUtil.createTextMessage( topicPub.getSession(), txMsg.getText() );
						outMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
						outMsg.setStringProperty(ProtocolString.CONTROL_CMD, ProtocolString.CONTROL_CMD_CHANGE_VERTEX_WEIGHT);
						outMsg.setStringProperty(ProtocolString.F_SENDER, Market.nodeID);
						outMsg.setStringProperty(ProtocolString.F_RECIVER, "");
						outMsg.setJMSPriority( ProtocolString.networkMsgPriority );
						topicPub.publishMsg(false, 0, outMsg);
					}
				}
				else if( ProtocolString.TYPE_ORDER.equals(type) ){
					
					synchronized ( orderMessages ) {
						orderMessages ++;
					}
					
					Order or = (Order) ( (ObjectMessage)message ).getObject();
					synchronized (orderMessageSize) {
						orderMessageSize += or.getAgentId().length()*2*8 + 64*2+32*3;
					}
					
//					System.out.println( "market: order:   "+message.getStringProperty( ProtocolString.F_SENDER )+"    "+( (ObjectMessage)message ).getObject().toString() );
					synchronized (receivedOrderList) {
						receivedOrderList.add(message);
					}
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public Vertex getVertexOfList(int vertexId){
		Vertex v = new Vertex();
		v.vertexId = vertexId;
		synchronized (vertexList) {
			int index = vertexList.indexOf(v);
			if( -1 == index )
				return null;
			return vertexList.get(index);
		}
	}
	
	public Boolean inTheSameTick(){
		synchronized (nodeCurrentTick) {
			for( int i = 0; i < nodeIdList.size(); i++ ){
				if( currentTick != nodeCurrentTick.get(nodeIdList.get(i)) ){
					return false;
				}
			}
		}
		return true;
	}
	
	private long s = 0;
	private long end = 0;
	
	public void startMarket( String partitionParams ){
		
		Market.partitionParams = partitionParams;
		
		s = System.currentTimeMillis();
		System.out.println( "start:    " + s  );
		
		vertexList = Partition.readGraph( graphFilePath, graph );
		List<List<Integer>> partition ;
		if( Market.multiLevelPar ){
			partition = Partition.paitition( graphFilePath, nodeIdList.size(), partitionParams );
		}
		else{
			partition = Partition.ranPaitition( vertexList, nodeIdList.size() );
		}
		
		System.out.println("1:   "+System.currentTimeMillis());

		for( int i = 0; i < nodeIdList.size(); i++ ){
			weightOfNode.put(nodeIdList.get(i), 0D);
		}
		
		StringBuilder anTableStr = new StringBuilder();
		totalWeight = 0D;
		
		for( int j = 0; j < partition.size(); j++ ){
			List<Integer> vertexes = partition.get(j);
			for( int k = 0; k < vertexes.size(); k++ ){
				Vertex ver = getVertexOfList( vertexes.get(k) );
				String nodeId = nodeIdList.get(j);
				this.AN_Table.put( ver , nodeId );
				totalWeight += ver.vertexWeight;
				weightOfNode.put( nodeId, ver.vertexWeight + weightOfNode.get(nodeId) );
				if ( anTableStr.length() == 0 ){
					anTableStr.append( vertexes.get(k).toString()+";"+nodeIdList.get(j) );
				}
				else{
					anTableStr.append( "#"+ vertexes.get(k).toString()+";"+nodeIdList.get(j) );
				}
			}
		}
		
//		System.out.println("totalWeight:"+totalWeight);
//		Iterator<String> weiIte = weightOfNode.keySet().iterator();
//		while( weiIte.hasNext() ){
//			String nodeId =  weiIte.next();
//			System.out.print( weightOfNode.get( nodeId ) + ";" + nodeId + "#" );
//		}
//		System.out.println();
		
		System.out.println("2:   "+System.currentTimeMillis());
		
		try {
			Message msg = MessageUtil.createTextMessage( topicPub.getSession(), anTableStr.toString() );
			msg.setJMSPriority(ProtocolString.networkMsgPriority);
			msg.setStringProperty(ProtocolString.F_SENDER, Market.marketTopicId);
			msg.setStringProperty(ProtocolString.F_RECIVER, "");
			msg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_ANTABLE);
			
			topicPub.publishMsg(false, 0, msg);
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public void deelOrderMessage(){
		Message msg = null ;
		
		synchronized (receivedOrderList) {
			if( receivedOrderList.size() > 0 ){
				msg = receivedOrderList.get(0);
				receivedOrderList.remove(0);
			}
			else{
				msg = null;
			}
		}
		
		if( null != msg ){
			try {
				Order or = (Order) ( (ObjectMessage)msg ).getObject();
				if( null != or ){
					List<Trade> traList = new ArrayList<Trade>();
					//insert order into trade Area and trading
			        synchronized ( Market.tradeStock ) {
			            if ( 0 == Market.tradeStock.insertIntoQueue(or) ) {
			            	traList = Market.tradeStock.auction();
			            }
			        }
			        
			        //trading is success
			        if ( null != traList && !traList.isEmpty() ) {
			            for (int i = 0; i < traList.size(); i++) {
			            	Trade trade = traList.get(i);
			            	sendTradeMsgOut(trade, Market.nodeID, trade.getBuyId(), false, 0);
			            	sendTradeMsgOut(trade, Market.nodeID, trade.getSellId(), false, 0);
			            }
			        }
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void sendControlMsgOut( String controlMsg, String sender, String receiver, Boolean persistent, long timeToLive){
		if ( null != controlMsg ){
			try {
				if( null != receiver ){
					
					String queueId = null;
					synchronized (AN_Table) {
						queueId = AN_Table.get(receiver);
					}
					if( null != queueId ){
						Point2PointProducer p = producerMap.get(queueId);
						if( null != p ){
							TextMessage textMsg = MessageUtil.createTextMessage( p.getSession(), controlMsg );
							textMsg.setStringProperty(ProtocolString.F_SENDER, sender);
							textMsg.setStringProperty(ProtocolString.F_RECIVER, receiver);
							textMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_CONTROL);
							p.sendMessage( persistent, timeToLive, textMsg );
						}
					}
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void sendOrderMsgOut( Order order, String sender, String receiver, Boolean persistent, long timeToLive){
		if ( null != order ){
			try {
				if( null != receiver ){
					String queueId = null;
					synchronized (AN_Table) {
						queueId = AN_Table.get(receiver);
					}
					if( null != queueId ){
						Point2PointProducer p = producerMap.get(queueId);
						if( null != p ){
							ObjectMessage objMsg = MessageUtil.createObjectMessage( p.getSession(), order );
							objMsg.setStringProperty(ProtocolString.F_SENDER, sender);
							objMsg.setStringProperty(ProtocolString.F_RECIVER, receiver);
							objMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_ORDER);
							p.sendMessage( persistent, timeToLive, objMsg );
						}
						
					}
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void sendTradeMsgOut( Trade trade, String sender, String receiver, Boolean persistent, long timeToLive){
		if ( null != trade ){
			try {
				if( null != receiver ){
					String queueId = null;
					synchronized (AN_Table) {
						queueId = AN_Table.get(receiver);
					}
					if( null != queueId ){
						Point2PointProducer p = producerMap.get(queueId);
						if( null != p ){
							ObjectMessage objMsg = MessageUtil.createObjectMessage( p.getSession(), trade );
							objMsg.setStringProperty(ProtocolString.F_SENDER, sender);
							objMsg.setStringProperty(ProtocolString.F_RECIVER, receiver);
							objMsg.setStringProperty(ProtocolString.F_TYPE, ProtocolString.TYPE_TRADE);
							p.sendMessage( persistent, timeToLive, objMsg );
						}
						
					}
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	@Override
	public void destroy() {
		
		synchronized (graph) {
			this.graph.clear();
		}
		synchronized (AN_Table) {
			this.AN_Table.clear();
		}
		synchronized (receivedOrderList) {
			this.receivedOrderList.clear();
		}
		
		
//		for( int i = 0; i < deelOrderThreadsNum; i++ ){
//			deelOrderThreads[i].s
//		}
		synchronized ( threadEnd ) {
			threadEnd = true;
		}
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			String nodeId = nodeIdList.get(i);
			Point2PointProducer p = producerMap.get(nodeId);
			if( null != p ){
				p.destroy();
			}
		}
		producerMap.clear();
		this.nodeIdList.clear();
		
		super.destroy();
		this.topicPub.destroy();
	}
	
	class VertexToNode{	//二次划分的时候使用
		public Vertex vertex;
		public String nodeId;	//标示vertex会被划分到哪个队列中	
		public double gain;		//标示vertex会被划分到这个队列的gain
		public VertexToNode(Vertex ver, String nodeId, double gain) {
			this.vertex = ver;
			this.nodeId = nodeId;
			this.gain = gain;
		}
	}
	
	public static void deleteFromList( List<VertexToNode> vnList, Vertex ver ){
		Iterator<VertexToNode> vnIte = vnList.iterator();
		List<Integer> indexList = new ArrayList<Integer>();
		int index = 0;
		while( vnIte.hasNext() ){
			VertexToNode inVn = vnIte.next();
			if( inVn.vertex.vertexId == ver.vertexId ){
				indexList.add(index);
			}
			index ++;
		}
		
		for( int i = indexList.size() - 1; i >= 0 ; i-- ){
			int removeIndex = indexList.get(i);
			vnList.remove( removeIndex );
		}
	}
	public static void addGainInList( List<VertexToNode> vnList, Vertex ver, String nodeId, double addedGain ){
		Iterator<VertexToNode> vnIte = vnList.iterator();
		VertexToNode vn = null;
		int index = 0;
		while( vnIte.hasNext() ){
			VertexToNode inVn = vnIte.next();
			if( inVn.vertex.equals( ver ) && inVn.nodeId.equals( nodeId ) ){
				inVn.gain = inVn.gain + addedGain;
				vn = inVn;
				break;
			}
			index ++;
		}
		vnList.remove(index);
		index = 0;
		vnIte = vnList.iterator();
		while( vnIte.hasNext() ){
			VertexToNode inVn = vnIte.next();
			if( inVn.gain <= vn.gain ){
				break;
			}
			index ++;
		}
		vnList.add(index, vn);
	}
	
	
	public Map<Vertex, String> growingAlgorithm( double b ){
		
		double maxNodeWeight = b*totalWeight/nodeIdList.size();
//		double meanNodeWeight = totalWeight/nodeIdList.size();
		
//		long s = System.currentTimeMillis();
		
		Map<Vertex, String> refined = new HashMap<Vertex, String>();
		double gain = 0;
		double in = 0;
		double out = 0;
		
//		System.err.println( "KL:  aa  1" );
		
		List<VertexToNode> priList = new LinkedList<VertexToNode>();
		
		Iterator<Vertex> chIte = changed.iterator();
		while( chIte.hasNext() ){
			Vertex ver = chIte.next();
			if( null != ver ){
				String nodeId = null;
				
				nodeId = AN_Table.get(ver);
				
				if( null != nodeId ){
					synchronized (weightOfNode) {
						weightOfNode.put( nodeId, weightOfNode.get(nodeId) - ver.vertexWeight );
					}
				}
			}
		}
		
//		System.err.println("in    "+weightOfNode.get("node96") + "    " + weightOfNode.get("node97") + "    " + weightOfNode.get("node98") + "    " + weightOfNode.get("node99") );
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			String nodeId = nodeIdList.get(i);
			
			Iterator<Vertex> setIterator = changed.iterator();
			while( setIterator.hasNext() ){
				Vertex ver = setIterator.next();
				in = 0; out = 0;
				List<Edge> edgeList = graph.get(ver);
				for( int j = 0; j < edgeList.size(); j++ ){
					Edge edge = edgeList.get(j);
					if( !changed.contains(edge.endPoint) ){
						String inNodeId = null;
						inNodeId = AN_Table.get( edge.endPoint );
						if( null == inNodeId ){
							edgeList.remove(j);
							j--;
						}
						else{
							if( inNodeId.equals( nodeId ) )
								in += edge.edgeweight;
							else
								out += edge.edgeweight;
						}
					}
				}
				gain = in - out;
				int index = 0;
				Iterator<VertexToNode> gainIterator = priList.iterator();
				while( gainIterator.hasNext() ){
					VertexToNode vn = gainIterator.next();
					if( gain >= vn.gain ){
						break;
					}
					index ++;
				}
				priList.add(index, new VertexToNode(ver, nodeId, gain) );
			}
		}
		int size = changed.size();
		for( int i = 0; i < size; i++ ){
			
//			System.err.println( "KL:   1  0 "+ i );
			
			VertexToNode vn = null;
			Iterator<VertexToNode> priIte = priList.iterator();
			while( priIte.hasNext() ){
				VertexToNode inVN = priIte.next();
				if( weightOfNode.get( inVN.nodeId ) + inVN.vertex.vertexWeight < maxNodeWeight ){
					vn = inVN;
					break;
				}
			}
			
			if( vn == null )
				break;
			
//			System.err.println( "KL:   1  1 "+ i );
			
			deleteFromList(priList, vn.vertex);
			
			weightOfNode.put( vn.nodeId, weightOfNode.get( vn.nodeId ) + vn.vertex.vertexWeight );
			
			refined.put(vn.vertex, vn.nodeId);
			AN_Table.put(vn.vertex, vn.nodeId);
			
			changed.remove( vn.vertex );
			
			List<Edge> edgeList = graph.get( vn.vertex );
			for( int j = 0; j < edgeList.size(); j++ ){
				Edge edge = edgeList.get(j);
				if( changed.contains( edge.endPoint ) ){
					for( int k = 0; k < nodeIdList.size(); k++ ){
						String nodeId = nodeIdList.get(k);
						if( nodeId.equals( vn.nodeId ) ){
							addGainInList(priList, edge.endPoint, nodeId, edge.edgeweight);
						}
						else{
							addGainInList(priList, edge.endPoint, nodeId, -1*edge.edgeweight);
						}
					}
				}
			}
		}
		
//		System.err.println( "in size: "+changed.size() );
		changed.clear();
		
//		System.out.println( System.currentTimeMillis() - s );
		return refined;
	}
	
	class DeelOrderThread extends Thread{
		@Override
		public void run() {
			super.run();
			while (true) {
				deelOrderMessage();
//				synchronized (threadEnd) {
					if( threadEnd )
						break;
//				}
			}
//			System.out.println("thread  "+  hashCode() +"  stop" );
		}
	}

	@Override
	public void onException(JMSException arg0) {
		arg0.printStackTrace();
	}
	
}
