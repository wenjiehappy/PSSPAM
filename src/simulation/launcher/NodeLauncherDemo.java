package simulation.launcher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import simulation.market.ProtocolString;
import simulation.node.Consuming;
import simulation.node.Controller;

import comm.activemq.ActiveMqParam;

public class NodeLauncherDemo {
	public static void main(String[] args) {
		File marketCfgFile = new File( Controller.nodeCfgFile );
		try {
			DataInputStream fileIn = new DataInputStream(new FileInputStream(marketCfgFile));
			String line = fileIn.readLine();
			
			String[] nodes = line.split(" ");
			
			final List<String> nodeIdList = new ArrayList<String>();
			
			for( int i = 0; i < nodes.length; i++ ){
				String nodeId = nodes[i].trim();
				if( null != nodeId && nodeId.length() > 0 ){
					nodeIdList.add( nodeId );
				}
			}
			
			line = fileIn.readLine();
			final String graphFilePath = line.trim();
			
			ActiveMqParam.BROKER_URL = fileIn.readLine().trim();
			
			line = fileIn.readLine().trim();
			String[] tickRound = line.split(" ");
			final int roundPerTick = Integer.parseInt( tickRound[0].trim() );
			final int tickNum = Integer.parseInt( tickRound[1].trim() );
			
			line = fileIn.readLine().trim();
			ProtocolString.threadNum = Integer.parseInt( line );
			
			Controller.sendNum = Integer.parseInt( fileIn.readLine().trim() );
			
			ProtocolString.N = Double.parseDouble( fileIn.readLine().trim() );
			
			ProtocolString.edgeFactor = Integer.parseInt( fileIn.readLine().trim() );
			
			if( Integer.parseInt( fileIn.readLine().trim() ) == 1 )
				Controller.evolve = true;
			else
				Controller.evolve = false;
			Controller.m = Integer.parseInt( fileIn.readLine().trim() );
			Controller.alpha = Double.parseDouble( fileIn.readLine().trim() );
			Controller.beita =  Double.parseDouble( fileIn.readLine().trim() );
			Controller.gama =  Double.parseDouble( fileIn.readLine().trim() );
			Controller.a = Integer.parseInt( fileIn.readLine().trim() );
			Controller.b = Integer.parseInt( fileIn.readLine().trim() );
			ProtocolString.rate = Integer.parseInt( fileIn.readLine().trim() );
			System.out.println( ProtocolString.N + "   "+ Controller.evolve +"  " + Controller.m +"  " + Controller.alpha +"  " + Controller.beita +"  " + Controller.gama +"  " + Controller.a +"  " + Controller.b );
			
			new Thread(){
				public void run() {
					try {
						Controller node1 = new Controller(nodeIdList.get(0),  roundPerTick, tickNum, nodeIdList );
						node1.startNode( graphFilePath );
					} catch (JMSException e) {
						e.printStackTrace();
					}
				};
			}.start();
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
