//package simulation.launcher;
//
//import java.io.DataInputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import javax.jms.JMSException;
//import simulation.market.Market;
//import simulation.node.Controller;
//
//public class LauncherDemo {
//	
//	public static void main(String[] args) {
//		
//		final int roundPerTick = 1;
//		final int tickNum = 3;
//		
//		File marketCfgFile = new File( Controller.nodeCfgFile );
//		try {
//			DataInputStream fileIn = new DataInputStream(new FileInputStream(marketCfgFile));
//			String line = fileIn.readLine();
//			final String nodeID = line.trim();
//			line = fileIn.readLine();
//			final String graphFilePath = line.trim();
//			new Thread(){
//				public void run() {
//					try {
//						Controller node1 = new Controller(nodeID, roundPerTick, tickNum);
//						System.out.println(nodeID);
//						node1.startNode( graphFilePath );
//					} catch (JMSException e) {
//						e.printStackTrace();
//					}
//				};
//			}.start();
//			
////			new Thread(){
////				public void run() {
////					try {
////						Market market = new Market(roundPerTick, tickNum);
////						market.startMarket();
//////						"/home/shawn/network/astro-ph0"
////					} catch (JMSException e) {
////						e.printStackTrace();
////					}
////				};
////			}.start();
//		} catch (FileNotFoundException e1) {
//			e1.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//}