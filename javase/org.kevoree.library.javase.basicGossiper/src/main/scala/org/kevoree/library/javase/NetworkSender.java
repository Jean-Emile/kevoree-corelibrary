package org.kevoree.library.javase;

import jexxus.client.UniClientConnection;
import jexxus.common.Connection;
import jexxus.common.ConnectionListener;
import jexxus.common.Delivery;
import jexxus.server.ServerConnection;
import org.kevoree.library.basicGossiper.protocol.message.KevoreeMessage;
import org.kevoree.library.javase.basicGossiper.GossiperProcess;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 12/11/12
 * Time: 15:46
 */
public class NetworkSender implements INetworkSender {

    private GossiperProcess process = null;

    public NetworkSender(GossiperProcess p) {
        process = p;
    }

    @Override
    public void sendMessageUnreliable(KevoreeMessage.Message m, InetSocketAddress addr) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(2);
            m.writeTo(output);
            byte[] message = output.toByteArray();
            output.close();
            DatagramPacket packet = new DatagramPacket(message, message.length, addr);
            DatagramSocket dsocket = new DatagramSocket();
            dsocket.send(packet);
            dsocket.close();
        } catch (Exception e) {
            org.kevoree.log.Log.debug("", e);
        }
    }

    @Override
    public boolean sendMessage(KevoreeMessage.Message m, InetSocketAddress addr) {
        UniClientConnection conn = null;
        try {
            conn = new UniClientConnection(new ConnectionListener() {

                @Override
                public void connectionBroken(Connection broken, boolean forced) {

                }

                @Override
                public void receive(byte[] data, Connection from) {
                }

                @Override
                public void clientConnected(ServerConnection conn) {
                }
            }, addr.getAddress().getHostAddress(), addr.getPort(), false);
            conn.connect(5000);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(2);
            m.writeTo(output);
            conn.send(output.toByteArray(), Delivery.RELIABLE);
            output.close();
            org.kevoree.log.Log.debug("message ({}) sent to {}", m.getContentClass() , addr.toString());
            return true;
        } catch (Exception e) {
            org.kevoree.log.Log.debug("", e);
        } finally {
            if (conn != null && conn.isConnected()) {
                try {
                    conn.close();
                } catch (Exception ignored){
                }
            }
        }
        return false;
    }

}
