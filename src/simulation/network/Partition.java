package simulation.network;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Partition {
	
	/**
	 * @param inputFileName
	 * @param partitionNum
	 * @param arguments ���������֮��ֻ�ܿ�һ��ո�
	 * @return ���طֳɵ�ÿ�������е㹹�ɵ�List��List
	 */
	public static List< List<Integer> > paitition(String inputFileName, int partitionNum, String arguments){
		
		String[] cmds = null;
		if( null == arguments ){
			cmds = new String[ 3 ];
			cmds[0] = "gpmetis";
			cmds[1] = inputFileName;
			cmds[2] = partitionNum+"";
		}
		else{
			String [] args = arguments.split(" ");
			cmds = new String[ args.length + 3 ];
			cmds[0] = "gpmetis";
			cmds[1] = inputFileName;
			cmds[2] = partitionNum+"";
			for( int i = 0; i < args.length; i++ ){
				cmds[i+3] = args[i];
			}
		}
		
		List< List<Integer> > partition = new ArrayList< List<Integer> >();
		
		for( int i = 0; i < partitionNum; i++ ){
			List<Integer> vertexes = new LinkedList<Integer>();
			partition.add(i, vertexes);
		}
		
        try {
    		Process process = Runtime.getRuntime().exec( cmds ); 
			process.waitFor();
			process.destroy();
			
			String outPutFile = inputFileName+".part."+partitionNum;
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
		}catch (InterruptedException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
        return partition;
	}
	
	
	public static List< List<Integer> > ranPaitition( List<Vertex> vertexList, int partitionNum ){
		
		List< List<Integer> > result = new ArrayList<List<Integer>>();
//		for( int i = 0; i < vertexList.size(); i++ ){
//			List<Integer> par = new ArrayList<Integer>();
//			result.add(par);
//		}
		
		double totalWeight = 0;
		for( int i = 0; i < vertexList.size(); i++ ){
			totalWeight += vertexList.get(i).vertexWeight;
		}
		
		double meanWeight = totalWeight / partitionNum;
		
		int index = 0;
		for( int i = 0; i < partitionNum - 1; i++ ){
			double parWeight = 0;
			List<Integer> parList = new ArrayList<Integer>();
			while( parWeight < meanWeight ){
				Vertex ver = vertexList.get(index);
				parList.add( ver.vertexId );
				parWeight += ver.vertexWeight;
				index ++;
			}
//			System.out.println( parWeight );
			result.add(parList);
		}
		double parWeight = 0;
		List<Integer> parList = new ArrayList<Integer>();
		for( ; index < vertexList.size(); index ++ ){
			parList.add( vertexList.get(index).vertexId );
			parWeight += vertexList.get(index).vertexWeight;
		}
//		System.out.println( parWeight );
		result.add(parList);
		
		return result;
	}
	
	
	public static void main(String[] args) {
//		Map<Vertex, List<Edge>> graph = readGraph("demo3");
		
		List<Vertex> vertexList = new ArrayList<Vertex>();
		
		/*
		Vertex v = new Vertex();
		v.vertexId = 1;
		v.vertexWeight = 2;
		vertexList.add(v);
		
		v = new Vertex();
		v.vertexId = 2;
		v.vertexWeight = 3;
		vertexList.add(v);
		
		v = new Vertex();
		v.vertexId = 4;
		v.vertexWeight = 2;
		vertexList.add(v);
		
		Vertex ver = new Vertex();
		ver.vertexId = 2;
		
		System.out.println( vertexList.indexOf( ver ) );
		*/
		
		
		/*
		//测试随机划分
		Map<Vertex, List<Edge>> graph = new HashMap<Vertex, List<Edge>>();
		String filePath = "C:\\Users\\chx\\Desktop\\论文\\代码\\数据\\cond-mat-2005_0\\cond-mat-2005_0";
		vertexList = Partition.readGraph( filePath, graph );
		List<List<Integer>> result = ranPaitition(vertexList, 4);
		try {
			System.setOut(new PrintStream(new File("partition")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		for( int i = 0; i < result.size(); i++ ){
			List<Integer> parList = result.get(i);
			System.out.print( parList.size() +" ");
			for( int j = 0; j < parList.size(); j++ ){
				System.out.print( parList.get(j) +" " );
			}
			System.out.println();
		}
		*/
		
	}
	
	public static List<Vertex> readGraph( String inputFileName, Map<Vertex, List<Edge> > result ){
		List<Vertex> verList = new ArrayList<Vertex>();
		File partitionFile = new File(inputFileName);
		 try {
			 if( partitionFile.exists() ){
					DataInputStream fileIn = new DataInputStream(new FileInputStream(partitionFile));
					
					String line = fileIn.readLine();
					line = line.trim();
					String[] cmds = line.split(" ");
					int vertexNum = Integer.parseInt( cmds[0] );
					int edgeNum = Integer.parseInt( cmds[1] );
					String graphType = null;
					
					if( cmds.length >= 3 && null != cmds[2] && cmds[2].trim().length() > 0 )
						graphType = cmds[2].trim();
					
					for( int i = 0; i < vertexNum; i++ ){
						Vertex ver = new Vertex();
						ver.vertexId = i + 1;
						verList.add(ver);
					}
					
					if( null == graphType ){	//edges and vertexes both don't have weight
						for( int i = 0; i < vertexNum; i++ ){
							List<Edge> links = new ArrayList<Edge>();
							
							Vertex ver = verList.get(i);
							
							line = fileIn.readLine();
							if( null != line && line.trim().length() > 0 ){
								line = line.trim();
								String[] veres = line.split(" ");
								if( null != veres ){
									for( int j = 0; j < veres.length; j++ ){
										String verstr = veres[j];
										if( null != verstr && verstr.trim().length() > 0 ){
											Edge edge = new Edge();
											edge.startPoint = ver;
											edge.endPoint = verList.get( Integer.parseInt(verstr) - 1 );
											links.add( edge );
										}
									}
								}
							}
							result.put(ver, links);
						}
					}
					else if( "001".equals(graphType) ){	//edges have weight, vertexes don't
						for( int i = 0; i < vertexNum; i++ ){
							List<Edge> links = new ArrayList<Edge>();
							
							Vertex ver = verList.get(i);
							
							line = fileIn.readLine();
							if( null != line && line.trim().length() > 0 ){
								line = line.trim();
								String[] veres = line.split(" ");
								if( null != veres ){
									for( int j = 0; j < veres.length; j++ ){
										String verstr = veres[j];
										if( null != verstr && verstr.trim().length() > 0 ){
											Edge edge = new Edge();
											edge.startPoint = ver;
											edge.endPoint = verList.get( Integer.parseInt(verstr) - 1 );
											edge.endPoint.vertexId = Integer.parseInt(verstr);
											j++;
											edge.edgeweight = Double.parseDouble( veres[j].trim() );
											links.add( edge );
										}
									}
								}
							}
							result.put(ver, links);
						}
					}
					else if( "010".equals(graphType) ){	//vertexes have weight, edges don't
						for( int i = 0; i < vertexNum; i++ ){
							List<Edge> links = new ArrayList<Edge>();

							Vertex ver = verList.get(i);
							
							line = fileIn.readLine();
							if( null != line && line.trim().length() > 0 ){
								line = line.trim();
								String[] veres = line.split(" ");
								if( null != veres ){
									ver.vertexWeight = Double.parseDouble( veres[0].trim() );
									for( int j = 1; j < veres.length; j++ ){
										String verstr = veres[j];
										if( null != verstr && verstr.trim().length() > 0 ){
											Edge edge = new Edge();
											edge.startPoint = ver;
											edge.endPoint = verList.get( Integer.parseInt(verstr) - 1 );
											links.add( edge );
										}
									}
								}
							}
							result.put(ver, links);
						}
					}
					else if( "011".equals(graphType) ){	//edges and vertexes both have weight
						for( int i = 0; i < vertexNum; i++ ){
							List<Edge> links = new ArrayList<Edge>();
							
							Vertex ver = verList.get(i);
							
							line = fileIn.readLine();
							if( null != line && line.trim().length() > 0 ){
								line = line.trim();
								String[] veres = line.split(" ");
								if( null != veres ){
									ver.vertexWeight = Double.parseDouble( veres[0].trim() );
									for( int j = 1; j < veres.length; j++ ){
										String verstr = veres[j];
										if( null != verstr && verstr.trim().length() > 0 ){
											Edge edge = new Edge();
											edge.startPoint = ver;
											edge.endPoint = verList.get( Integer.parseInt(verstr) - 1 );
											j++;
											edge.edgeweight = Double.parseDouble( veres[j].trim() );
											links.add( edge );
										}
									}
								}
							}
							result.put(ver, links);
						}
					}
				}else{
					System.out.println(" graph file no where ");
				}
		 }catch (IOException e) {
			e.printStackTrace();
		}
		return verList;
	}
	
}
