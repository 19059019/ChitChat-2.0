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

@SuppressWarnings("deprecation")
class ClientPane extends javax.swing.JFrame implements Runnable {

    private static Socket client = null;
    private static DataInputStream serverMessage = null;
    private static DataInputStream clientMessage = null;
    private static PrintStream output = null;
    private static boolean status = true;
    private static boolean inGroup = false;
    private  static String user = "Default";
    private static Vector<String> userNames = new Vector<>();
    private static Vector<String> groupUserNames = new Vector<>();

    public void ClientPaneInit() {
        initComponents();
        setVisible(true);
        jPanel2.setVisible(true);
        setTitle("ChitChat - " + user);
    }

    public static void main(String[] args) {
        String host = "";
        int port = 8000;
        ImageIcon logo = new ImageIcon("chitchat.png");

        while (host.equals("")) {
            host = (String) JOptionPane.showInputDialog(null, "Please enter the host",
                    "Host", JOptionPane.QUESTION_MESSAGE, logo, null, "");

            if (host == null) {
                System.exit(0);
            }
        }

        try {
            client = new Socket(host, port);
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null, "Unknown host. Come back when"
                    + " you're sure of where you're going!");
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }

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
                user = "";
                String users = serverMessage.readLine();
                userNames = new Vector<>(Arrays.asList(users.split("##")));

                while (user.equals("")) {
                    user = (String) JOptionPane.showInputDialog(null, "Please enter your nickname",
                            "Host", JOptionPane.QUESTION_MESSAGE, logo, null, "");

                    if (user == null) {
                        System.exit(0);
                    }

                    //Check for duplicate usernames
                    if (!userNames.isEmpty()) {
                        if (userNames.contains(user)) {
                            JOptionPane.showMessageDialog(null,
                                    "Nickname already in "
                                    + "use!\n Please enter a unique nickname.");
                            user = "";
                        }
                    }
                }

                new Thread(new ClientPane()).start();

                output.println(user);
                
                //this should get replaced by sending client behavior to server
                socket = new DatagramSocket();
                String tosend = users + "##" + user;
                byte[] payload = tosend.getBytes();
                DatagramPacket p = new DatagramPacket(payload, payload.length, InetAddress.getByName(host), 8000);
               
                socket.send(p);
                
                while (status) {
                    String message = clientMessage.readLine().trim();

                    if (message.startsWith("EXIT")) {
                        break;
                    } else {
                        output.println("@everyone " + message);
                    }
                }

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
        messageListener();
    }

    @SuppressWarnings("deprecation")
    public void messageListener() {
        String message;

        try {
            while ((message = serverMessage.readLine()) != null) {
                if (message.startsWith("*userNames*")) {
                    // Update user list
                    userNames = new Vector<>(Arrays.asList(message.split("##")));
                    if (!inGroup) {
                        lstGroupUsers.setListData(userNames);
                    } else {
//                        // Check if group member left
                        for (String s : groupUserNames) {
                            if (!userNames.contains(s)) {
                                taGroupText.append("\n" + s + " Has Left The group");
                                // Remove Group member on server side
                                output.println("*groupremove##" + s);
                                groupUserNames.remove(s);
                            }
                        }
                    }

                    message = userNames.get(1);
                    userNames.remove(1);
                    userNames.remove(0);
                    lstOnlineUsers.setListData(userNames);

                    if (user.equals("Default")) {
                        user = userNames.get(userNames.size() - 1);
                        setTitle("ChitChat - " + user);
                    }
                } else if (message.startsWith("*group")) {
                    // Recieve group message
                    System.out.println("Recieving group message");
                    System.out.println(message);

                    inGroup = true;
                    ArrayList<String> components = new ArrayList<>(Arrays.asList(message.split("##")));
                    if (components.get(1).contains(": *startgroup")) {
                        // Set user names of group
                        System.out.println("Starting Group");
                        message = "Added to a group";
                        components.remove(0);
                        components.remove(0);
                        for (String s : components) {
                            System.out.println(s);
                        }
                        groupUserNames = new Vector<>(components);
                        lstGroupUsers.setListData(groupUserNames);
                    } else {
                        message = components.get(1);
                    }
                    taGroupText.append("\n" + message);
                    continue;
                }

                System.out.println(message);
                taChatArea.append("\n" + message);
                //lblNumUsers.setText(userNames.size() + "");
            }

            status = false;
        } catch (IOException e) {
            System.out.println("Disconnected!");
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        jLabel2 = new javax.swing.JLabel();
        tfGroupMessage = new javax.swing.JTextField();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstGroupUsers = new javax.swing.JList<>();
        lblTitle1 = new javax.swing.JLabel();

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
        lblTitle.setText("<html> <div stlye=\"text-align:center;\">CHIT CHAT<sup><font size=\"4\"> VoIP</font></sup><br><font size=\"4\">It's Where It's At!</font></div>");

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
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnSend)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnGroupCall)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                            .addComponent(tfMessageInput))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 261, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(btnWhisper, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnGroup, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE))
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 271, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(55, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 367, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(lblTitle)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnWhisper)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnGroup)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfMessageInput, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSend)
                    .addComponent(btnGroupCall))
                .addContainerGap(26, Short.MAX_VALUE))
        );

        jScrollPane1.getAccessibleContext().setAccessibleName("");

        jTabbedPane1.addTab("Global Chat", jPanel1);

        taGroupText.setColumns(20);
        taGroupText.setRows(5);
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

        jLabel2.setText("Users:");

        tfGroupMessage.setText("Type message or command here...");
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
        lblTitle1.setText("<html> <div stlye=\"text-align:center;\">CHIT CHAT<sup><font size=\"4\"> VoIP</font></sup><br><font size=\"4\">It's Where It's At!</font></div>");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(btnCreateGroup, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblTitle1)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnGroupSend)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCall))
                    .addComponent(tfGroupMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(67, 67, 67))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(lblTitle1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCreateGroup))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tfGroupMessage, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGroupSend)
                    .addComponent(btnCall))
                .addContainerGap())
        );

        jTabbedPane1.addTab("VoIP", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGroupCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGroupCallActionPerformed

    }//GEN-LAST:event_btnGroupCallActionPerformed

    private void btnGroupSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGroupSendActionPerformed
        if (!tfGroupMessage.getText().equals("") && !tfGroupMessage.getText().equals("Type message or command here...")) {
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
            System.out.println("*group##" + msg);
            tfGroupMessage.setText("Type message or command here...");
        }
    }//GEN-LAST:event_btnGroupSendActionPerformed

    private void btnCreateGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateGroupActionPerformed
        // TODO Create Group
        btnCreateGroup.setEnabled(false);
        Object[] targets = lstGroupUsers.getSelectedValues();
        String addToGroup = "*startgroup";
        addToGroup += "##" + user;
        for (Object o : targets) {
            addToGroup += "##" + (String) o;
        }
        System.out.println("Attempting to start a group");
        System.out.println(addToGroup);
        output.println(addToGroup);
    }//GEN-LAST:event_btnCreateGroupActionPerformed

    private void btnCallActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCallActionPerformed

    }//GEN-LAST:event_btnCallActionPerformed

    private void tfGroupMessageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfGroupMessageActionPerformed

    }//GEN-LAST:event_tfGroupMessageActionPerformed

    private void lstGroupUsersValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstGroupUsersValueChanged

    }//GEN-LAST:event_lstGroupUsersValueChanged

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {
        if (!tfMessageInput.getText().equals("") && !tfMessageInput.getText().equals("Type message or command here...")) {
            String msg = tfMessageInput.getText();

            output.println("@everyone " + msg);

            if (msg.startsWith("EXIT")) {
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCall;
    private javax.swing.JButton btnCreateGroup;
    private javax.swing.JButton btnGroup;
    private javax.swing.JButton btnGroupCall;
    private javax.swing.JButton btnGroupSend;
    private javax.swing.JButton btnSend;
    private javax.swing.JButton btnWhisper;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
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
    // End of variables declaration//GEN-END:variables
}
