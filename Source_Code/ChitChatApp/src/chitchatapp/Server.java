package chitchatapp;

import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {

    private static ServerSocket server = null;
    private static Socket client = null;
    private static final int clientLimit = 10;
    private static final clientInstance[] clientThreads = new clientInstance[clientLimit];
    private static PrintStream output = null;
    private static Boolean status = true;
    private static ArrayList<String> userNames = new ArrayList<String>();

    public static void main(String[] args) {
        int i;
        int port = 8000;

        // open ServerSocket
        try {
            server = new ServerSocket(port);
            System.out.println("ChitChat Server Running!");
        } catch (IOException e) {
            System.err.println(e);
        }

        // create new socket for each new client that attempts to connect
        while (status) {
            try {
                client = server.accept();
                System.out.println("Client attemptig to connect.");

                for (i = 0; i < clientLimit; i++) {
                    if (clientThreads[i] == null) {
                        clientThreads[i] = new clientInstance(client, clientThreads, userNames);
                        clientThreads[i].start();
                        break;
                    }
                }

                // Message if too many clients have connected
                if (i == clientLimit) {
                    output = new PrintStream(client.getOutputStream());
                    output.println("ChitChat chatroom full, unlucky!");
                    System.out.println("Client rejected due to client limit.");
                    output.close();
                    client.close();
                }

            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
