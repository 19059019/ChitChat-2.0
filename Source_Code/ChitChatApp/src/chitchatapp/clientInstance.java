package chitchatapp;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.sql.Timestamp;
import java.util.Vector;
import javax.swing.JList;
import javax.swing.JTextArea;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@SuppressWarnings("deprecation")
class clientInstance extends Thread {

    private DataInputStream clientMessage = null;
    private PrintStream output = null;
    private Socket client = null;
    private clientInstance[] clientThreads;
    private int clientLimit;
    private ArrayList<String> userNames;
    private Vector<String> names = new Vector<String>();
    private String user;
    private boolean inGroup = false;
    private ArrayList<String> groupNames = new ArrayList<>();
    private JTextArea taClientAct;
    private String ipName;
    private JList lstOnlineUsers;
    private DatagramSocket voiceSocket;
    private final int BUFF_SIZE = 65000;
    private sendPacket packetSender = new sendPacket();
    public boolean inCall = false;

    public clientInstance(Socket client, DatagramSocket voiceSocket, clientInstance[] clientThreads, ArrayList<String> userNames,
            JTextArea taClientAct, JList lstOnlineUsers, String ipName) {
        this.client = client;
        this.clientThreads = clientThreads;
        this.userNames = userNames;
        clientLimit = clientThreads.length;
        this.taClientAct = taClientAct;
        this.ipName = ipName;
        this.lstOnlineUsers = lstOnlineUsers;
        this.voiceSocket = voiceSocket;
    }

    /**
     * Starts the thread for the client on the server. Udates user lists,
     * creates groups, voice notes, calls and ends the client etc, when
     * necessary
     */
    @Override
    public void run() {
        clientInstance[] clientThreads = this.clientThreads;
        int clientLimit = this.clientLimit;

        for (int i = 0; i < userNames.size(); i++) {
            names.add(userNames.get(i));
        }

        try {

            clientMessage = new DataInputStream(client.getInputStream());
            output = new PrintStream(client.getOutputStream());
            // Send current usernames to client for nickname picking
            String userList = listToString(userNames);
            output.println(userList);
            user = clientMessage.readLine();
            Timestamp stamp = new Timestamp(System.currentTimeMillis());

            if (user != null) {
                names.add(user);
                names = sortNames(names);

                synchronized (this) {
                    userNames.add(user);
                    groupNames.add(user);

                    taClientAct.append(user + " Joined With HostName: " + ipName + "  " + stamp + "\n");
                    lstOnlineUsers.setListData(names);
                }
            }

            output.println("Welcome to Chit Chat, it's where its at!" + "\n To leave the chatroom send \'EXIT\'");

            // Update All clients user lists for connection
            for (int i = 0; i < clientLimit; i++) {
                String message = "*userNames*##";
                String users = listToString(userNames);

                if (clientThreads[i] != null) {
                    message += user + " is now where its at!" + users;
                    clientThreads[i].output.println(message);
                }
            }

            output.println("*ipName##" + ipName);
            packetSender.send(ipName, "wololo".getBytes(), 8000);

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
                } else if (line.startsWith("*")) {
                    // Split command by the '##' delimiter
                    LinkedList<String> components = new LinkedList<>(Arrays.asList(line.split("##")));
                    String command = components.get(0);

                    // *groupremove##username: Remove user from local group
                    if (command.equals("*groupremove")) {
                        command = "*group";
                        String name = components.get(1);
                        if (groupNames.contains(name)) {
                            groupNames.remove(name);
                        }
                        continue;
                    }

                    // *call
                    if (command.equals("*call")) {
                        boolean allowed = true;
                        for (clientInstance c : clientThreads) {
                            if (c != null && !c.user.equals(this.user) && groupNames.contains(c.user)) {
                                if (c.inCall) {
                                    allowed = false;
                                } else {
                                    packetSender.send(c.ipName, "#".getBytes(), 8000);

                                    synchronized (this) {
                                        taClientAct.append("Adding " + c.user + " to call.\n");
                                    }
                                }
                            }
                        }
                        if (allowed) {
                            packetSender.send(ipName, "#".getBytes(), 8000);
                        }
                        continue;
                    }

                    // *vn (Voice Notes)
                    if (command.equals("*vn")) {
                        if (inGroup == false) {
                            continue;
                        }
                        // send '*' then vn then '*'
                        byte[] buffer = new byte[BUFF_SIZE];
                        DatagramPacket p = new DatagramPacket(buffer, BUFF_SIZE);
                        voiceSocket.receive(p);

                        if (buffer[0] == "*".getBytes()[0]) {
                            buffer = new byte[BUFF_SIZE];
                            p = new DatagramPacket(buffer, BUFF_SIZE);
                            voiceSocket.receive(p);

                            for (clientInstance c : clientThreads) {
                                if (c != null && !c.user.equals(this.user) && groupNames.contains(c.user)) {
                                    packetSender.send(c.ipName, "*".getBytes(), 8000);
                                }
                            }
                            while (buffer[0] != "*".getBytes()[0]) {
                                for (clientInstance c : clientThreads) {
                                    if (c != null && !c.user.equals(this.user) && groupNames.contains(c.user)) {
                                        packetSender.send(c.ipName, buffer, 8000);
                                    }
                                }
                                buffer = new byte[BUFF_SIZE];
                                p = new DatagramPacket(buffer, BUFF_SIZE);
                                voiceSocket.receive(p);
                            }
                            for (clientInstance c : clientThreads) {
                                if (c != null && !c.user.equals(this.user) && groupNames.contains(c.user)) {
                                    packetSender.send(c.ipName, "*".getBytes(), 8000);
                                }
                            }
                        }

                        // Send to all group members but ones self
                        continue;
                    }

                    // *startgroup##member1##member2##member3: creates a group
                    if (command.equals("*startgroup")) {
                        command = "*group";
                        groupMessage = true;
                        String alert = "Group Started: " + user;

                        for (clientInstance c : clientThreads) {
                            if (c != null // c is a valid client
                                    && components.contains(c.user) // c is required in the group
                                    && !c.user.equals(this.user) // c is not the current client
                                    && !c.inGroup) { // c is not already in a group
                                addGroupUser(c.user);
                                c.inGroup = true;
                                c.addGroupUser(user);
                                for (int i = 1; i < components.size(); i++) {
                                    if (!components.get(i).equals(this.user)) {
                                        c.addGroupUser(components.get(i));
                                    }
                                }
                                alert += ", " + c.user;
                                this.inGroup = true;
                            }
                        }

                        synchronized (this) {
                            taClientAct.append(alert + "\n");
                        }
                    }
                    // *group##message: Indicates a group message
                    if (command.equals("*group")) {

                        groupMessage = true;
                        if (groupNames.size() == 1) {
                            line = "Group Not Yet Created or empty (Some users may already be in a channel)";
                        } else if (!line.startsWith("*startgroup")) {
                            line = components.get(1);
                        }
                    }

                } else {
                    synchronized (this) {
                        taClientAct
                                .append("Incorrect Communications format from: " + user + "\nTerminating connection.\n");
                    }
                }

                // Send Message to correct client/s
                for (int i = 0; i < clientLimit; i++) {

                    // group message forwarding
                    if (groupMessage && clientThreads[i] != null) {

                        if (groupNames.contains(clientThreads[i].user)) {
                            // send to group member
                            clientThreads[i].output.println("*group##" + user + ": " + line);
                            validUser = true;
                        }
                        continue;
                    }

                    // General chat and whisper forwarding
                    if (whisper.equals("") && clientThreads[i] != null) {
                        // send to all valid users
                        clientThreads[i].output.println(user + ": " + line);
                        validUser = true;
                    } else if ((clientThreads[i] != null && clientThreads[i].user.equals(whisper))
                            || clientThreads[i] == this) {
                        // Whisper at user
                        clientThreads[i].output.println("[WHISPERED]" + user + ": " + line);
                        if (clientThreads[i] != this) {
                            validUser = true;
                        }
                    }
                }

                if (!validUser) {
                    this.output.println("You tried to whisper to an invalid user");
                }
            }

            // remove user from list of usernames
            stamp = new Timestamp(System.currentTimeMillis());
            names.remove(user);

            synchronized (this) {
                userNames.remove(user);

                taClientAct.append(user + " disconnected: " + stamp + "\n");
                lstOnlineUsers.setListData(names);
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

    /**
     * Converts lists to strings so they can be sent
     */
    private String listToString(ArrayList<String> input) {
        String out = "";
        out = input.stream().map((name) -> "##" + name).reduce(out, String::concat);
        return out;
    }

    /**
     * Creates a list of the user names connected to the server
     */
    public String getUserNames() {
        return listToString(userNames) + "##" + user;
    }

    /**
     * Adds a user to the group
     */
    public void addGroupUser(String user) {
        if (!user.equals(this.user)) {
            synchronized (this) {
                taClientAct.append(this.user + " Added " + user + "\n");
            }
            groupNames.add(user);
        }
    }

    /**
     * Sorts the list to usernames alphabetically
     */
    private Vector<String> sortNames(Vector<String> names) {
        for (int i = 0; i < names.size() - 1; i++) {
            int index = i;
            for (int j = i + 1; j < names.size(); j++) {
                if (names.get(j).compareTo(names.get(index)) < 0) {
                    index = j;
                }
            }

            String temp = names.get(index);
            names.remove(index);
            names.add(index, names.get(i));
            names.remove(i);
            names.add(i, temp);
        }

        return names;
    }
}
