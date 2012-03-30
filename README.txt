Server
======

To compile the server program:
	cd server
	make
	
Afterwards, run the server program:
	java Main <port-number> [<data-file>]
<port-number> is the port you want the server to start listening to (mandatory).
<data-file> is the path to a data file you want the server to load (optional). If not provided, the server will create one and automatically save data to it before exitting. The default data file path is current_directory/datafile.

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
	Insert <server name> <IP address> <port number>
		Inserts a new server
	Delete <server name> [<IP address>] [<port number>]
		Deletes an existing server
	Find <server name> <IP address> 
		Retreives the servers having the given (possibly wildcard) parameters and their count.
	Kill
		Kills the server
	Quit
		Quits the client
	Link <server name>
		Checks the given server, flags a logical link to it if it's alive.
	Unlink <server name>
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


======