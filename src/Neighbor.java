import java.util.HashMap;

public class Neighbor {
	private Record server;
	private HashMap<String, Integer> distanceVector;
	public static final Integer infinity = 999999;
	
	public Neighbor(Record server, HashMap<String, Integer> distanceVector) {
		this.server = server;
		this.distanceVector = distanceVector;
	}
	
	public Integer getDistance(String server){
		Integer distance = distanceVector.get(server);
		if (distance == null)
			distance = infinity;
		return distance;
	}
}
