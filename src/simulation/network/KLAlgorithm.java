package simulation.network;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.NodeList;

public class KLAlgorithm {
	
	Map<Vertex, String> AN_Table = new HashMap<Vertex, String>();	//A-N Table
	
	Map<Vertex, List<Edge>> graph = new HashMap<Vertex, List<Edge>>();
	
	List<Vertex> vertexList = new ArrayList<Vertex>();
	
	List<String> nodeIdList = new ArrayList<String>();
	Map<String, Double> weightOfNode = new HashMap<String, Double>();
	
	Set<Vertex> changed = new HashSet<Vertex>();
	
	public void ssn( double b ) {
		
		vertexList = Partition.readGraph("astro-ph0", graph);
		
		nodeIdList.add("node0");
		nodeIdList.add("node1");
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			weightOfNode.put(nodeIdList.get(i), 0D);
		}
		
		AN_Table.put(getVertexOfList(1), "node0");
		AN_Table.put(getVertexOfList(2), "node0");
		AN_Table.put(getVertexOfList(4), "node0");
		AN_Table.put(getVertexOfList(6), "node0");
		
		AN_Table.put(getVertexOfList(3), "node1");
		AN_Table.put(getVertexOfList(5), "node1");
		AN_Table.put(getVertexOfList(7), "node1");
		AN_Table.put(getVertexOfList(8), "node1");
		
		double totalWeight = 0;
		
		Iterator<Vertex> anIte = AN_Table.keySet().iterator();
		while( anIte.hasNext() ){
			Vertex ver = anIte.next();
			String nodeId = AN_Table.get(ver);
			totalWeight += ver.vertexWeight;
			weightOfNode.put( nodeId, ver.vertexWeight + weightOfNode.get(nodeId) );
		}
		
		double maxNodeWeight = b*totalWeight/nodeIdList.size();
		double minNodeWeight = totalWeight*( 1-1.0*(nodeIdList.size() - 1)/nodeIdList.size() );
		System.out.println(maxNodeWeight+"   "+minNodeWeight);
		//每个结点的weight
//		Iterator<String> weightIte = weightOfNode.keySet().iterator();
//		while( weightIte.hasNext() ){
//			String nodeId = weightIte.next();
//			System.out.println(nodeId+"   "+weightOfNode.get(nodeId));
//		}
//		System.out.println( totalWeight );
//		System.out.println( b*totalWeight/nodeIdList.size() );
		
		changed.addAll(vertexList);
		
		List<Vertex> firstQueue = new ArrayList<Vertex>();
		List<Double> firstD = new ArrayList<Double>();
		List<Vertex> secondQueue = new ArrayList<Vertex>();
		List<Double> secondD = new ArrayList<Double>();
		
		Iterator<Vertex> changeIte = changed.iterator();
		while( changeIte.hasNext() ){
			Vertex ver = changeIte.next();
			
			double in = 0;
			double out = 0;
			List<Edge> edges = graph.get(ver);
			if( "node0".equals(AN_Table.get(ver)) ){
				for( int j = 0; j < edges.size(); j++ ){
					Edge edge = edges.get(j);
					if( "node0".equals( AN_Table.get( edge.endPoint ) ) ){
						in += edge.edgeweight;
					}
					else{
						out += edge.edgeweight;
					}
				}
				
				double d = out - in;
				
//				int index = 0; 
//				for( ; index < firstD.size(); index++ ){
//					if( d >= firstD.get( index ) ){
//						break;
//					}
//				}
//				firstD.add(index, d);
//				firstQueue.add(index, ver);
				
				//未排序插入
				firstD.add( d );
				firstQueue.add( ver );
			}
			else{
				for( int j = 0; j < edges.size(); j++ ){
					Edge edge = edges.get(j);
					if( "node1".equals( AN_Table.get( edge.endPoint ) ) ){
						in += edge.edgeweight;
					}
					else{
						out += edge.edgeweight;
					}
				}
				double d = out - in;
//				int index = 0; 
//				for( ; index < secondD.size(); index++ ){
//					if( d >= secondD.get( index ) ){
//						break;
//					}
//				}
//				secondD.add(index, d);
//				secondQueue.add(index, ver);
				
				//未排序插入
				secondD.add(d);
				secondQueue.add(ver);
			}
		
		}
		
		while( !firstD.isEmpty() && !secondD.isEmpty() ){
			int firstI = 0;
			int secondI = 0;
			double gain = -1000;
			
//			System.out.println("First  ");
//			out(firstQueue, firstD);
//			System.out.println("Second  ");
//			out(secondQueue, secondD);
			
			for( int i = 0; i < firstD.size(); i++ ){
				List<Edge> edgeList = graph.get( firstQueue.get(i) );
				for( int j = 0; j < secondD.size(); j++ ){
					double innerWei = -1;
					for( int k = 0; k < edgeList.size(); k++ ){
						if( edgeList.get(k).endPoint.vertexId == secondQueue.get(j).vertexId ){
							innerWei = edgeList.get(k).edgeweight;
						}
					}
					if( innerWei < 0 )
						innerWei = 0;
					
					double tempGain = firstD.get(i) + secondD.get(j) - 2*innerWei;
					if( tempGain > gain ){
						gain = tempGain;
						firstI = i;
						secondI = j;
					}
				}
			}
			
			if( weightOfNode.get("node0")-firstQueue.get(firstI).vertexWeight+secondQueue.get(secondI).vertexWeight > maxNodeWeight ){
				firstQueue.remove(firstI);
				firstD.remove(firstI);
			}
			
			else if( weightOfNode.get("node1")+firstQueue.get(firstI).vertexWeight-secondQueue.get(secondI).vertexWeight > maxNodeWeight ){
				secondQueue.remove(secondI);
				secondD.remove(secondI);
			}
			
			else if( gain > 0 || ( gain == 0 && Math.abs(  weightOfNode.get("node1") -  weightOfNode.get("node0") ) > Math.abs(  weightOfNode.get("node0")-2*firstQueue.get(firstI).vertexWeight -  weightOfNode.get("node1") + 2*secondQueue.get(secondI).vertexWeight )   ) ){

				System.out.println( firstQueue.get(firstI) + "  " + secondQueue.get(secondI) +"   "+gain);
				
				Vertex fir = firstQueue.get(firstI);
				Vertex sec = secondQueue.get(secondI);
				
				AN_Table.put(fir, "node1");
				AN_Table.put(sec, "node0");
				
				firstQueue.remove(firstI);
				firstD.remove(firstI);
				secondQueue.remove(secondI);
				secondD.remove(secondI);
				
				List<Edge> firEdList = graph.get(fir);
				for( int j = 0; j < firEdList.size(); j++ ){
					Edge edge = firEdList.get(j);
					int index = firstQueue.indexOf( edge.endPoint );
					
					if( index >= 0  ){
						firstD.set(index, firstD.get(index) + 2*edge.edgeweight);
					}
					else{
						index = secondQueue.indexOf( edge.endPoint );
						if( index >= 0 )
							secondD.set(index, secondD.get(index) - 2*edge.edgeweight);
					}
				}
				firEdList = graph.get(sec);
				for( int j = 0; j < firEdList.size(); j++ ){
					Edge edge = firEdList.get(j);
					int index = firstQueue.indexOf( edge.endPoint );
					
					if( index >= 0  ){
						firstD.set(index, firstD.get(index) - 2*edge.edgeweight);
					}
					else{
						index = secondQueue.indexOf( edge.endPoint );
						if( index >= 0 )
							secondD.set(index, secondD.get(index) + 2*edge.edgeweight);
					}
					
				}
			}
			else{
				break;
			}
		}
		
	}
	
	public void refine( double b ) {
		try {
			PrintWriter pw = new PrintWriter(new File("temp"));
			pw.print( vertexList.size() +" ");
			int totalEdge = 0;
			Iterator<Vertex> vi = graph.keySet().iterator();
			while( vi.hasNext() ){
				Vertex v = vi.next();
				totalEdge += graph.get(v).size();
			}
			pw.print( totalEdge/2+" " );
			pw.println( "011" );
			for( int i = 0; i < vertexList.size(); i++ ){
				Vertex v = vertexList.get(i);
				pw.print((int)v.vertexWeight + " ");
				List<Edge> edgeList = graph.get(v);
				for( int j = 0; j < edgeList.size(); j++ ){
					Edge e = edgeList.get(j);
					pw.print( e.endPoint.vertexId + " "+(int)e.edgeweight + " " );
				}
				pw.println();
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void out(List<Vertex> vl, List<Double> wl){
		for( int i = 0; i < vl.size(); i++ ){
			System.out.println(vl.get(i).vertexId+"    "+wl.get(i));
		}
	}
	
	public Vertex getVertexOfList(int vertexId){
		Vertex v = new Vertex();
		v.vertexId = vertexId;
		int index = vertexList.indexOf(v);
		if( -1 == index )
			return null;
		return vertexList.get(index);
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
	
	public void growingAlgorithm( double b ){
		
//		vertexList = Partition.readGraph("astro-ph0", graph);
		vertexList = Partition.readGraph("cond-mat-2005_0", graph);
//		vertexList = Partition.readGraph("demo_8", graph);
		
		nodeIdList.add("node0");
		nodeIdList.add("node1");
		nodeIdList.add("node2");
//		nodeIdList.add("node3");
		
		List< List<Integer> > partition = new ArrayList< List<Integer> >();
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			List<Integer> vertexes = new LinkedList<Integer>();
			partition.add(i, vertexes);
		}
		 try {
//	    		Process process = Runtime.getRuntime().exec( cmds ); 
//				process.waitFor();
//				process.destroy();
				
				String outPutFile = "cond-mat-2005_0.part.3";
				File partitionFile = new File(outPutFile);
				
				if( partitionFile.exists() ){
					
					DataInputStream fileIn = new DataInputStream(new FileInputStream(partitionFile));
					
					int vertex = 1;
					String line = fileIn.readLine();
					while( null != line ){
						line = line.trim();
						int par = Integer.parseInt(line);
						if( par < nodeIdList.size() ){
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
				AN_Table.put( getVertexOfList( vertexes.get(k) ) ,  nodeIdList.get(j) );
			}
		}
		
		System.out.println( AN_Table.size() +"   "+vertexList.size() );
		System.out.println();
		
		
//		AN_Table.put(getVertexOfList(1), "node0");
//		AN_Table.put(getVertexOfList(2), "node0");
//		AN_Table.put(getVertexOfList(4), "node0");
//		AN_Table.put(getVertexOfList(6), "node0");
//		
//		AN_Table.put(getVertexOfList(3), "node1");
//		AN_Table.put(getVertexOfList(5), "node1");
//		AN_Table.put(getVertexOfList(7), "node1");
//		AN_Table.put(getVertexOfList(8), "node1");
		
		double totalWeight = 0;
		
		for( int i = 0; i < nodeIdList.size(); i++ ){
			weightOfNode.put(nodeIdList.get(i), 0D);
		}
		
		Iterator<Vertex> anIte = AN_Table.keySet().iterator();
		while( anIte.hasNext() ){
			Vertex ver = anIte.next();
			String nodeId = AN_Table.get(ver);
			weightOfNode.put( nodeId, ver.vertexWeight + weightOfNode.get(nodeId) );
		}
		
		for( int i = 0; i < vertexList.size(); i ++ ){
			totalWeight += vertexList.get(i).vertexWeight;
		}
		
		double maxNodeWeight = b*totalWeight/nodeIdList.size();
		double meanNodeWeight = totalWeight/nodeIdList.size();
//		double minNodeWeight = totalWeight*( 1-1.0*(nodeIdList.size() - 1)/nodeIdList.size() );
//		System.out.println(maxNodeWeight+"   "+minNodeWeight);
		//每个结点的weight
//		Iterator<String> weightIte = weightOfNode.keySet().iterator();
//		while( weightIte.hasNext() ){
//			String nodeId = weightIte.next();
//			System.out.println(nodeId+"   "+weightOfNode.get(nodeId));
//		}
//		System.out.println( totalWeight );
//		System.out.println( b*totalWeight/nodeIdList.size() );
		
		
		
//		changed.addAll(vertexList);
		
		long s = System.currentTimeMillis();
		
		Random ran = new Random();
		for( int i = 0; i < 100; i++ ){
			int verId = ran.nextInt( 40000 ) + 1;
			Vertex ver = getVertexOfList(verId);
			changed.add( ver );
			System.out.print( ver.vertexId+";"+AN_Table.get(ver)+"#" );
		}
		Vertex vvv = getVertexOfList(1);
		AN_Table.put(vvv, "node1");
		changed.add( vvv );
		System.out.println( "\n"+ vvv.vertexId+";"+AN_Table.get(vvv)+"#" );
		System.out.println( System.currentTimeMillis() - s );
		
		System.out.println("set: "+changed.size());
		
		anIte = graph.keySet().iterator();
		double edgeCut = 0;
		while ( anIte.hasNext() ) {
			Vertex ver = anIte.next();
			List<Edge> edgeList = graph.get( ver );
			for( int i = 0; i < edgeList.size(); i++ ){
				Edge e = edgeList.get(i);
				if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
					edgeCut += e.edgeweight;
			}
		}
		System.out.println( " refined size:"+  changed.size() );
		System.out.println( edgeCut +"    " + edgeCut/2 );
		
		s = System.currentTimeMillis();
		anIte = changed.iterator();
		while( anIte.hasNext() ){
			Vertex ver = anIte.next();
			String nodeId = AN_Table.get(ver);
			if( null != nodeId )
				weightOfNode.put( nodeId, weightOfNode.get(nodeId) - ver.vertexWeight );
		}
		
		
		Map<Vertex, String> refined = new HashMap<Vertex, String>();
		double gain = 0;
		double in = 0;
		double out = 0;
//		double weight;
		
		List<VertexToNode> priList = new LinkedList<VertexToNode>();
		
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
						String inNodeId = AN_Table.get( edge.endPoint );
						if( inNodeId.equals( nodeId ) )
							in += edge.edgeweight;
						else
							out += edge.edgeweight;
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
		
		
		
		
		
		/*
		for( int i = 0; i < nodeIdList.size() - 1; i++ ){
			nodeId = nodeIdList.get(i);
			
//			priList.clear();
//			gainList.clear();
			
			Iterator<Vertex> setIterator = changed.iterator();
			while( setIterator.hasNext() ){
				Vertex ver = setIterator.next();
				in = 0; out = 0;
				List<Edge> edgeList = graph.get(ver);
				for( int j = 0; j < edgeList.size(); j++ ){
					Edge edge = edgeList.get(j);
					if( !changed.contains(edge.endPoint) ){
						String inNodeId = AN_Table.get( edge.endPoint );
						for( int k = 0; k < nodeIdList.size(); k++ ){
							if( inNodeId.equals(nodeIdList.get(k)) ){
								if( i == k ){
									in += edge.edgeweight;
								}
								else if( i < k ){
									out += edge.edgeweight;
								}
								break;
							}
						}
					}
				}
				gain = in - out;
				int index = 0;
				Iterator<Double> gainIterator = gainList.iterator();
				while( gainIterator.hasNext() ){
					double inGain = gainIterator.next();
					if( gain >= inGain ){
						break;
					}
					index ++;
				}
				gainList.add(index, gain);
				priList.add(index, ver);
			}
			
//			for( int n = 0; n < priList.size(); n++ )
//				System.out.println(priList.get(n)+"   "+gainList.get(n));
//			
//			System.out.println();
			
			weight = weightOfNode.get(nodeId);
			
			while( !changed.isEmpty() ){
				Vertex ver = null;
				int verIndex = 0;
				Iterator<Vertex> priIte = priList.iterator();
				while( priIte.hasNext() ){
					Vertex inV = priIte.next();
					if( weight + inV.vertexWeight < maxNodeWeight ){
						ver = inV;
						break;
					}
					verIndex ++;
				}
				
				if( ver == null )
					break;
				
				weight += ver.vertexWeight;
				refined.put(ver, nodeId);
				AN_Table.put(ver, nodeId);
				
				gainList.remove( verIndex );
				priList.remove( ver );
				changed.remove( ver );
				
				List<Edge> edgeList = graph.get(ver);
				for( int j = 0; j < edgeList.size(); j++ ){
					Edge edge = edgeList.get(j);
					int index = priList.indexOf( edge.endPoint );
					if( index > 0 ){
						double newGain = gainList.get(index) + edge.edgeweight;
						gainList.remove(index);
						priList.remove(index);
						int newIndex = 0;
						Iterator<Double> gainIterator = gainList.iterator();
						while( gainIterator.hasNext() ){
							double inGain = gainIterator.next();
							if( newGain >= inGain ){
								break;
							}
							newIndex ++;
						}
						gainList.add(newIndex, newGain);
						priList.add(newIndex, edge.endPoint);
					}
				}
				
//				if( weight >= meanNodeWeight && gainList.get(0) < 0 ){
				if( weight >= meanNodeWeight ){
					break;
				}
				
			}
			weightOfNode.put(nodeId, weight);
		}
		
		nodeId = nodeIdList.get( nodeIdList.size() - 1 );
		weight = weightOfNode.get(nodeId);
		anIte = priList.iterator();
		while( anIte.hasNext() ){
			Vertex ver = anIte.next();
			weight += ver.vertexWeight;
			refined.put( ver, nodeId);
		}
		
		weightOfNode.put(nodeId, weight);
		*/
		
		System.out.println( System.currentTimeMillis() - s );
		
//		anIte = refined.keySet().iterator();
//		while( anIte.hasNext() ){
//			Vertex ver = anIte.next();
//			System.out.println( ver.toString() +"   "+refined.get(ver) );
//		}
//		
//		Iterator<String> weiIte = weightOfNode.keySet().iterator();
//		while( weiIte.hasNext() ){
//			nodeId = weiIte.next();
//			System.out.println( nodeId +"   "+weightOfNode.get(nodeId) );
//		}
		
		anIte = graph.keySet().iterator();
		edgeCut = 0;
		while ( anIte.hasNext() ) {
			Vertex ver = anIte.next();
			List<Edge> edgeList = graph.get( ver );
			for( int i = 0; i < edgeList.size(); i++ ){
				Edge e = edgeList.get(i);
				if( !AN_Table.get(ver).equals( AN_Table.get( e.endPoint ) ) )
					edgeCut += e.edgeweight;
			}
		}
		System.out.println( " refined size:"+  refined.size() );
		
		anIte = refined.keySet().iterator();
		while( anIte.hasNext() ){
			Vertex ver = anIte.next();
			System.out.print(ver.vertexId+";"+refined.get(ver)+"#");
		}
		
		System.out.println("\n\n"+ edgeCut +"    " + edgeCut/2 );
		
	}
	
	public static void main(String[] args) {
		KLAlgorithm kl = new KLAlgorithm();
//		kl.ssn( 1.1 );
//		System.out.println(System.currentTimeMillis());
//		kl.refine(1.1);
//		System.out.println(System.currentTimeMillis());
		
		kl.growingAlgorithm(1.03);
		
	}
	
	
}