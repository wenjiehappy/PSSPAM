package comm.activemq;

import org.apache.activemq.ActiveMQConnection;

public class ActiveMqParam {
	public static final String USER = ActiveMQConnection.DEFAULT_USER;
	public static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;
	public static String BROKER_URL = ActiveMQConnection.DEFAULT_BROKER_URL;
//	public static String BROKER_URL = "failover://tcp://192.168.44.128:61616";
}