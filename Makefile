JFLAGS = -g
JC = javac
CLASSPATH = -classpath src/
BIN = -d out/
.SUFFIXES: .java .class
.java.class:
	$(JC) $(BIN) $(JFLAGS) $(CLASSPATH) $*.java
 
CLIENTCLASSES = \
    src/ui/TestApp.java \

SERVERCLASSES = \
	src/communication/Message.java \
    src/communication/Receiver.java \
	src/model/BackedFile.java \
	src/model/Chunk.java \
	src/protocol/Backup.java \
	src/protocol/Control.java \
	src/protocol/Restore.java \
	src/ui/Peer.java \
	src/utils/Database.java \
	src/utils/Support.java \
 
default: classes
 
classes: $(CLIENTCLASSES:.java=.class)\
$(SERVERCLASSES:.java=.class)

client : $(CLIENTCLASSES:.java=.class)

server : $(SERVERCLASSES:.java=.class)
 
clean:
	$(RM) *.class