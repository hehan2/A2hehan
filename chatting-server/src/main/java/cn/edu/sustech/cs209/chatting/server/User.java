package cn.edu.sustech.cs209.chatting.server;

import java.net.InetAddress;

public class User {
    int port;
    InetAddress adr;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getAdr() {
        return adr;
    }

    public void setAdr(InetAddress adr) {
        this.adr = adr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String name;
    public User(int port, InetAddress adr, String name){
        this.port = port;
        this.adr = adr;
        this.name = name;
    }
}
