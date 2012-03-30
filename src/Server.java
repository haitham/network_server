import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Server {
	
	private Integer port;
	private Boolean alive;
	private Database database;
	
	public Server(Integer port, String databasePath) {
		this.port = port;
		this.alive = true;
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
			if (parts.length != 4)
				return "ERROR: Wrong number of parameters";
			return database.insertServer(parts[1], parts[2], new Integer(parts[3]));
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
			if (parts.length < 2 || parts.length > 4)
				return "ERROR: Wrong number of parameters";
			String name = parts[1];
			String ipAddress = parts.length < 3 ? null : parts[2];
			Integer port = parts.length < 4 ? null : new Integer(parts[3]);
			return database.deleteServer(name, ipAddress, port);
		} else if (parts[0].trim().equals("Link")){
			// Link command
			if (parts.length != 2)
				return "ERROR: Wrong number of parameters";
			return link(parts[1]);
		} else if (parts[0].trim().equals("Unlink")){
			// Unlink command
			if (parts.length != 2)
				return "ERROR: Wrong number of parameters";
			return unlink(parts[1]);
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
			if (parts.length != 3)
				return "ERROR: Wrong number of parameters";
			return listClients(parts[2], parts[1]);
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
				unlinkedServers = unlinkedServers + " " + server.getName();
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

	private String listClients(String serverName, String clientName) {
		// A map of server => clients
		HashMap<String, List<String>> results = new HashMap<String, List<String>>();
		// check for self server
		Boolean self = false;
		if (serverName.equals("*") || serverName.toLowerCase().matches("(\"(.+\\,)?)?self((\\,.+)?\")?")){
			results.put("self", listLocalClients(clientName));
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
		// listing clients from other linked servers
		String unlinkedServers = "";
		String deadServers = "";
		for (Record server : servers){
			if (!server.isLinked()){
				unlinkedServers = unlinkedServers + " " + server.getName();
				continue;
			}
			if (results.get(server.getName()) != null)
				continue;
			try {
				results.put(server.getName(), listRemoteClients(server, clientName));
			} catch (SocketTimeoutException e) {
				deadServers = deadServers + " " + server.getName();
			}
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
		String warning = "";
		if (!unlinkedServers.isEmpty())
			warning += " - WARNING: unlinked servers were excluded: [" + unlinkedServers + "]";
		if (!deadServers.isEmpty())
			warning += " - WARNING: Dead linked servers: [" + deadServers + "]";
		return "" + resultCount + " results found" + warning + "\n" + buffer.toString();
	}

	private List<String> listRemoteClients(Record server, String clientName) throws SocketTimeoutException {
		String response = sendAndReceive(server.getIpAddress(), server.getPort(), "List " + clientName + " self");
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

	private List<String> listLocalClients(String clientName) {
		List<String> clients = new ArrayList<String>();
		for (Record client : database.retrieveClients(clientName))
			clients.add(client.getName());
		return clients;
	}

	private String link(String serverName) {
		List<Record> servers = database.retrieveServers(serverName, null, null);
		if (servers.isEmpty())
			return "ERROR: unknown server name";
		Record server = servers.get(0);
		if (server.isLinked())
			return "ERROR: server was already linked - command ignored";
		String response = sendAndReceive(server.getIpAddress(), server.getPort(), "Test");
		if ("ERROR".equals(response))
			return "ERROR: unable to connect to " + server.getName() + "(" + server.getIpAddress() + ":" + server.getPort() + ")";
		else {
			server.setLinked(true);
			return serverName + " linked successfully";
		}
	}
	
	private String unlink(String serverName){
		List<Record> servers = database.retrieveServers(serverName, null, null);
		if (servers.isEmpty())
			return "ERROR: unknown server name";
		Record server = servers.get(0);
		if (!server.isLinked())
			return "ERROR: server was not linked - command ignored";
		server.setLinked(false);
		return serverName + " unlinked successfully";
	}
	
	private String sendAndReceive(String ipAddress, Integer port, String command) {
		String result = "";
		try {
			InetAddress address = InetAddress.getByName(ipAddress);
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(1000);
			//sending
			DatagramPacket packet = new DatagramPacket(command.getBytes(), command.getBytes().length, address, port);
			socket.send(packet);
			//receiving
			byte[] buf = new byte[256];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			result = new String(packet.getData(), 0, packet.getLength());
		} catch (Exception e) {
			return "ERROR";
        }
		return result;
	}
	
}
