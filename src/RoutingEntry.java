public class RoutingEntry {
	private String destination;
	private Neighbor next;
	private Integer cost;
	
	public RoutingEntry(String destination, Neighbor next, Integer cost) {
		this.destination = destination;
		this.next = next;
		this.cost = cost;
	}
	
	public String destination(){
		return this.destination;
	}
	
	public Neighbor next(){
		return this.next;
	}
	
	public Integer cost(){
		return this.cost;
	}
	
}
