package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientThread extends Thread{
    private Controller controller;
    private final DatagramSocket socket;
    private byte[] incoming =new byte[256];
    private boolean running = true;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public ClientThread(Controller controller, DatagramSocket socket){
        this.controller = controller;
        this.socket = socket;
    }
    public void run(){
        System.out.println("local thread starting..");
        while (running){
            DatagramPacket packet = new DatagramPacket(incoming, incoming.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println(message.length());
            System.out.println("clinet receiving data: "+message);
            if(message.startsWith("add")){
                controller.addUser(message.split(",")[1]);
            }
            else if(message.startsWith("normal")){
                String[] mesSet = message.split(",");
                long stamp = Long.parseLong(mesSet[1]);
                String sendBy = mesSet[2];
                String sendTo = mesSet[3];
                String data = mesSet[4];
                Message mes = new Message(stamp, sendBy, sendTo, data);
                Platform.runLater(() -> {
                    controller.receiveMes(sendBy, mes);
                });
            }
            else if (message.startsWith("onlineUser")){
                String[] mesSet = message.split(",");
                for (int i = 1; i < mesSet.length; i++) {
                    controller.addUser(mesSet[i]);
                }
            }
            else if (message.startsWith("formingGroup")){
                String groupName = message.substring(13);
                Platform.runLater(() -> {
                    controller.groupMessage(groupName);
                });
            }
            else if(message.startsWith("groupMessage")){
                String[] mesSet = message.split(",");
                String groupName = mesSet[mesSet.length-1];
                long stamp = Long.parseLong(mesSet[1]);
                String sendBy = mesSet[2];
                String sendTo = message.substring(message.indexOf("#") + 1);
                String data = mesSet[4];
                Message mes = new Message(stamp, sendBy, sendTo, data);
                Platform.runLater(() -> {
                    controller.receiveGroupMes(mes, sendTo);
                });
            }

            else if(message.startsWith("closeCon")){
                String offUser = message.split(",")[1];
                Platform.runLater(() -> {
                    controller.offUser(offUser);
                });
            }
            else if(message.startsWith("updateGroup")){
                String[] messageSet = message.split(";");
                String oldGroup = messageSet[1];
                String newGroup = messageSet[2];
                Platform.runLater(() -> {
                    controller.updateGroup(oldGroup, newGroup);
                });

            }

        }
    }
}
