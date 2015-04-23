package simulation.network;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


public class ReadPartition {
	public static void main(String[] args) {
		
		int parNum = 10;
		
		Vector< List<Integer> > partition = new Vector< List<Integer> >();
		
		for( int i = 0; i < parNum; i++ ){
			List<Integer> vertexes = new LinkedList<Integer>();
			partition.add(i, vertexes);
		}
		
		String fileName = "cond-mat-2005_0.part.10";
		File partitionFile = new File(fileName);
		
		if( partitionFile.exists() ){
			try {
				DataInputStream fileIn = new DataInputStream(new FileInputStream(partitionFile));
				
				int i = 1;
				
				String line = fileIn.readLine();
				while( null != line ){
					line = line.trim();
					int par = Integer.parseInt(line);
					if( par < parNum ){
						List<Integer> vertexes = partition.get(par);
						vertexes.add(i);
					}
					
					line = fileIn.readLine();
					i++;
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			File outFile = new File("out");
			PrintStream pt = null;
			try {
				pt = new PrintStream(outFile);
				System.setOut( pt );
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			for( int i = 0; i < parNum; i++ ){
				List<Integer> vertexes = partition.get(i);
				System.out.print(i+":");
				for( int j = 0; j < vertexes.size(); j++ ){
					System.out.print(vertexes.get(j)+" ");
				}
				System.out.println();
			}
			if( null != pt ){
				pt.close();
			}
		}
	}
}
