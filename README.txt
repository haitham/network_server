Server
======

To compile the server program:
	cd server
	make
	
Afterwards, run the server program:
	java Main <port-number> <server-name>
<port-number> is the port you want the server to start listening to (mandatory).
<server-name> is the name of the starting server.

======

Client
======

To compile the client program:
	cd client
	make
	
Afterwards, run the client program:
	java Main
	
Usage: You can type one of the following commands
	
	Server <IP address> <port number>
		Sets the server properties for all subsequent requests.
	Test
		Tests the connection to the server
	Insert <IP address> <port number>
		Inserts a new server
	Delete [<IP address>] [<port number>]
		Deletes an existing server
	Find <server name> <IP address> 
		Retreives the servers having the given (possibly wildcard) parameters and their count.
	Kill
		Kills the server
	Quit
		Quits the client
	Link {<IP address> <port number>} | <server name>
		Checks the given server, flags a logical link to it if it's alive.
	Unlink {<IP address> <port number>} | <server name>
		Removes the logical link with the given server
	Register <client name> <port number>
		Registers the client on the server under the given name,
		starts waiting for incoming messages
	Unregister <client name>
		Unregisters the client from the server
	List <client name list> <server name list>
		Matches the client name list given against the registered clients
		on the servers provided in the server name list, returns the result.
	Send <client name list> <server name list> <message>
		Sends the given message to the clients matching the client name list on the given servers.
		<message> should start on a new line and ends by a line containing only a period.
	Neighbors <server name list>
		Retreives the neighbors of each server in the server name list.
	Forwarding <server name list>
		Retreives the Routing table of each server in the server name list.


======