import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;


public class Database {
	
	private List<Record> servers;
	private String path;
	private List<Record> clients;
	
	public Database(String path) {
		this.path = path;
		this.servers = new ArrayList<Record>();
		this.clients = new ArrayList<Record>();
		readServers(path);
	}
	
	private Record formRecord(String recordString){
		String[] parts = recordString.split("\\s");
		if (parts.length != 3){ //sanity check
			System.out.println("Corrupted record");
		}
		return new Record(parts[0], new Integer(parts[1]));
	}
	
	private void readServers(String path){
		try {
			FileInputStream iStream = new FileInputStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(iStream)));
			String line = null;
			while ((line = reader.readLine()) != null){
				servers.add(formRecord(line));
			}
			reader.close();
		} catch (Exception e) {
			System.out.println("Database file not found. Will create on writing");
		}
	}
	
	public List<Record> retrieveClients(String name){
		List<Record> results = new ArrayList<Record>();
		for (Record record : clients){
			if (record.matches(name, null, null)){
				results.add(record);
			}
		}
		return results;
	}
	
	public String insertClient(String name, String ipAddress, Integer port){
		if (retrieveClients(name).size() > 0)
			return "ERROR: client name has already been used";
		clients.add(new Record(name, ipAddress, port));
		return "Client resgistered successfully";
	}
	
	public String deleteClient(String name){
		List<Record> results = retrieveClients(name);
		if (results.isEmpty())
			return "ERROR: record not found";
		clients.remove(results.get(0));
		return "Client unregistered successfully";
	}
	
	public List<Record> retrieveServers(String name, String ipAddress, Integer port){
		List<Record> results = new ArrayList<Record>();
		for (Record record : servers){
			if (record.matches(name, ipAddress, port)){
				results.add(record);
			}
		}
		return results;
	}
	
	public String insertServer(String ipAddress, Integer port){
		if (retrieveServers("*", ipAddress, port).size() > 0)
			return "ERROR: server IP:port combination has already been used";
		servers.add(new Record(ipAddress, port));
		return "Record Added successfully";
	}
	
	public String deleteServer(String name, String ipAddress, Integer port){
		List<Integer> occurences = new ArrayList<Integer>();
		for (int i=0; i< servers.size(); i++){
			if (servers.get(i).matches(name, ipAddress, port)){
				occurences.add(i);
			}
		}
		for (int o : occurences){
			servers.remove(o);
		}
		if (occurences.size() == 0){
			return "ERROR: record not found";
		} else {
			return "" + occurences.size() + " record(s) deleted successfully";
		}
	}
	
	public String save(){
		try {
			FileOutputStream oStream = new FileOutputStream(path);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(oStream)));
			for (Record record : servers){
				writer.write(record.toString() + "\n");
			}
			writer.close();
		} catch (Exception e) {
			System.out.println("Error writing Database file");
			e.printStackTrace();
			return "Error writing Database file";
		}
		return "Database saved successfully";
	}
	
}
