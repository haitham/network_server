JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Main.java \
	Server.java \
	Database.java \
	IPAddress.java \
	Record.java \
	RoutingEntry.java \
	Neighbor.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class