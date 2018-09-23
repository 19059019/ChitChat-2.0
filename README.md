![alt text](chitchat.png)

# RW354 Project1 - Multi-Client/Server chat program

## Group Members:

Michael John Shepherd   - 19059019 at sun dot ac dot za

Martin William von Fintel       - 20058837 at sun dot ac dot za

## File Contents:

The **Source_code** folder contains the Netbeans project that holds all of the
java files that are used in this project. This includes Server.java,
clientInstance.java and ClientPane.java. There is also a *Makefile* in the **src**
folder that will compile the needed files and *run_server* and *run_client*
shell files to run the server and the client.

The **19059019_20058837_Project1** folder contains the package needed to run the
server and a *ChitChatApp.jar* that is the executable jar file for a chat client.
There is also a *Makefile* and a *run* shell file that will make and start the chat
Server.

## Usage:

To run the application, navigate to **19059019_20058837_Project1** and first start
the server with the following command:

```
. run.sh
```

This will compile and run the server.

The limit to the number of clients that can connect to the server is 10.

Thereafter, the client can be run. This can either be done by double clicking on the
.jar file in your file manager, or by executing the following command in **19059019_20058837_Project1**:

```
java -jar ChitChatApp.jar
```

The jar file should be able to connect to a running server from any computer, given that it has
the server's IP address.

Note that if the jar file states that it is not executable, then you will need to run the
following command in the terminal, in the **19059019_20058837_Project1** directory before
it will be allowed to run:

```
chmod +x ChitChatApp.jar
```
