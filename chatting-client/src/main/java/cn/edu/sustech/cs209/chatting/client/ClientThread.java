package cn.edu.sustech.cs209.chatting.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientThread extends Thread{
    private Controller controller;
    private final DatagramSocket socket;
    private byte[] incoming =new byte[256];
    public ClientThread(Controller controller, DatagramSocket socket){
        this.controller = controller;
        this.socket = socket;
    }
    public void run(){
        System.out.println("local thread starting..");
        while (true){
            DatagramPacket packet = new DatagramPacket(incoming, incoming.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String message = new String(packet.getData(), 0, packet.getLength()) + "\n";
            System.out.println("clinet receiving data: "+message);
            if(message.startsWith("add")){
                controller.addUser(message.split(",")[1]);
            }
        }
    }
}
