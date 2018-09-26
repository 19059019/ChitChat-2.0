JFlags = -Xlint:none
JC = javac

.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFlags) $*.java

CLASSES = \
	chitchatapp/ClientPane.java\
	chitchatapp/clientInstance.java\
	chitchatapp/ServerPane.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) chitchatapp/*.class
