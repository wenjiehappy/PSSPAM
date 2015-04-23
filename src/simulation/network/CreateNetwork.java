package simulation.network;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

public class CreateNetwork {
	
	public static void main(String[] args) {
//		String source = null;
//		String filePath = "C:\\Users\\chx\\Desktop\\network\\karate.gml";
		String filePath = "C:\\Users\\chx\\Desktop\\network\\as-22july06_0.gml";
		File inFile = new File( filePath );
		File outFile = new File("out");
		Map<Integer, Vector<Integer>> graph = new TreeMap<Integer, Vector<Integer>>();
		Map<Integer, Integer> vertexWeight = new TreeMap<Integer, Integer>();
		
		if( !inFile.exists() ){
			System.out.println(inFile.getAbsolutePath()+" does not exists");
		}
		
		try {
			PrintStream pt = new PrintStream(outFile);
			System.setOut( pt );
			
			FileInputStream ins = new FileInputStream(inFile);
			DataInputStream din = new DataInputStream(ins);
			
			String line = "";
//			source = source + line;
			line = din.readLine();
			while( line != null ){
				line = line.trim();
				if( line.length() > 0 ){
					if( line.startsWith("id") ){
//						line = din.readLine();
//						line = din.readLine();
//						line = line.trim();
						Integer node = Integer.parseInt( line.split(" ")[1] );
						
						if( !graph.containsKey( node ) ){
							Vector<Integer> edges = new Vector<Integer>();
							graph.put(node, edges);
						}
						
					}
					else if( line.startsWith("source") ){
//						line = din.readLine();
//						line = din.readLine();
//						line = line.trim();
						Integer source = Integer.parseInt( line.split(" ")[1] );
						
						line = din.readLine();
						line = line.trim();
						Integer target = Integer.parseInt( line.split(" ")[1] );
						
						if( !graph.containsKey( source ) ){
							Vector<Integer> edges = new Vector<Integer>();
							graph.put(source, edges);
						}
						
						Vector<Integer> sourceEdge = graph.get(source);
						sourceEdge.add(target);
						
						if( source == 40382 || target == 40382 ){
							System.out.print("");
						}
						
						if( !graph.containsKey( target ) ){
							Vector<Integer> edges = new Vector<Integer>();
							graph.put(target, edges);
						}
						Vector<Integer> targetEdge = graph.get(target);
						targetEdge.add(source);
						
					}
				}
				line = din.readLine();
			}
			
			Iterator ie = graph.keySet().iterator();
			int edgeSize = 0;
			int nodeSize = graph.size();
			while( ie.hasNext() ){
				Object obj = ie.next();
				Vector<Integer> edges = graph.get(obj);
				edgeSize += edges.size();
			}
			
			System.out.println( nodeSize +" "+ edgeSize/2 );
			ie = graph.keySet().iterator();
			while( ie.hasNext() ){
				Object obj = ie.next();
//				System.out.println(obj);
				Vector<Integer> edges = graph.get(obj);
//				for( int i = 0; i < edges.size(); i++ ){
//					if( i < edges.size() - 1 ){
//						System.out.print( edges.get(i)+" " );
//					}
//					else{
//						System.out.println( edges.get(i) );
//					}
//				}
				if( edges.size() == 0 ){
					System.out.println();
				}
				else{
					for( int i = 0; i < edges.size(); i++ ){
						if( i < edges.size() - 1 ){
							System.out.print( (edges.get(i)+1)+" " );
						}
						else{
							System.out.println( (edges.get(i)+1) );
						}
					}
				}
			}

			pt.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}