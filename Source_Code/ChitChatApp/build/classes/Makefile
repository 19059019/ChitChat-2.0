JFlags = -Xlint:none
JC = javac

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFlags) $*.java

CLASSES = \
	chitchatapp/ClientPane.java\
	chitchatapp/clientInstance.java\
	chitchatapp/Server.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) chitchatapp/*.class
