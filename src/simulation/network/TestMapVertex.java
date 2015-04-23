package simulation.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMapVertex {
	public static void main(String[] args) {
		Map<Vertex, String> anTable = new HashMap<Vertex, String>();
		List<Vertex> verList = new ArrayList<Vertex>();
		
		Vertex ver = new Vertex();
		ver.vertexId = 0;
		anTable.put(ver, "node0");
		verList.add(ver);
		
		Vertex ver1 = new Vertex();
		ver1.vertexId = 1;
		anTable.put(ver1, "node1");
		verList.add(ver1);
		
		Vertex ver2 = new Vertex();
		ver2.vertexId = 2;
		anTable.put(ver2, "node2");
		verList.add(ver2);
		
		Vertex ver3 = new Vertex();
		ver3.vertexId = 3;
		anTable.put(ver3, "node3");
		verList.add(ver3);
		
		Vertex v = new Vertex();
		v.vertexId = 2;
		
		String re = anTable.get(v);
		
		if( anTable.containsKey(v) ){
			System.out.println("yes");
		}
		else{
			System.out.println("no");
		}
		
		if( null == re ){
			System.out.println("null");
		}
		else{
			System.out.println( re );
		}
		
		System.out.println( verList.indexOf(v) );
		System.out.println( verList.indexOf( ver2 ) );
		
	}
	
}
