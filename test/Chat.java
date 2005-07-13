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
public class Chat implements App {
    public void deliver(ByteBuffer[] msg, Gossip gimpl) {
        ByteBuffer rec = Buffers.compact(msg);
        byte[] buf = new byte[rec.remaining()];

        rec.get(buf);
        System.out.println(new String(buf));
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Chat local_ip peer1 ... peerN");
            System.exit(0);
        }

        int fanout = 10;
        int group_size = 20;
        short g_syncport = 1;
        short m_syncport = 0;
        int port = 12345;

        try {

            Transport trans = new Transport(new InetSocketAddress(args[0], port));
            GossipImpl gimpl = new GossipImpl(trans, g_syncport, fanout,
                    group_size);
            MembershipImpl mimpl = new MembershipImpl(trans, m_syncport, fanout,
                    group_size);
		
            gimpl.handler(new Chat());
            Thread t = new Thread(trans);

            t.setDaemon(true);

            t.start();

            for (int i = 1; i < args.length; i++) {
                gimpl.add(new InetSocketAddress(args[i], port));
            }
        
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(System.in));
            String line;

            while ((line = r.readLine()) != null) {
                gimpl.multicast(
                        new ByteBuffer[] { ByteBuffer.wrap(line.getBytes())});
            }
	
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// arch-tag: 453e25c0-77de-4ef9-85cd-49689226ed84
