package simulation.market;

public class ProtocolString {
	public static final String F_SENDER = "Sender";
	public static final String F_RECIVER = "Receiver";
	
	public static final String F_TYPE = "Type";
	public static final String TYPE_CONTROL = "Control";
	public static final String TYPE_ORDER = "Order";
	public static final String TYPE_TRADE = "Trade";
	public static final String TYPE_COMMUNICATION = "Communication";
	public static final String TYPE_ANTABLE = "ANTable";
	public static final String TYPE_DESTROY = "Destroy";
	public static final String TYPE_MARKET_REFINED = "MarketRefined";
	public static final String TYPE_Controller_REFINED = "ControllerRefined";
	
	public static final String CONTROL_CMD = "Cmd";
	public static final String CONTROL_CMD_CURRENT_TICK = "CurrentTick";
	public static final String CONTROL_CMD_NEXT_TICK = "NextTick";
	
	
	public static final String CONTROL_CMD_ADD_LINK = "AddLink";
	public static final String CONTROL_CMD_DELETE_LINK = "DeleteLink";
	public static final String CONTROL_CMD_Add_VERTEX = "AddVertex";
	public static final String CONTROL_CMD_DELETE_VERTEX = "DeleteVertex";
	public static final String CONTROL_CMD_CHANGE_VERTEX_WEIGHT = "ChangeVertexWeight";
	public static final String CONTROL_CMD_CHANGE_LINK_WEIGHT = "ChangeLinkWeight";
	
	public static final int synchronizationMsgPriority = 1;		//the lager the priority is higher
	public static final int networkMsgPriority = 9;
	public static final int communicationMsgPriority = 3;
	public static final int defaultMsgPriority = 4;
	public static final int destroyPriority = 1;
	
	public static int threadNum = 0;
	public static int consumeThread = 20;
	public static final int seed = 1288923;
	public static int edgeFactor = 80;
	public static double N = 50000;
	public static int rate = 1;
	
}
