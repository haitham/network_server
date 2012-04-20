public class RoutingEntry {
	String destination;
	String next;
	Integer cost;
	
	public RoutingEntry(String destination, String next, Integer cost) {
		this.destination = destination;
		this.next = next;
		this.cost = cost;
	}
}
