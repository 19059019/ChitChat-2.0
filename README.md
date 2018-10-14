![alt text](chitchat.png)

# RW354 Project4 - VoIP Chat program

## Group Members:

Michael John Shepherd   - 19059019 at sun dot ac dot za

Martin William von Fintel       - 20058837 at sun dot ac dot za

## File Contents:

The **Source_code** folder contains the Netbeans project that holds all of the
java files that are used in this project. This includes ServerPane.java,
clientInstance.java, ClientPane.java and sendPacket.java. There is also a *Makefile* in the **src**
folder that will compile the needed files and *run_server* and *run_client*
shell files to run the server and the client.

## Usage:

To run the application, navigate to **Source_Code/ChitChatApp/src** and first start
the server with the following command:

```
. run_server.sh
```

This will compile and run the server.

The limit to the number of clients that can connect to the server is 10.

Thereafter, the client can be run. with the command:

```
. run_client.sh
```

The client will be able to connect to a running server from any computer, given that it has
the server's IP address. Note that only one client will be allowed to connect from each computer.