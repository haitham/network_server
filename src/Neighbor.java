import java.util.HashMap;
import java.util.Set;

public class Neighbor {
	private Record server;
	private HashMap<String, Integer> distanceVector;
	public static final Integer infinity = 999999;
	
	public Neighbor(Record server, HashMap<String, Integer> distanceVector) {
		this.server = server;
		this.distanceVector = distanceVector;
	}
	
	public Integer getDistance(String server){
		if (this.server.getName().equals(server))
			return 0;
		Integer distance = distanceVector.get(server);
		if (distance == null)
			return infinity;
		return distance;
	}
	
	public Set<String> reachableServers(){
		return distanceVector.keySet();
	}
	
	public Record server(){
		return this.server;
	}
	
	public void setDistanceVector(HashMap<String, Integer> vector){
		this.distanceVector = vector;
	}
}
