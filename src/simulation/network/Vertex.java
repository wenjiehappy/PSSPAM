package simulation.network;

public class Vertex {
	public int vertexId;
	public double vertexWeight = 1;
	
	@Override
	public int hashCode() {
		return this.vertexId;
	}
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Vertex re = new Vertex();
		re.vertexId = vertexId;
		re.vertexWeight = vertexWeight;
		return re;
	}
	@Override
	public boolean equals(Object obj) {
		Vertex ve = (Vertex)obj;
		if( null == ve ){
			return false;
		}
		if( ve.vertexId == vertexId ){
			return true;
		}
		return false;
	}
}
