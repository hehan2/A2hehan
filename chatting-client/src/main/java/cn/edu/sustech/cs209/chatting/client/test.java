package cn.edu.sustech.cs209.chatting.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class test {
    private static final int SERVER_PORT = 8000;
    private static final InetAddress address;

    static {
        try {
            address = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        System.out.println(address);
        InetAddress ad = InetAddress.getByName((address+"").split("/")[1]);
        System.out.println(ad);
    }
}