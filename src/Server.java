import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Server {
	public static final Integer infinity = 999999;
	
	private Integer port;
	private Boolean alive;
	private Database database;
	private String serverName;
	private HashMap<String, Integer> distanceVector;
	private List<Neighbor> neighbors;
	private HashMap<String, RoutingEntry> routingMap;
	
	public Server(Integer port, String name, String databasePath) {
		this.port = port;
		this.alive = true;
		this.serverName = name;
		this.distanceVector = new HashMap<String, Integer>();
		this.neighbors = new ArrayList<Neighbor>();
		this.routingMap = new HashMap<String, RoutingEntry>();
		if (databasePath == null){
			databasePath = "datafile";
		}
		database = new Database(databasePath);
		try {
			System.out.println("===Server started, now listening===");
			final DatagramSocket socket = new DatagramSocket(port);
			while(alive){
				//read
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				final String command = new String(packet.getData(), 0, packet.getLength());
				final InetAddress clientAddress = packet.getAddress();
				final int clientPort = packet.getPort();
				System.out.println(">> From " + clientAddress.toString() + ":" + clientPort + " > " + command);
				
				//Special case for kill - no thread
				if (command.trim().startsWith("Kill")){
					//process
					String result = processCommand(command, clientAddress);
					
					//write
					buf = result.getBytes();
					packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
					try {
						socket.send(packet);
					} catch (IOException e) {
						System.out.println("Error writing to socket");
					}
					return;
				}
				
				// Spawn a thread for processing the incoming command
				new Thread(new Runnable() {
					public void run() {
						//process
						String result = processCommand(command, clientAddress);
						
						//write
						byte[] buf = result.getBytes();
						DatagramPacket packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
						try {
							socket.send(packet);
						} catch (IOException e) {
							System.out.println("Error writing to socket");
						}
					}
				}, "CommandProcessor").start();
			}
			socket.close();
		} catch (SocketException e) {
			System.out.println("Error opening a socket on port " + port);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error reading form socket");
			e.printStackTrace();
		}
	}

	private String processCommand(String command, InetAddress clientAddress) {
		String[] parts = command.split("\\s+");
		if ("Kill".equals(parts[0].trim())){
			//Kill command
			alive = false;
			return database.save() + "\nServer dying"; 
		} else if (parts[0].trim().equals("Insert")){
			//Insert command
			if (parts.length != 3)
				return "ERROR: Wrong number of parameters";
			return database.insertServer(parts[1], new Integer(parts[2]));
		} else if (parts[0].trim().equals("Find")){
			//Find command
			if (parts.length != 3)
				return "ERROR: Wrong number of parameters";
			List<Record> records = database.retrieveServers(parts[1], parts[2], null);
			StringBuffer buffer = new StringBuffer().append(records.size()).append(" record(s) found");
			for (Record record : records){
				buffer.append("\n").append(record.toString());
			}
			return buffer.toString();
		} else if (parts[0].trim().equals("Delete")){
			//Delete command
			if (parts.length < 1 || parts.length > 3)
				return "ERROR: Wrong number of parameters";
			String name = "*";
			String ipAddress = parts.length < 2 ? null : parts[1];
			Integer port = parts.length < 3 ? null : new Integer(parts[2]);
			return database.deleteServer(name, ipAddress, port);
		} else if (parts[0].trim().equals("Link")){
			// Link command
			List<Record> records;
			if (parts.length == 2){
				records = database.retrieveServers(parts[1], null, null);
			} else if (parts.length == 3){
				records = database.retrieveServers("*", parts[1], new Integer(parts[2]));
			} else {
				return "ERROR: Wrong number of parameters";
			}
			if (records.isEmpty())
				return "ERROR: server name not found";
			return link(records.get(0));
		} else if (parts[0].trim().equals("Unlink")){
			// Unlink command
			List<Record> records;
			if (parts.length == 2){
				records = database.retrieveServers(parts[1], null, null);
			} else if (parts.length == 3){
				records = database.retrieveServers("*", parts[1], new Integer(parts[2]));
			} else {
				return "ERROR: Wrong number of parameters";
			}
			if (records.isEmpty())
				return "ERROR: server name not found";
			return unlink(records.get(0));
		} else if (parts[0].trim().equals("Register")){
			//Register command
			if (parts.length != 3)
				return "ERROR: Wrong number of parameters";
			return database.insertClient(parts[1], clientAddress.toString().replaceFirst("/", ""), new Integer(parts[2]));
		} else if (parts[0].trim().equals("Unregister")){
			//Unregister command
			if (parts.length != 2)
				return "ERROR: Wrong number of parameters";
			return database.deleteClient(parts[1]);
		} else if (parts[0].trim().equals("List")){
			// List command
			if (parts.length == 3)
				return listClients(parts[2], parts[1]);
			else if (parts.length == 2)
				return listClients("self", parts[1]);
			else
				return "ERROR: Wrong number of parameters";
		} else if (parts[0].trim().equals("Send")){
			// Send command
			String[] lines = command.split("\n");
			parts = lines[0].split("\\s+");
			if (parts.length != 3)
				return "ERROR: Wrong number of parameters";
			String message = "";
			for (int i=1; i<lines.length; i++)
				message = message + lines[i] + "\n";
			return sendMessage(parts[2], parts[1], message);
		} else if (parts[0].trim().equals("ServerLink")){
			return serverLink(command, clientAddress);
		} else if (parts[0].trim().equals("ServerUnlink")){
			return serverUnlink(command);
		} else if (parts[0].trim().equals("DistanceInfo")){
			return receiveDistanceInfo(command);
		} else if (parts[0].trim().equals("ForwardTo")){
			String message = command.split("\\+\\-\\+")[1];
			if (parts[1].split("\\+\\-\\+")[0].trim().equals(this.serverName))
				return processCommand(message, clientAddress);
			else
				return forward(parts[1].split("\\+\\-\\+")[0].trim(), message);
		} else{
			return "Unknown command";
		}
	}

	private String sendMessage(String serverName, String clientName, String message) {
		// A map of server => clients
		HashMap<String, List<String>> recepients = new HashMap<String, List<String>>();
		// check for self server
		Boolean self = false;
		if (serverName.equals("*") || serverName.toLowerCase().matches("(\"(.+\\,)?)?self((\\,.+)?\")?")){
			recepients.put("self", sendLocalMessage(clientName, message));
			self = true;
		}
		//retrieving other servers
		List<Record> servers = database.retrieveServers(serverName, null, null);
		if (serverName != "*"){
			Integer serverCount = serverName.length() - serverName.replaceAll("\\,", "").length() + 1;
			if (self)
				serverCount -= 1;
			if (servers.size() < serverCount)
				return "ERROR: Unknown server name(s)";
		}
		// sending to clients from other linked servers
		String unlinkedServers = "";
		String deadServers = "";
		for (Record server : servers){
			if (!server.isLinked()){
				unlinkedServers = unlinkedServers + " " + server.getUrl();
				continue;
			}
			if (recepients.get(server.getName()) != null)
				continue;
			try {
				recepients.put(server.getName(), sendRemoteMessage(server, clientName, message));
			} catch (SocketTimeoutException e) {
				deadServers = deadServers + " " + server.getName();
			}
		}
		
		//returning results in literal manner
		StringBuffer buffer = new StringBuffer();
		Integer resultCount = 0;
		for (String server : recepients.keySet()){
			List<String> clients = recepients.get(server);
			resultCount += clients.size();
			for (String client : clients){
				buffer.append(server).append("/").append(client).append("\n");
			}
		}
		String warning = "";
		if (!unlinkedServers.isEmpty())
			warning += " - WARNING: unlinked servers were excluded: [" + unlinkedServers + "]";
		if (!deadServers.isEmpty())
			warning += " - WARNING: Dead linked servers: [" + deadServers + "]";
		if (resultCount == 0)
			return "Error: no matching recepients available" + warning + "\n" + buffer.toString();
		else
			return "Delivered to " + resultCount + " recepients" + warning + "\n" + buffer.toString();
	}

	private List<String> sendRemoteMessage(Record server, String clientName, String message) throws SocketTimeoutException {
		String response = sendAndReceive(server.getIpAddress(), server.getPort(), "Send " + clientName + " self\n" + message);
		if (response.trim().startsWith("ERROR"))
			throw new SocketTimeoutException();
		String[] responseLines = response.split("\n");
		List<String> clients = new ArrayList<String>();
		for (int i=1; i<responseLines.length; i++){
			if (!responseLines[i].trim().isEmpty() && responseLines[i].indexOf("/") > 0)
				clients.add(responseLines[i].trim().split("/")[1]);
		}
		return clients;
	}

	private List<String> sendLocalMessage(String clientName, String message) {
		List<String> recepients = new ArrayList<String>();
		for (Record client : database.retrieveClients(clientName)){
			if (sendAndReceive(client.getIpAddress(), client.getPort(), message).indexOf("ERROR") < 0)
				recepients.add(client.getName());
		}
		return recepients;
	}
	
	private String[] splitNameList(String nameList){
		return nameList.replaceAll("\"", "").split("\\s*\\,\\s*");
	}
	
	private String unreachableServers(List<String> servers){
		String unreachableServers = "";
		for (String name : servers){
			if (!name.equals("self") && !name.equals(this.serverName) && !name.equals("*") && this.distanceVector.get(name) == null)
				unreachableServers = unreachableServers + " " + name;
		}
		if (!unreachableServers.isEmpty())
			unreachableServers = "[" + unreachableServers + "]";
		return unreachableServers;
	}

	private String listClients(String serverName, String clientName) {
		// A map of server => clients
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		
		List<String> serverNames = Arrays.asList(splitNameList(serverName));
		String unreachableServers = unreachableServers(serverNames);
		if (!unreachableServers.isEmpty())
			return "ERROR - unreachable servers: " + unreachableServers;
		
		List<String> servers = new ArrayList<String>();
		for (String server : routingMap.keySet()){
			if (serverName.equals("*") || serverNames.contains(server))
				servers.add(server);
		}
		
		// check for self server
		if (serverName.equals("*") || serverNames.contains("self") || serverNames.contains(this.serverName)){
			results.put("self", listLocalClients(clientName));
		}
		
		// remote servers
		for (String server : servers){
			results.put(server, listRemoteClients(server, clientName));
		}
		
		//returning results in literal manner
		StringBuffer buffer = new StringBuffer();
		Integer resultCount = 0;
		for (String server : results.keySet()){
			List<String> clients = results.get(server);
			resultCount += clients.size();
			for (String client : clients){
				buffer.append(server).append("/").append(client).append("\n");
			}
		}
		return "" + resultCount + " results found" + "\n" + buffer.toString();
	}

	private String forward(String destination, String message) {
		RoutingEntry route = routingMap.get(destination);
		Record next = route.next().server();
		return sendAndReceive(next.getIpAddress(), next.getPort(), "ForwardTo " + destination + "+-+" + message);
	}
	
	private List<String> listRemoteClients(String server, String clientName) {
		String response = forward(server, "List " + clientName + " self");
		List<String> clients = new ArrayList<String>();
		if (response.trim().startsWith("ERROR"))
			return clients;
		String[] responseLines = response.split("\n");
		for (int i=1; i<responseLines.length; i++){
			if (!responseLines[i].trim().isEmpty() && responseLines[i].indexOf("/") > 0)
				clients.add(responseLines[i].trim().split("/")[1]);
		}
		return clients;
	}

	private List<String> listLocalClients(String clientName) {
		List<String> clients = new ArrayList<String>();
		for (Record client : database.retrieveClients(clientName))
			clients.add(client.getName());
		return clients;
	}
	
	private String distanceVectorString(){
		String result = "";
		for (String name : distanceVector.keySet()){
			result = result + " " + name + ":" + distanceVector.get(name);
		}
		return result;
	}
	
	private String serverLink(String command, InetAddress address) {
		String[] parts = command.trim().split("\\s+");
		List<Record> matches = database.retrieveServers("*", address.toString().replaceFirst("/", ""), new Integer(parts[2]));
		if (matches.isEmpty()){
			database.insertServer(address.toString().replaceFirst("/", ""), new Integer(parts[2]));
			matches = database.retrieveServers("*", address.toString().replaceFirst("/", ""), new Integer(parts[2]));
		}
		Record server = matches.get(0);
		server.setName(parts[1]);
		server.setLinked(true);
		HashMap<String, Integer> vector = new HashMap<String, Integer>();
		for (int i=3; i<parts.length; i++){
			vector.put(parts[i].split("\\:")[0], new Integer(parts[i].split("\\:")[1]));
		}
		neighbors.add(new Neighbor(server, vector));
		recomputeRouting();
		recomputeDistances(server.getName());
		return this.serverName + " " + distanceVectorString();
	}
	
	private Neighbor findNeighbor(String name){
		for (Neighbor neighbor: neighbors){
			if (neighbor.server().getName().equals(name))
				return neighbor;
		}
		return null;
	}
	
	private String link(Record server) {
		if (server.isLinked())
			return "ERROR: server was already linked - command ignored";
		String message = "ServerLink " + this.serverName + " " + this.port + " " + distanceVectorString();
		String response = sendAndReceive(server.getIpAddress(), server.getPort(), message);
		if ("ERROR".equals(response))
			return "ERROR: unable to connect to " + server.getUrl();
		if ("NAMEERROR".equals(response))
			return "ERROR: name duplication occurred in network";
		else {
			String[] parts = response.trim().split("\\s+");
			server.setName(parts[0]);
			server.setLinked(true);
			HashMap<String, Integer> vector = new HashMap<String, Integer>();
			for (int i=1; i<parts.length; i++){
				vector.put(parts[i].split("\\:")[0], new Integer(parts[i].split("\\:")[1]));
			}
			neighbors.add(new Neighbor(server,vector));
			recomputeRouting();
			recomputeDistances(null);
			return server.getName() + " linked successfully";
		}
	}
	
	private String receiveDistanceInfo(String command) {
		String[] parts = command.trim().split("\\s+");
		Neighbor neighbor = findNeighbor(parts[1]);
		if (neighbor == null)
			return "ERROR: " + parts[1] + " is not a neighbor";
		HashMap<String, Integer> vector = new HashMap<String, Integer>();
		for (int i=2; i<parts.length; i++){
			vector.put(parts[i].split("\\:")[0], new Integer(parts[i].split("\\:")[1]));
		}
		neighbor.setDistanceVector(vector);
		recomputeRouting();
		recomputeDistances(null);
		return "OK: Distance info adjusted";
	}
	
	private void recomputeDistances(String except) {
		HashMap<String, Integer> newDistanceVector = new HashMap<String, Integer>();
		for (String server : routingMap.keySet()){
			newDistanceVector.put(server, routingMap.get(server).cost());
		}
		if (!newDistanceVector.equals(this.distanceVector)){
			this.distanceVector = newDistanceVector;
			propagateDistanceVector(except);
		}
	}

	private void propagateDistanceVector(String except) {
		for (Neighbor neighbor : neighbors){
			if (neighbor.server().getName().equals(except))
				continue;
			String message = "DistanceInfo " + this.serverName + " " + distanceVectorString();
			sendAndReceive(neighbor.server().getIpAddress(), neighbor.server().getPort(), message);
		}
	}

	private void recomputeRouting(){
		routingMap = new HashMap<String, RoutingEntry>();
		for (String server : reachableServers()){
			Integer minDistance = infinity;
			Neighbor minNeighbor = null;
			for (Neighbor neighbor : neighbors){
				Integer distance = neighbor.getDistance(server);
				if (distance < minDistance){
					minDistance = distance;
					minNeighbor = neighbor;
				}
			}
			if (minDistance > 10) // count to infinity
				continue;
			routingMap.put(server, new RoutingEntry(server, minNeighbor, minDistance + 1));
		}
	}
	
	private List<String> reachableServers(){
		List<String> servers = new ArrayList<String>();
		for (Neighbor neighbor : neighbors){
			if (!servers.contains(neighbor.server().getName())){
				servers.add(neighbor.server().getName());
			}
			for (String name : neighbor.reachableServers()){
				if (!serverName.equals(name) && !servers.contains(name)){
					servers.add(name);
				}
			}
		}
		return servers;
	}
	
	private String unlink(Record server){
		if (!server.isLinked())
			return "ERROR: server was not linked - command ignored";
		String message = "ServerUnlink " + this.serverName;
		sendAndReceive(server.getIpAddress(), server.getPort(), message);
		server.setLinked(false);
		neighbors.remove(findNeighbor(server.getName()));
		recomputeRouting();
		recomputeDistances(null);
		return server.getName() + " unlinked successfully";
	}
	
	private String serverUnlink(String command) {
		String[] parts = command.trim().split("\\s+");
		Neighbor neighbor = findNeighbor(parts[1]);
		neighbor.server().setLinked(false);
		neighbors.remove(neighbor);
		recomputeRouting();
		recomputeDistances(null);
		return "OK";
	}
	
	private String sendAndReceive(String ipAddress, Integer port, String command) {
		String result = "";
		try {
			InetAddress address = InetAddress.getByName(ipAddress);
			DatagramSocket socket = new DatagramSocket();
//			socket.setSoTimeout(2000);
			System.out.println("<<<< To " + ipAddress + ":" + port + " > " + command);
			//sending
			DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, port);
			socket.send(packet);
			//receiving
			byte[] buf = new byte[256];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			result = new String(packet.getData(), 0, packet.getLength());
		} catch (Exception e) {
			result = "ERROR";
        }
		System.out.println(">>>> From " + ipAddress + ":" + port + " > " + result);
		return result;
	}
	
}
