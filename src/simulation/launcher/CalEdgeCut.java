package simulation.launcher;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import simulation.network.Edge;
import simulation.network.Partition;
import simulation.network.Vertex;

public class CalEdgeCut {
	public Vertex getVertexOfList(int vertexId){
		Vertex v = new Vertex();
		v.vertexId = vertexId;
			int index = vertexList.indexOf(v);
			if( -1 == index )
				return null;
			return vertexList.get(index);
	}
	private Map<Vertex, List<Edge>> graph = new HashMap<Vertex, List<Edge>>();
	private List<Vertex> vertexList = new ArrayList<Vertex>();
	private List<String> nodeIdList = new ArrayList<String>();
	private Map<Vertex, String> AN_Table = new HashMap<Vertex, String>();
	
	public void ran(){
		nodeIdList.add( "node0" );
		nodeIdList.add( "node1" );
		nodeIdList.add( "node2" );
		nodeIdList.add( "node3" );
		
		graph.clear();
		vertexList.clear();
		
		graph = new HashMap<Vertex, List<Edge>>();
		vertexList = Partition.readGraph( "cond-mat-2005_0", graph );
		List< List<Integer> > partition = Partition.ranPaitition(vertexList, 4);
		
		for( int j = 0; j < partition.size(); j++ ){
			List<Integer> vertexes = partition.get(j);
			for( int k = 0; k < vertexes.size(); k++ ){
				Vertex ver = getVertexOfList( vertexes.get(k) );
				String nodeId = nodeIdList .get(j);
				AN_Table.put( ver , nodeId );
			}
		}
		
		Iterator<Vertex> anIte = graph.keySet().iterator();
		double edgeCut = 0;
		double a = 0;
		while ( anIte.hasNext() ) {
			Vertex ver = anIte.next();
			List<Edge> edgeList = graph.get( ver );
			for( int i = 0; i < edgeList.size(); i++ ){
				Edge e = edgeList.get(i);
				 a += e.edgeweight;
				if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
					edgeCut += e.edgeweight;
			}
		}
		System.out.println("aaa:"+a/2);
		System.out.println( edgeCut/400 );
	}
	
	public void mul(){
		nodeIdList.add( "node0" );
		nodeIdList.add( "node1" );
		nodeIdList.add( "node2" );
		nodeIdList.add( "node3" );
		
		graph.clear();
		vertexList.clear();
		
		graph = new HashMap<Vertex, List<Edge>>();
		vertexList = Partition.readGraph( "cond-mat-2005_0", graph );
		List< List<Integer> > partition = new ArrayList< List<Integer> >();
		
		int partitionNum = nodeIdList.size();
		
		for( int i = 0; i < partitionNum ; i++ ){
			List<Integer> vertexes = new LinkedList<Integer>();
			partition.add(i, vertexes);
		}
		
		try {
			String outPutFile = "cond-mat-2005_0.part.3";
			File partitionFile = new File(outPutFile);
			
			if( partitionFile.exists() ){
				
				DataInputStream fileIn = new DataInputStream(new FileInputStream(partitionFile));
				
				int vertex = 1;
				String line = fileIn.readLine();
				while( null != line ){
					line = line.trim();
					int par = Integer.parseInt(line);
					if( par < partitionNum ){
						List<Integer> vertexes = partition.get(par);
						vertexes.add(vertex);
					}
					line = fileIn.readLine();
					vertex++;
				}
			}else{
				System.out.println(" partition file no where ");
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		for( int j = 0; j < partition.size(); j++ ){
			List<Integer> vertexes = partition.get(j);
			for( int k = 0; k < vertexes.size(); k++ ){
				Vertex ver = getVertexOfList( vertexes.get(k) );
				String nodeId = nodeIdList .get(j);
				AN_Table.put( ver , nodeId );
			}
		}
		
		Iterator<Vertex> anIte = graph.keySet().iterator();
		double edgeCut = 0;
		double a = 0;
		while ( anIte.hasNext() ) {
			Vertex ver = anIte.next();
			List<Edge> edgeList = graph.get( ver );
			for( int i = 0; i < edgeList.size(); i++ ){
				Edge e = edgeList.get(i);
				 a += e.edgeweight;
				if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
					edgeCut += e.edgeweight;
			}
		}
		System.out.println("a:"+a/2);
		System.out.println( edgeCut/400 );
		
	}
	
	public static void main(String[] args) {
		CalEdgeCut a = new CalEdgeCut();
		a.mul();
		
		CalEdgeCut c = new CalEdgeCut();
		c.ran();
		
	}
}
