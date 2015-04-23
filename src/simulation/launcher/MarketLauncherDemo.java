package simulation.launcher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import comm.activemq.ActiveMqParam;

import simulation.market.Market;
import simulation.market.ProtocolString;

public class MarketLauncherDemo {
	public static void main(String[] args) {
		File marketCfgFile = new File( Market.marketConfigFileName );
		try {
			DataInputStream fileIn = new DataInputStream( new FileInputStream(marketCfgFile) );
			String line = fileIn.readLine();
			line = line.trim();
			String[] nodes = line.split(" ");
			
			final List<String> nodeIdList = new ArrayList<String>();
			
			for( int i = 0; i < nodes.length; i++ ){
				String nodeId = nodes[i];
				if( null != nodeId && nodeId.trim().length() > 0 ){
					nodeId = nodeId.trim();
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
			
			Market.percentage = Double.parseDouble( fileIn.readLine().trim() );
			
			if( Integer.parseInt( fileIn.readLine().trim() ) == 1 ){
				Market.multiLevelPar = true;
			}
			else{
				Market.multiLevelPar = false;
			}
			
			if( Integer.parseInt( fileIn.readLine().trim() ) == 1 ){
				Market.refinePar = true;
			}
			else{
				Market.refinePar = false;
			}
			
			Market.loadImbalance = Double.parseDouble( fileIn.readLine().trim() );
			ProtocolString.rate = Integer.parseInt( fileIn.readLine().trim() );
			
			String params =  fileIn.readLine();
			if( null == params || params.trim().length() == 0 )
				params = null;
			else
				params = params.trim();
			final String partitionParams = params;
			
			System.out.println( Market.loadImbalance +"   " + partitionParams );
			
			new Thread(){
				public void run() {
					try {
						Market market = new Market(roundPerTick, tickNum, graphFilePath, nodeIdList);
						market.startMarket( partitionParams );
//						"/home/shawn/network/astro-ph0"
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
