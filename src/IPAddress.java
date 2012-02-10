/*
 * An encapsulation of the IP address logic needed in the server
 */
public class IPAddress {
	private Integer[] parts;
	
	public IPAddress(String address) {
		this.parts = new Integer[4];
		String[] parts = address.split("\\.");
		for (int i=0; i<4; i++){
			this.parts[i] = new Integer(parts[i].trim());
		}
	}
	
	/*
	 * Detects whether this address matches the given address, including wildcards
	 */
	public Boolean matches(String address){
		String[] parts = address.split("\\.");
		for (int i=0; i<4; i++){
			if (!"*".equals(parts[i].trim()) && new Integer(parts[i]) != this.parts[i] )
				return false;
		}
		return true;
	}
	
	public String toString(){
		return new StringBuffer(this.parts[0]).append(".").append(this.parts[1]).append(".").append(this.parts[2]).append(".").append(this.parts[3]).toString();
	}
}
