package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static byte[] incoming = new byte[256];
    private static final int PORT = 8000;
    private static DatagramSocket socket;

    private static InetAddress address = null;

    private static List<User> users = new ArrayList<>();
    public static void main(String[] args) {
        System.out.println("Starting server");
        try {
            socket = new DatagramSocket(PORT);
            address = InetAddress.getByName("localhost");
        }
        catch (SocketException | UnknownHostException e){
            e.printStackTrace();
        }
        while (true){
            DatagramPacket packet = new DatagramPacket(incoming, incoming.length);
            try {
                socket.receive(packet);
            }
            catch (IOException e){
                e.printStackTrace();
            }
            String message = new String(packet.getData(), 0, packet.getLength());
            System.out.println("receiving data: " + message);
            String[] messageSet = message.split(",");
            if(messageSet[0].equals("init")){
                int port = Integer.parseInt(messageSet[1]);
                InetAddress adr = null;
                try {
                    adr = InetAddress.getByName(messageSet[2]);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
                User newUser = new User(port, adr, messageSet[3]);
                users.add(newUser);
                byte[] mes= ("add," + messageSet[3]).getBytes();
                for (User user : users) {
                    if(user != newUser){
                        DatagramPacket packet1 = new DatagramPacket(mes, mes.length, user.getAdr(), user.getPort());
                        try {
                            socket.send(packet1);
                            System.out.println("sending add successfully");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else{
                        String onlineUser = "";
                        for (int i = 0; i < users.size(); i++) {
                            onlineUser = onlineUser + users.get(i).getName() + ",";
                        }
                        byte[] Ownmes = ("onlineUser," + onlineUser).getBytes();
                        DatagramPacket packet1 = new DatagramPacket(Ownmes, Ownmes.length, user.getAdr(), user.getPort());
                        try {
                            socket.send(packet1);
                            System.out.println("sending add successfully");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            if(messageSet[0].equals("normal")){
                String sendTo = messageSet[3];
                User user = findUser(sendTo);
                if(user == null){
                    System.out.println("No such user");
                }
                else{
                    int port = user.getPort();
                    InetAddress adr = user.getAdr();
                    byte[] mes = message.getBytes();
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, adr, port);
                    try {
                        socket.send(packet1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }

    }
    public static User findUser(String target){
        for (int i = 0; i < users.size(); i++) {
            if(users.get(i).getName().equals(target)){
                return users.get(i);
            }
        }
        return null;
    }
}
