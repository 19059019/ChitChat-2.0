package chitchatapp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

class sendPacket {

    private DatagramSocket socket = null;
    private final int BUFF_SIZE = 65000;

    public sendPacket() {
        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Sends the buffer to the host over the port
     *
     * @param host the host to send to
     * @param buffer the byte buffer to send
     * @param port the port for it to be sent over
     */
    public void send(String host, byte[] buffer, int port) {
        try {
            InetAddress inet = InetAddress.getByName(host);
            // Send multiple packets < 65000
            if (buffer.length > BUFF_SIZE) {
                System.out.println("Forwarding Multiple Packets");
                double packetCount = Math.ceil(buffer.length / (BUFF_SIZE * 1.0));
                for (int i = 0; i < packetCount; i++) {
                    byte[] temp = new byte[BUFF_SIZE];
                    if (packetCount - i == 1) {
                        temp = Arrays.copyOfRange(buffer, i * BUFF_SIZE, buffer.length - 1);
                    } else {
                        temp = Arrays.copyOfRange(buffer, i * BUFF_SIZE, (i + 1) * BUFF_SIZE - 1);
                    }
                    this.send(host, temp, port);
                }
            } else {
                System.out.println("Forwarding Single Packet");
                DatagramPacket p = new DatagramPacket(buffer, buffer.length, inet, port);
                socket.send(p);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Sends a voice note
     *
     * @param host the host to send to
     * @param vn the byte buffer containing the voicenote data
     * @param port the port for it to be sent over
     */
    public void sendVN(String host, byte[] vn, int port) {
        this.send(host, "*".getBytes(), port);
        this.send(host, vn, port);
        this.send(host, "*".getBytes(), port);
    }

    /**
     * Sends the buffer to the multiple clients in the group
     *
     * @param buffer the byte buffer to send
     * @param group the inet address for the group
     * @param port the port for it to be sent over
     */
    public void sendMulti(byte[] buffer, InetAddress group, int port) {
        try {
            DatagramPacket p = new DatagramPacket(buffer, buffer.length, group, 4321);
            this.socket.send(p);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
