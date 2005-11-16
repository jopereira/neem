/*
 * Chat.java
 */

package test;


import neem.*;
import java.nio.*;
import java.io.*;
import java.net.*;


/**
 * Simple chat application.
 */
public class Chat extends Thread {
    public Chat(MulticastChannel neem) {
        this.neem=neem;
    }

    private MulticastChannel neem;
    
    public void run() {
        try {
            while(true) {
        		byte[] buf = new byte[1000];
                ByteBuffer bb=ByteBuffer.wrap(buf);
                neem.read(bb);
        		System.out.println("> "+new String(buf));
            }
        } catch(Exception e) {e.printStackTrace();}
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Chat port peer1 ... peerN");
            System.exit(0);
        }

        int fanout = 10;
        int group_size = 20;
        int port = Integer.parseInt(args[0]);

        try {

            MulticastChannel neem = new MulticastChannel(new InetSocketAddress(port), fanout, group_size);
            neem.setLoopbackMode(false);
            System.out.println("Started: "+neem.getLocalSocketAddress());
            if (neem.getLocalSocketAddress().getAddress().isLoopbackAddress())
                System.out.println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.");

            Chat chat=new Chat(neem);    
            chat.setDaemon(true);
            chat.start();

            for (int i = 1; i < args.length; i++) {
                String[] peer=args[i].split(":");
                port = Integer.parseInt(peer[1]);
                neem.connect(new InetSocketAddress(peer[0], port));
            }
        
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String line;

            while ((line = r.readLine()) != null) {
                neem.write(ByteBuffer.wrap(line.getBytes()));
            }

            neem.close();
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// arch-tag: 453e25c0-77de-4ef9-85cd-49689226ed84
