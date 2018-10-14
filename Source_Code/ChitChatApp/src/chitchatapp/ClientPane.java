package chitchatapp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.sound.sampled.*;
import java.io.*;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@SuppressWarnings("deprecation")
class ClientPane extends javax.swing.JFrame implements Runnable {

    /* Variables for voicenote */
    private static boolean recording = false;
    private static ByteArrayOutputStream vnByteOutput = null;
    private static ByteArrayOutputStream recievedVN = null;
    private static AudioFormat audioFormat = null;
    private static TargetDataLine targetData = null;
    private static SourceDataLine sourceData = null;
    private static AudioInputStream inputStream = null;
    private static boolean vnInbox = false;
    private static boolean vnOutbox = false;

    /* Variables for voice call */
    private static MulticastSocket groupCallMultiSocket = null;
    private static boolean onCall = false;
    private static String multiCastIP = "230.0.0.1";

    private static Socket client = null;
    private static DataInputStream serverMessage = null;
    private static DataInputStream clientMessage = null;
    private static PrintStream output = null;
    private static boolean status = true;
    private static boolean inGroup = false;
    private static String user = "Default";
    private static Vector<String> userNames = new Vector<>();
    private static Vector<String> groupUserNames = new Vector<>();
    private static String ipName = "";
    private static DatagramSocket voiceSocket = null;
    private static final int BUFF_SIZE = 65000;
    private static sendPacket packetSender = new sendPacket();
    private static String host;
    private static String hostName = "";

    public void ClientPaneInit() {
        initComponents();
        setVisible(true);
        jPanel2.setVisible(true);
        setTitle("ChitChat - " + user);
    }

    public static void main(String[] args) {
        host = "";
        int port = 8000;
        ImageIcon logo = new ImageIcon("chitchat.png");

        // Get host ip of server
        while (host.equals("")) {
            host = (String) JOptionPane.showInputDialog(null, "Please enter the host", "Host",
                    JOptionPane.QUESTION_MESSAGE, logo, null, "");
            if (host == null) {
                System.exit(0);
            }
        }

        // Create sockets
        try {
            client = new Socket(host, port);
            voiceSocket = new DatagramSocket(8000);
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "Unknown host. Come back when" + " you're sure of where you're going!");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }

        // Start streams
        try {
            serverMessage = new DataInputStream(client.getInputStream());
            clientMessage = new DataInputStream(new BufferedInputStream(System.in));
            output = new PrintStream(client.getOutputStream());
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }

        if (client != null && serverMessage != null && output != null) {
            DatagramSocket socket = null;
            
            try {
                // get username
                user = "";
                String users = serverMessage.readLine();
                userNames = new Vector<>(Arrays.asList(users.split("##")));

                while (user.equals("")) {
                    user = (String) JOptionPane.showInputDialog(null, "Please enter your nickname", "Host",
                            JOptionPane.QUESTION_MESSAGE, logo, null, "");

                    if (user == null) {
                        System.exit(0);
                    }

                    // Check for duplicate usernames
                    if (!userNames.isEmpty()) {
                        if (userNames.contains(user)) {
                            JOptionPane.showMessageDialog(null,
                                    "Nickname already in " + "use!\n Please enter a unique nickname.");
                            user = "";
                        }
                    }
                }

                // Start the communicationds threads
                new Thread(new ClientPane()).start();
                output.println(user);
                
                // Wait for input
                while (status) {
                    String message = clientMessage.readLine().trim();

                    if (message.startsWith("EXIT")) {
                        break;
                    } else {
                        output.println("@everyone " + message);
                    }
                }

                // Close streams and sockets
                System.out.println("Cheerio!");
                output.close();
                clientMessage.close();
                serverMessage.close();
                client.close();
                System.exit(0);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    @Override
    public void run() {
        ClientPaneInit();
        // Sample size 8000, bit size 16
        audioFormat = customAudioFormat(44100, 16);
        // Thread for listening for udp messages
        Thread udpListener = new Thread(new Runnable() {
            public void run() {
                voiceListener();
            }
        });
        udpListener.start();
        // thread for listening for tcp messages
        messageListener();
    }

    /**
     * This method listens for tcp messages
     *
     **/
    @SuppressWarnings("deprecation")
    public void messageListener() {
        String message;

        try {
            while ((message = serverMessage.readLine()) != null) {
                if (message.startsWith("*userNames*")) {
                    // Update user list
                    userNames = new Vector<>(Arrays.asList(message.split("##")));
                    if (!inGroup) {
                        groupLabel.setText("Online Users:");
                        lstGroupUsers.setListData(userNames);
                    } else {
                        // Check if group member left
                        for (int i = 0; i < groupUserNames.size(); i++) {
                            String s = groupUserNames.get(i);
                            if (!userNames.contains(s)) {
                                i--;
                                taGroupText.append("\n" + s + "Has Left The group");
                                // Remove Group member on server side
                                output.println("*groupremove##" + s);
                                groupUserNames.remove(s);
                                lstGroupUsers.setListData(groupUserNames);
                            }
                        }
                        if (groupUserNames.size() == 1) {
                            groupLabel.setText("Online Users:");
                            btnCreateGroup.setEnabled(true);
                            inGroup = false;
                            lstGroupUsers.setListData(userNames);
                        }
                    }

                    message = userNames.get(1);
                    userNames.remove(1);
                    userNames.remove(0);

                    userNames = sortNames(userNames);
                    lstOnlineUsers.setListData(userNames);

                    if (user.equals("Default")) {
                        user = userNames.get(userNames.size() - 1);
                        setTitle("ChitChat - " + user);
                    }
                } else if (message.startsWith("*group")) {
                    // Recieve group message
                    inGroup = true;
                    ArrayList<String> components = new ArrayList<>(Arrays.asList(message.split("##")));
                    if (components.get(1).contains(": *startgroup")) {
                        groupLabel.setText("Users in your group:");
                        // Set user names of group
                        taGroupText.append("Starting Group\n");
                        message = "Added to a group";
                        components.remove(0);
                        components.remove(0);
                        groupUserNames = new Vector<>(components);
                        lstGroupUsers.setListData(groupUserNames);
                    } else {
                        message = components.get(1);
                    }
                    if (message.contains("Group Not Yet Created")) {
                        btnCreateGroup.setEnabled(true);
                    }
                    taGroupText.append(message + "\n");
                    continue;
                } else if (message.startsWith("*ipName")) {
                    String[] components = message.split("##");
                    ipName = components[1];
                    continue;
                }

                if (groupUserNames.size() == 1) {
                    groupLabel.setText("Online Users:");
                    btnCreateGroup.setEnabled(true);
                }

                taChatArea.append(message + "\n");
            }

            status = false;
        } catch (IOException e) {
            System.out.println("Disconnected!");
        }
    }

    /**
     * This method listens for UDP messages
     *
     **/
    public void voiceListener() {
        boolean isListening = true;
        try {
            byte[] buffer = new byte[BUFF_SIZE];
            DatagramPacket p = new DatagramPacket(buffer, BUFF_SIZE);
            voiceSocket.receive(p);
            hostName = p.getAddress().getHostName();
            while (isListening) {
                // Recieve packets (* denotes vn)
                buffer = new byte[BUFF_SIZE];
                p = new DatagramPacket(buffer, BUFF_SIZE);
                voiceSocket.receive(p);

                // Recieve Voicenote
                if (buffer[0] == "*".getBytes()[0]) {
                    taGroupText.append("Someone sent you a voiceNote\n");
                    vnInbox = true;
                    recievedVN = new ByteArrayOutputStream();

                    while (true) {
                        buffer = new byte[BUFF_SIZE];
                        p = new DatagramPacket(buffer, BUFF_SIZE);
                        voiceSocket.receive(p);
                        if (buffer[0] == "*".getBytes()[0]) {
                            recievedVN.close();
                            vnPlayRecieved.setEnabled(true);
                            break;
                        } else {
                            recievedVN.write(buffer, 0, buffer.length);
                        }
                    }
                }
                if (buffer[0] == "#".getBytes()[0]) {
                    onCall = !onCall;
                    if (onCall) {
                        makeCall();
                        Thread call = new Thread(new Runnable() {
                            public void run() {
                                callListener();
                            }
                        });
                        call.start();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * This method listens for packets during a call
     *
     **/
    public void callListener() {
        try {
            btnCall.setText("END CALL");
            taGroupText.append("Initiating a call\n");
            groupCallMultiSocket = new MulticastSocket(4321);
            InetAddress group = InetAddress.getByName(multiCastIP);
            groupCallMultiSocket.joinGroup(group);

            while (onCall) {
                byte[] callBuffer = new byte[BUFF_SIZE];
                DatagramPacket callPacket = new DatagramPacket(callBuffer, BUFF_SIZE);
                groupCallMultiSocket.receive(callPacket);
                if (callBuffer[0] == "#".getBytes()[0]) {
                    onCall = false;
                    taGroupText.append("Ending call\n");
                    btnCall.setText("MAKE CALL");
                    break;
                }
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                b.write(callBuffer);
                b.close();
                playVn(b);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taChatArea = new javax.swing.JTextArea();
        tfMessageInput = new javax.swing.JTextField();
        lblTitle = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstOnlineUsers = new javax.swing.JList<>();
        btnWhisper = new javax.swing.JButton();
        btnGroup = new javax.swing.JButton();
        btnSend = new javax.swing.JButton();
        btnGroupCall = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        taGroupText = new javax.swing.JTextArea();
        btnGroupSend = new javax.swing.JButton();
        btnCreateGroup = new javax.swing.JButton();
        btnCall = new javax.swing.JButton();
        groupLabel = new javax.swing.JLabel();
        tfGroupMessage = new javax.swing.JTextField();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstGroupUsers = new javax.swing.JList<>();
        lblTitle1 = new javax.swing.JLabel();
        vnRecord = new javax.swing.JButton();
        vnPlayLocal = new javax.swing.JButton();
        vnSend = new javax.swing.JButton();
        vnPlayRecieved = new javax.swing.JButton();
        vnStopRecording = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        taChatArea.setEditable(false);
        taChatArea.setColumns(20);
        taChatArea.setLineWrap(true);
        taChatArea.setRows(5);
        jScrollPane1.setViewportView(taChatArea);

        tfMessageInput.setText("Type message or command here...");
        tfMessageInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfMessageInputFocusGained(evt);
            }
        });

        lblTitle.setFont(new java.awt.Font("Purisa", 1, 24)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(51, 153, 0));
        lblTitle.setText(
                "<html> <div stlye=\"text-align:center;\">CHIT CHAT<sup><font size=\"4\"> VoIP</font></sup><br><font size=\"4\">It's Where It's At!</font></div>");

        jLabel1.setText("Users:");

        lstOnlineUsers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstOnlineUsers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstOnlineUsersValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(lstOnlineUsers);

        btnWhisper.setText("Whisper");
        btnWhisper.setEnabled(false);
        btnWhisper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWhisperActionPerformed(evt);
            }
        });

        btnGroup.setText("Return to Group Chat");
        btnGroup.setEnabled(false);
        btnGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGroupActionPerformed(evt);
            }
        });

        btnSend.setText("Send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        btnGroupCall.setText("Group Call");
        btnGroupCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGroupCallActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup().addComponent(btnSend).addGap(0, 0,
                                Short.MAX_VALUE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 268,
                                                Short.MAX_VALUE)
                                        .addComponent(tfMessageInput))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 261,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel1Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                .addComponent(btnWhisper, javax.swing.GroupLayout.Alignment.LEADING,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnGroup, javax.swing.GroupLayout.Alignment.LEADING,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE))
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 271,
                                                javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(77, Short.MAX_VALUE)))));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addComponent(jScrollPane1,
                                javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
                                jPanel1Layout.createSequentialGroup().addComponent(lblTitle)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 185,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnWhisper)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnGroup)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tfMessageInput, javax.swing.GroupLayout.PREFERRED_SIZE, 39,
                                javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel1Layout
                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(btnSend))
                        .addContainerGap(26, Short.MAX_VALUE)));

        jScrollPane1.getAccessibleContext().setAccessibleName("");

        jTabbedPane1.addTab("Global Chat", jPanel1);

        taGroupText.setColumns(20);
        taGroupText.setRows(5);
        taGroupText.setEditable(false);
        taGroupText.setLineWrap(true);
        jScrollPane4.setViewportView(taGroupText);

        btnGroupSend.setText("Send");
        btnGroupSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGroupSendActionPerformed(evt);
            }
        });

        btnCreateGroup.setText("Create Group");
        btnCreateGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateGroupActionPerformed(evt);
            }
        });

        btnCall.setText("Start Call");
        btnCall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCallActionPerformed(evt);
            }
        });

        groupLabel.setText("Online Users:");

        tfGroupMessage.setText("Type message or command here...");
        tfGroupMessage.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                tfGroupMessageFocusGained(evt);
            }
        });
        tfGroupMessage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfGroupMessageActionPerformed(evt);
            }
        });

        lstGroupUsers.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstGroupUsersValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(lstGroupUsers);

        lblTitle1.setFont(new java.awt.Font("Purisa", 1, 24)); // NOI18N
        lblTitle1.setForeground(new java.awt.Color(51, 153, 0));
        lblTitle1.setText(
                "<html> <div stlye=\"text-align:center;\">CHIT CHAT<sup><font size=\"4\"> VoIP</font></sup><br><font size=\"4\">It's Where It's At!</font></div>");

        vnRecord.setText("Record Voice Note");
        vnRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vnRecordActionPerformed(evt);
            }
        });

        vnPlayLocal.setText("Play Voice Note");
        vnPlayLocal.setEnabled(false);
        vnPlayLocal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vnPlayLocalActionPerformed(evt);
            }
        });

        vnSend.setText("Send Voice Note");
        vnSend.setEnabled(false);
        vnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vnSendActionPerformed(evt);
            }
        });

        vnPlayRecieved.setText("Voice Note Inbox");
        vnPlayRecieved.setEnabled(false);
        vnPlayRecieved.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vnPlayRecievedActionPerformed(evt);
            }
        });

        vnStopRecording.setText("Stop Recording");
        vnStopRecording.setEnabled(false);
        vnStopRecording.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vnStopRecordingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup().addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup().addComponent(btnGroupSend)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnCall).addGap(104, 104, 104)
                                        .addComponent(vnSend, javax.swing.GroupLayout.PREFERRED_SIZE, 169,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(
                                                vnPlayRecieved, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                .addGroup(jPanel2Layout.createSequentialGroup().addGroup(jPanel2Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 266,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblTitle1).addComponent(groupLabel)
                                                        .addComponent(jScrollPane5,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                javax.swing.GroupLayout.DEFAULT_SIZE)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addComponent(btnCreateGroup,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 169,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(vnStopRecording,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                                                        javax.swing.GroupLayout.PREFERRED_SIZE))))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(tfGroupMessage, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                        266, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(vnRecord, javax.swing.GroupLayout.PREFERRED_SIZE, 169,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
                                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(vnPlayLocal, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                                        javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(25, 25, 25)))));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(jPanel2Layout
                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup().addComponent(lblTitle1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(groupLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 271,
                                        javax.swing.GroupLayout.DEFAULT_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnCreateGroup).addComponent(vnStopRecording)))
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 388,
                                javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(tfGroupMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 32,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(vnRecord).addComponent(vnPlayLocal))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup().addGap(12, 12, 12)
                                        .addGroup(jPanel2Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnGroupSend).addComponent(btnCall)))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout
                                                .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(vnSend).addComponent(vnPlayRecieved))))
                        .addContainerGap(77, Short.MAX_VALUE)));

        jTabbedPane1.addTab("VoIP", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                javax.swing.GroupLayout.Alignment.TRAILING,
                layout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE).addComponent(jTabbedPane1,
                        javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGroupCallActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGroupCallActionPerformed

    }// GEN-LAST:event_btnGroupCallActionPerformed

    private void btnGroupSendActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnGroupSendActionPerformed
        if (!tfGroupMessage.getText().equals("")
                && !tfGroupMessage.getText().equals("Type message or command here...")) {
            String msg = tfGroupMessage.getText();

            if (msg.startsWith("EXIT")) {
                output.println("@everyone " + msg);

                tfMessageInput.setText("Cheerio!");

                try {
                    output.close();
                    clientMessage.close();
                    serverMessage.close();
                    client.close();
                    System.exit(0);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            output.println("*group##" + msg);
            tfGroupMessage.setText("Type message or command here...");
        }
    }// GEN-LAST:event_btnGroupSendActionPerformed

    private void btnCreateGroupActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCreateGroupActionPerformed
        // TODO Create Group
        if (lstGroupUsers.getSelectedValues().length == 0) {
            taGroupText.append("\nNo Users Selected");
        } else {
            Object[] targetArray = lstGroupUsers.getSelectedValues();
            ArrayList<Object> targets = new ArrayList<>(Arrays.asList(targetArray));

            for (int i = 0; i < targets.size(); i++) {
                Object o = targets.get(i);
                if (((String) o).equals(user)) {
                    i--;
                    targets.remove(o);
                    break;
                }
            }
            if (targets.size() == 0) {
                taGroupText.append("\nCannot create a group with only yourself");
            } else {
                btnCreateGroup.setEnabled(false);
                String addToGroup = "*startgroup";
                addToGroup += "##" + user;
                for (Object o : targets) {
                    addToGroup += "##" + (String) o;
                }
                taGroupText.append("Attempting to start a group\n");
                output.println(addToGroup);
            }

        }

        taChatArea.append("Attempting to start a group\n");
    }// GEN-LAST:event_btnCreateGroupActionPerformed

    private void btnCallActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCallActionPerformed
        if (!onCall) {
            makeCall();
            btnCall.setText("END CALL");
        } else {
            btnCall.setText("MAKE CALL");
            try {
                InetAddress group = InetAddress.getByName(multiCastIP);
                packetSender.sendMulti("#".getBytes(), group, 4321);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }// GEN-LAST:event_btnCallActionPerformed

    private void tfGroupMessageActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_tfGroupMessageActionPerformed

    }// GEN-LAST:event_tfGroupMessageActionPerformed

    private void lstGroupUsersValueChanged(javax.swing.event.ListSelectionEvent evt) {// GEN-FIRST:event_lstGroupUsersValueChanged

    }// GEN-LAST:event_lstGroupUsersValueChanged

    // Send Voice Note
    private void vnSendActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_vnSendActionPerformed
        output.println("*vn");
        packetSender.sendVN(hostName, vnByteOutput.toByteArray(), 8001);
        vnSend.setEnabled(false);
        // getIP/hostname of destination
        // Connect with host
        // Send voice note
        // close connection

    }// GEN-LAST:event_vnSendActionPerformed

    // Play recorded voice note
    private void vnPlayLocalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_vnPlayLocalActionPerformed
        playVn(vnByteOutput);
    }// GEN-LAST:event_vnPlayLocalActionPerformed

    // Play recieved voice note
    private void vnPlayRecievedActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_vnPlayRecievedActionPerformed
        playVn(recievedVN);
        vnInbox = false;
    }// GEN-LAST:event_vnPlayRecievedActionPerformed

    // Record Voice Note
    private void vnRecordActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_vnRecordActionPerformed
        vnStopRecording.setEnabled(true);
        recording = true;
        recordVn();
    }// GEN-LAST:event_vnRecordActionPerformed

    // Stop Recording voice note
    private void vnStopRecordingActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_vnStopRecordingActionPerformed
        vnPlayLocal.setEnabled(true);
        recording = false;
        vnStopRecording.setEnabled(false);
        vnOutbox = true;
        vnSend.setEnabled(true);
    }// GEN-LAST:event_vnStopRecordingActionPerformed

    private void tfGroupMessageFocusGained(java.awt.event.FocusEvent evt) {// GEN-FIRST:event_tfGroupMessageFocusGained
        if (tfGroupMessage.getText().equals("Type message or command here...")) {
            tfGroupMessage.setText("");
        }
    }// GEN-LAST:event_tfGroupMessageFocusGained

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {
        if (!tfMessageInput.getText().equals("")
                && !tfMessageInput.getText().equals("Type message or command here...")) {
            String msg = tfMessageInput.getText();

            output.println("@everyone " + msg);

            if (msg.startsWith("EXIT")) {
                taChatArea.append("Cheerio!\n");

                try {
                    output.close();
                    clientMessage.close();
                    serverMessage.close();
                    client.close();
                    System.exit(0);
                } catch (IOException e) {
                    System.err.println(e);
                }
            }

            tfMessageInput.setText("Type message or command here...");
        }
    }

    private void btnWhisperActionPerformed(java.awt.event.ActionEvent evt) {
        String target = lstOnlineUsers.getSelectedValue();

        if (!tfMessageInput.getText().equals("") && !tfMessageInput.getText().equals("Type message here...")) {
            String msg = tfMessageInput.getText();

            output.println("@" + target + " " + msg);

            tfMessageInput.setText("Type message here...");
            btnGroup.setEnabled(true);
        }
    }

    private void lstOnlineUsersValueChanged(javax.swing.event.ListSelectionEvent evt) {
        btnWhisper.setEnabled(true);
    }

    private void btnGroupActionPerformed(java.awt.event.ActionEvent evt) {
        btnGroup.setEnabled(false);
        btnWhisper.setEnabled(false);
        lstOnlineUsers.clearSelection();
    }

    private void tfMessageInputFocusGained(java.awt.event.FocusEvent evt) {
        if (tfMessageInput.getText().equals("Type message or command here...")) {
            tfMessageInput.setText("");
        }
    }

    // sort user names by hostname
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

    /**
     * This method records the voice note locally in a new thread
     */
    private void recordVn() {
        // Sample size 8000, bit size 16
        try {
            DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetData = (TargetDataLine) AudioSystem.getLine(dataInfo);
            targetData.open(audioFormat);
            targetData.start();
            Thread recordVN = new Thread(new recordThread());
            recordVN.start();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * This call handles the starting and ending of calls and the transmitting of
     * local voice to the targets in a seperate thread.
     */
    private void makeCall() {
        // Sample size 8000, bit size 16
        try {
            if (!onCall) {
                // Ask for permission with tcp
                output.println("*call");
                // wait for response
                Thread.sleep(1000);
            }
            // Get/Set group ip with UDP
            if (onCall) {
                DataLine.Info dataInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                targetData = (TargetDataLine) AudioSystem.getLine(dataInfo);
                targetData.open(audioFormat);
                targetData.start();
                Thread call = new Thread(new callThread());
                call.start();
            } else {
                taGroupText.append("Call not possible\n");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * This function plays toPlay in a seperate thread
     * 
     * @param toPlay - data to be played
     */
    private void playVn(ByteArrayOutputStream toPlay) {
        try {
            byte[] inputBuffer = toPlay.toByteArray();
            InputStream byteInputStream = new ByteArrayInputStream(inputBuffer);
            inputStream = new AudioInputStream(byteInputStream, audioFormat,
                    inputBuffer.length / audioFormat.getFrameSize());
            DataLine.Info dataInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceData = (SourceDataLine) AudioSystem.getLine(dataInfo);
            sourceData.open(audioFormat);
            sourceData.start();
            Thread playVN = new Thread(new playThread());
            playVN.start();
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }

    /**
     * This function allows for customisation of the AudioFormat
     * 
     * @param sampleRate
     * @param sampleSize
     * @return audioFormat
     */
    private AudioFormat customAudioFormat(float sampleRate, int sampleSize) {
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;

        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }

    // Inner class to capture audio
    class recordThread extends Thread {
        byte buffer[] = new byte[10000];

        /**
         * Thread to record a voice note
         */
        public void run() {
            recording = true;
            vnByteOutput = new ByteArrayOutputStream();
            try {
                while (recording) {
                    int count = targetData.read(buffer, 0, buffer.length);
                    // Write Data from buffer up to the size of the captured data
                    if (count > 0) {
                        vnByteOutput.write(buffer, 0, count);
                    }
                }
                vnByteOutput.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    // Inner class to capture and send sound for a call
    class callThread extends Thread {
        byte buffer[] = new byte[10000];

        /**
         * Thread to capture and send voice
         */
        public void run() {
            onCall = true;
            try {
                InetAddress group = InetAddress.getByName(multiCastIP);
                while (onCall) {
                    int count = targetData.read(buffer, 0, buffer.length);
                    // Write Data from buffer up to the size of the captured data
                    if (count > 0) {
                        packetSender.sendMulti(buffer, group, 4321);
                    }
                }
                // Notify others that call is done
                packetSender.sendMulti("#".getBytes(), group, 4321);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    // inner class to playback audio
    class playThread extends Thread {
        byte[] buffer = new byte[10000];

        /**
         * Thread to play audio
         */
        public void run() {
            try {
                synchronized (this) {
                    int count;
                    // While the input does not return -1
                    while ((count = inputStream.read(buffer, 0, buffer.length)) != -1) {
                        // Write Data from buffer up to the size of the captured data
                        if (count > 0) {
                            sourceData.write(buffer, 0, count);
                        }
                    }
                    sourceData.drain();
                    sourceData.close();
                    inputStream.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCall;
    private javax.swing.JButton btnCreateGroup;
    private javax.swing.JButton btnGroup;
    private javax.swing.JButton btnGroupCall;
    private javax.swing.JButton btnGroupSend;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btnWhisper;
    private javax.swing.JLabel groupLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTitle1;
    private javax.swing.JList<String> lstGroupUsers;
    private javax.swing.JList<String> lstOnlineUsers;
    private javax.swing.JTextArea taChatArea;
    private javax.swing.JTextArea taGroupText;
    private javax.swing.JTextField tfGroupMessage;
    private javax.swing.JTextField tfMessageInput;
    private javax.swing.JButton vnPlayLocal;
    private javax.swing.JButton vnPlayRecieved;
    private javax.swing.JButton vnRecord;
    private javax.swing.JButton vnSend;
    private javax.swing.JButton vnStopRecording;
    // End of variables declaration//GEN-END:variables

}
