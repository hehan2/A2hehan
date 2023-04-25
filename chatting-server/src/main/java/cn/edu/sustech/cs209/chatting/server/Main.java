package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Main {
    private static byte[] incoming = new byte[256];
    private static final int PORT = 8000;
    private static DatagramSocket socket;

    private static InetAddress address = null;

    private static List<User> users = new ArrayList<>();

    private static Map<String, List<String>> groups = new HashMap<>();
    public static void main(String[] args) {

        System.out.println("Starting server");
        try {
            socket = new DatagramSocket(PORT);
            address = InetAddress.getByName("localhost");
        }
        catch (SocketException | UnknownHostException e){
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (int i = 0; i < users.size(); i++) {
                User user  = users.get(i);
                byte[] mes = ("serverDown").getBytes();
                DatagramPacket packet = new DatagramPacket(mes, mes.length, user.getAdr(), user.getPort());
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
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
                if(findUser(messageSet[3]) != null){
                    byte[] mes = ("repeatName").getBytes();
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, adr, port);
                    try {
                        socket.send(packet1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
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
            else if(messageSet[0].equals("normal")){
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
            else if (messageSet[0].equals("formingGroup")){
                int index = message.indexOf(")");
                String[] groupUsers = message.substring(index + 2).split(",");
                String groupName = message.substring(13, index + 1);
                if(groups.containsKey(groupName)){
                    continue;
                }
                else{
                    List<String> groupMember = new ArrayList<>(Arrays.asList(groupUsers));
                    groups.put(groupName, groupMember);
                }
                messageSet = message.substring(0, index + 1).split(",");
                String newMes = message.substring(0, index + 1);
                for (int i = 0; i < groupUsers.length; i++) {
                    String userName = groupUsers[i];
                    User user = findUser(userName);
                    int port = user.getPort();
                    InetAddress adr = user.getAdr();
                    byte[] mes = newMes.getBytes();
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, adr, port);
                    try {
                        socket.send(packet1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
            else if(messageSet[0].equals("groupMessage")){
                String groupName = "";
                for (int i = 3; i < messageSet.length - 1; i++) {
                    if(i != messageSet.length - 2){
                        groupName += messageSet[i] + ",";
                    }
                    else{
                        groupName += messageSet[i];
                    }
                }
                List<String> groupUser = groups.get(groupName);
                for (int i = 0; i < groupUser.size(); i++) {
                    String userName = groupUser.get(i);
                    System.out.println(userName);
                    if(userName.equals(messageSet[2])){
                        continue;
                    }
                    User user = findUser(userName);
                    int port = user.getPort();
                    InetAddress adr = user.getAdr();
                    String data = messageSet[messageSet.length-1];
                    byte[] mes = ("groupMessage," + messageSet[1] + "," + messageSet[2] + "," +user.getName() + "," + messageSet[messageSet.length - 1] + ",#" + groupName).getBytes();
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, adr, port);
                    try {
                        socket.send(packet1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            else if(messageSet[0].equals("closeCon")){
                User offUser = findUser(messageSet[1]);
                users.remove(offUser);
                for (int i = 0; i < users.size(); i++) {
                    User user = users.get(i);
                    byte[] mes = message.getBytes();
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, user.getAdr(), user.getPort());
                    try {
                        socket.send(packet1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else if(messageSet[0].equals("deleteGroup")){
                groups.remove(messageSet[1]);
            }
            else if(message.startsWith("updateGroup")){
                messageSet = message.split(";");
                String oldGroup = messageSet[1];
                String newGroup = messageSet[2];
                String offUserName = messageSet[3];
                List<String> groupMember = groups.get(oldGroup);
                if(messageSet.length == 5){
                    for (int i = 0; i < groupMember.size(); i++) {
                        if(!oldGroup.contains(groupMember.get(i))){
                            String addingName = groupMember.get(i);
                            if(newGroup.contains("...")){
                                int index = newGroup.indexOf("...");
                                newGroup = newGroup.substring(0, index) + addingName + ", " + newGroup.substring(index);
                            }
                            else{
                                int index = newGroup.indexOf("(");
                                newGroup = newGroup.substring(0, index - 1) + ", " + addingName + " "+ newGroup.substring(index);
                            }
                        }
                    }
                }
                if(groups.containsKey(newGroup)){
                    continue;
                }
                groupMember.remove(offUserName);
                groups.remove(oldGroup);
                groups.put(newGroup, groupMember);
                for (int i = 0; i < groupMember.size(); i++) {
                    User user = findUser(groupMember.get(i));
                    byte[] mes = (messageSet[0] + ";" + messageSet[1]+";"+newGroup+";"+messageSet[3]).getBytes();
                    assert user != null;
                    DatagramPacket packet1 = new DatagramPacket(mes, mes.length, user.getAdr(), user.getPort());
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
