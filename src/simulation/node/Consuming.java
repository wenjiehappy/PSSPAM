package simulation.node;

import simulation.market.ProtocolString;

public class Consuming {
	
	public static void consume( int weight ){
		double sum = 0;
		for(double i = 0; i < ProtocolString.N*weight; i++){
			double temp1 = Math.pow((i + 0.5) / ProtocolString.N*weight, 2);
			double temp2 = (double)4 / (1 + temp1);
			sum = sum + temp2;
		}
		double result = sum / ProtocolString.N*weight;
	}
	
//	public static void main(String[] args){
//		long s_time = System.currentTimeMillis();
//		Consuming con = new Consuming();
//		for( int i = 0; i < 10000; i++ )
//			con.consume(1);
//		long e_time = System.currentTimeMillis();
//		System.out.println("Total running time is " + (e_time - s_time) + "ms" );
//	}
}