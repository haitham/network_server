import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				final String command = new String(packet.getData(), 0, packet.getLength());
				final InetAddress clientAddress = packet.getAddress();
				final int clientPort = packet.getPort();
				System.out.println(">> From " + clientAddress.toString() + ":" + clientPort + " > " + command);
				
				// Spawn a thread for processing the incoming command
				new Thread(new Runnable() {
					public void run() {
						//process
						String result = processCommand(command);
						
						//write
						byte[] buf = result.getBytes();
						DatagramPacket packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
						try {
							socket.send(packet);
						} catch (IOException e) {
							System.out.println("Error writing to socket");
							e.printStackTrace();
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

	private String processCommand(String command) {
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
		} else{
			return "Unknown command";
		}
	}

	private String link(String serverName) {
		List<Record> servers = database.retrieveServers(serverName, null, null);
		if (servers.isEmpty())
			return "ERROR: unknown server name";
		Record server = servers.get(0);
		if (server.isLinked())
			return "WARNING: server was already linked - command ignored";
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
			return "WARNING: server was not linked - command ignored";
		server.setLinked(false);
		return serverName + " unlinked successfully";
	}
	
	private String sendAndReceive(String ipAddress, Integer port, String command) {
		String result = "";
		try {
			InetAddress address = InetAddress.getByName(ipAddress);
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(6000);
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
