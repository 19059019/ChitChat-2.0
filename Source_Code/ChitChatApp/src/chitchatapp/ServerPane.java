
package chitchatapp;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class ServerPane extends javax.swing.JFrame implements Runnable {

    private static ServerSocket server = null;
    private static Socket client = null;
    private static final int clientLimit = 10;
    private static final clientInstance[] clientThreads = new clientInstance[clientLimit];
    private static PrintStream output = null;
    private static Boolean status = true;
    private static ArrayList<String> userNames = new ArrayList<String>();
    private static Vector<String> names = new Vector<String>();

    /**
     * Creates new form ServerPane
     */
    public void ServerPaneInit() {
        initComponents();
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        taClientAct = new javax.swing.JTextArea();
        lblTitle = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstOnlineUsers = new javax.swing.JList<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        taClientAct.setColumns(20);
        taClientAct.setRows(5);
        jScrollPane1.setViewportView(taClientAct);

        lblTitle.setFont(new java.awt.Font("Purisa", 1, 18)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(51, 153, 0));
        lblTitle.setText("<html> <div stlye=\"text-align:center;\">CHIT CHAT<br><font size=\"4\">VoIP Server</font></div>");

        jScrollPane2.setViewportView(lstOnlineUsers);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new Thread(new ServerPane()).start();
    }

    @Override
    public void run() {
        ServerPaneInit();
        serverOps();
    }

    public void serverOps() {
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
                        names = new Vector<>(Arrays.asList(clientThreads[i].getUserNames().split("##")));
                        lstOnlineUsers.setListData(names);
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JList<String> lstOnlineUsers;
    private javax.swing.JTextArea taClientAct;
    // End of variables declaration//GEN-END:variables

}
