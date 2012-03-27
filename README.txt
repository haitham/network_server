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
	Insert <name> <IP address> <port number>
		Inserts a new record
	Delete <name> [<IP address>] [<port number>]
		Deletes an existing record
	Find <wild_name> <wild_IP> 
		Retreives the records having the given (possibly wildcard) parameters and their count.
	Kill
		Kills the server
	Quit
		Quits the client


======