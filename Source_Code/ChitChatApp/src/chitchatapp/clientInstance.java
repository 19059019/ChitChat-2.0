package chitchatapp;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.sql.Timestamp;

@SuppressWarnings("deprecation")
class clientInstance extends Thread {

    private DataInputStream clientMessage = null;
    private PrintStream output = null;
    private Socket client = null;
    private clientInstance[] clientThreads;
    private int clientLimit;
    private ArrayList<String> userNames;
    private String user;
    private boolean inGroup = false;
    private ArrayList<String> groupNames = new ArrayList<>();

    public clientInstance(Socket client, clientInstance[] clientThreads, ArrayList<String> userNames) {
        this.client = client;
        this.clientThreads = clientThreads;
        this.userNames = userNames;
        clientLimit = clientThreads.length;
    }

    @Override
    public void run() {
        clientInstance[] clientThreads = this.clientThreads;
        int clientLimit = this.clientLimit;

        try {
            clientMessage = new DataInputStream(client.getInputStream());
            output = new PrintStream(client.getOutputStream());

            // Send current usernames to client for nickname picking
            String userList = listToString(userNames);
            output.println(userList);
            user = clientMessage.readLine();

            synchronized (this) {
                if (user != null) {
                    userNames.add(user);
                    groupNames.add(user);
                }
            }

            output.println("Welcome to Chit Chat, it's where its at!"
                    + "\n To leave the chatroom send \'EXIT\'");

            Timestamp stamp = new Timestamp(System.currentTimeMillis());
            System.out.println(user + " Joined: " + stamp);
            
            // Update All clients user lists for connection
            for (int i = 0; i < clientLimit; i++) {
                String message = "*userNames*##";
                String users = listToString(userNames);
                
                if (clientThreads[i] != null) {
                    message += user + " is now where its at!" + users;
                    clientThreads[i].output.println(message);
                }
            }

            // Listen and handle messages from client
            while (true) {
                String line = clientMessage.readLine();
                String whisper = "";
                Boolean validUser = false;
                Boolean groupMessage = false;
                
                // Check for disconnect
                if (line.startsWith("@everyone EXIT")) {
                    break;
                }
                
                // Test for whisper
                if (line.startsWith("@")) {
                    for (int i = 1; i < line.length(); i++) {
                        if (Character.isWhitespace(line.charAt(i))) {
                            break;
                        } else {
                            whisper += line.charAt(i);
                            if (whisper.equals("everyone")) {
                                whisper = "";
                                break;
                            }
                        }
                    }

                // Test for other type of command
                } else if (line.startsWith("*")){
                    // Split command by the '##' delimiter
                    LinkedList<String> components = new LinkedList<>(Arrays.asList(line.split("##")));
                    String command = components.get(0);
                    line = components.get(components.size() - 1);
                    // *startgroup##member1##member2##member3: creates a group
                    if (command.equals("*startgroup")) {
                        command = "*group";
                        groupMessage = true;
                        String alert = "Group Started: " + user;
                        line = "You have been added to a group";

                        for (int i = 1; i < components.size(); i++) {
                            groupNames.add(components.get(i));
                            alert += ", " + components.get(i);
                        }

                        System.out.println(alert);
                    }

                    // *group##message: Indicates a group message
                    if (command.equals("*group")) {
                        groupMessage = true;
                        if (groupNames.size() == 1) {
                            line = "Group Not Yet Created";
                        }
                    }
                } else {
                    System.out.println("Incorrect Communications format from: " + user 
                                        + "\nTerminating connection.");
                }
                
                // Send Message to correct client/s
                for (int i = 0; i < clientLimit; i++) {

                    // group message forwarding
                    if (groupMessage && clientThreads[i] != null){
                        if (groupNames.contains(clientThreads[i].user)) {
                            // send to group member
                            clientThreads[i].output.println("*group##"+user + ": " + line);
                            validUser = true;
                        } else {
                            System.out.println("Wololo");
                            continue;
                        }
                    }
                    
                    // General chat and whisper forwarding
                    if (whisper.equals("") && clientThreads[i] != null) {
                        //send to all valid users
                        clientThreads[i].output.println(user + ": " + line);
                        validUser = true;
                    } else if ((clientThreads[i] != null
                            && clientThreads[i].user.equals(whisper))
                            || clientThreads[i] == this) {
                        // Whisper at user
                        clientThreads[i].output.println("[WHISPERED]" + user
                                + ": " + line);
                        if (clientThreads[i] != this) {
                            validUser = true;
                        }
                    }
                }

                if (!validUser) {
                    this.output.println("You tried to whisper at an invalid user");
                }
            }

            // remove user from list of usernames
            stamp = new Timestamp(System.currentTimeMillis());
            System.out.println(user + " Disconnected: " + stamp);

            synchronized (this) {
                userNames.remove(user);
            }
            
            // Update All clients user lists for connection
            for (int i = 0; i < clientLimit; i++) {
                String message = "*userNames*##";
                String users = listToString(userNames);
                if (clientThreads[i] != null) {
                    message += user + " Is no longer where it's at!" + users;
                    clientThreads[i].output.println(message);
                }
            }
            
            // Set this client to null
            for (int i = 0; i < clientLimit; i++) {
                if (clientThreads[i] == this) {
                    clientThreads[i] = null;
                }
            }

            clientMessage.close();
            output.close();
            client.close();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private String listToString(ArrayList<String> input) {
        String out = "";
        out = input.stream().map((name) -> "##" + name).reduce(out, String::concat);
        return out;
    }

    public String getUserNames() {
        return listToString(userNames) + "##" + user;
    }
}
