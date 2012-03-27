
public class Record {
	private String name;
	private IPAddress ipAddress;
	private Integer port;
	private Boolean linked;
	
	public Record(String name, String ipAddress, Integer port) {
		this.name = name;
		this.ipAddress = new IPAddress(ipAddress);
		this.port = port;
		this.linked = false;
	}
	
	public void setLinked(Boolean linked){
		this.linked = linked;
	}
	
	public Boolean isLinked(){
		return linked;
	}
	
	public String getName() {
		return name;
	}

	public String getIpAddress() {
		return ipAddress.toString();
	}

	public Integer getPort() {
		return port;
	}
	
	/*
	 * Detects whether this record matches the given parameters
	 * name is mandatory, others are matched only if provided
	 */
	public Boolean matches(String name, String ipAddress, Integer port){
		if (!"*".equals(name) && !this.name.equals(name)){
			return false;
		}
		if (ipAddress != null && !this.ipAddress.matches(ipAddress)){
			return false;
		}
		if (port != null && this.port != port){
			return false;
		}
		return true;
	}
	
	public String toString(){
		return name + " " + ipAddress.toString() + " " + port;
	}
	
}
