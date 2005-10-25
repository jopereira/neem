/*
 * Big.java
 *
 * Created on March 22, 2005, 5:04 PM
 */

package test;


import neem.*;
import java.nio.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.net.*;


/** Test Application
 *
 * @author psantos@GSD
 */
public class Big implements App {
    private int cicle;
    
    /** Creates a new instance of Big */
    public Big(int i) {
        this.cicle = i;
    }
    
    public static void sendMessage(String message, Neem gimpl, int id) {
        ByteArrayOutputStream baos;
        ObjectOutputStream os;
        byte[] ret1, ret2, ret3;
        Random random = new Random();
        Long time;
	
        try {
            baos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(baos);
            time = new Long(System.nanoTime());
            os.writeObject(time);
            ret1 = new byte[baos.size()];
            ret1 = baos.toByteArray();
            
            baos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(baos);
            os.writeObject(new Integer(id));
            ret2 = new byte[baos.size()];
            ret2 = baos.toByteArray();
            
            baos = new ByteArrayOutputStream();
            os = new ObjectOutputStream(baos);
            os.writeObject(message);
            ret3 = new byte[baos.size()];
            ret3 = baos.toByteArray();
                
            ByteBuffer msg1 = ByteBuffer.allocate(1048);
            ByteBuffer msg2 = ByteBuffer.allocate(1048);
            ByteBuffer msg3 = ByteBuffer.allocate(1048);

            msg1.put(ret1);
            msg1.flip();
            msg2.put(ret2);
            msg2.flip();
            msg3.put(ret3);
            msg3.flip();
            
            ByteBuffer[] msg = new ByteBuffer[] { msg3, msg1, msg2};
            ByteBuffer[] out = new ByteBuffer[(msg.length * 2)];
            ByteBuffer[] final_msg = intercalate(msg);

            System.arraycopy(final_msg, 0, out, 0, final_msg.length);
            
            gimpl.multicast(out);
            String s = new String(
                    "send: " + gimpl.getTransportIdAsString() + " "
                    + System.nanoTime() + " " + id + "\n");

            // System.out.println(s);
            FileOps.write(s, "exec." + gimpl.getTransportIdAsString() + ".log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * @param msg 
     * @param gimpl 
     */
    public void deliver(ByteBuffer[] msg, Gossip gimpl) {
        ByteBuffer rec = Buffers.compact(msg);
        ByteArrayInputStream bais;
        ObjectInputStream ois;
        String s;
        Long valuei;
        Integer valuej;
	
        byte[] dst;

        try {
            int k = rec.getInt();

            dst = new byte[k];
            rec.get(dst, 0, k);
            bais = new ByteArrayInputStream(dst);
            ois = new ObjectInputStream(bais);
            s = new String((String) ois.readObject());
	    
            int i = rec.getInt();

            dst = new byte[i];
            rec.get(dst, 0, i);
            bais = new ByteArrayInputStream(dst);
            ois = new ObjectInputStream(bais);
            valuei = ((Long) ois.readObject());
                    
            int j = rec.getInt();

            dst = new byte[j];
            rec.get(dst, 0, j);
            bais = new ByteArrayInputStream(dst);
            ois = new ObjectInputStream(bais);
            valuej = new Integer((Integer) ois.readObject());
            
            String s2 = new String(
                    "recv: " + s + " " + System.nanoTime() + " "
                    + valuej.intValue() + "\n");

            // System.out.println(s2);
            FileOps.write(s2, "exec." + gimpl.net().idString() + ".log");
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        }
    }
    
    public static ByteBuffer[] intercalate(ByteBuffer[] msg) {
        // put length before each object of the message
        ByteBuffer[] ret = new ByteBuffer[msg.length * 2];
        int size;

        for (int i = 0; i < msg.length; i++) {
            size = msg[i].remaining();
            ret[i * 2] = ByteBuffer.allocate(size);
            ret[i * 2].putInt(size);
            ret[i * 2].flip();
            ret[(i * 2) + 1] = msg[i];
        }
        return ret;
    }

    public static void main(String[] args) {
        
        if (args.length < 1) {
            System.err.println("Usage: Big local_ip remote_ip");
            System.exit(0);
        }
        int fanout = 10;
        int group_size = 20;
        short g_syncport = 1;
        short m_syncport = 0;
        String address = new String("192.168.82.137");
        // GossipImpl[] gimpls = new GossipImpl[group_size];
        // Membership[] mimpls = new Membership[group_size];
        Random random = new Random();
        // Transport trans = null;
        Neem[] neems = new Neem[group_size];
	
        inet_s_arr = new InetSocketAddress[group_size];

        for (int i = 0; i < group_size; i++) {
            // boolean connected = true;
            int port = 12346;

            // do {
            // try {
            // trans = new Transport(new InetSocketAddress(args[0], port));
            // connected = true;		    
            // } catch (BindException be) {
            // port++;
            // connected = false;
            // } catch (IOException ie) {
            // ie.printStackTrace();
            // port++;
            // connected = false;
            // }
            // } while (!connected);
            neems[i] = ConfigLoader.load(args[0]);
            // neems[i] = new Neem(args[0], port, g_syncport, m_syncport, fanout,
            // group_size);
            //
            // gimpls[i] = new GossipImpl(trans, g_syncport, fanout, group_size);
            //
            // mimpls[i] = new MembershipImpl(trans, m_syncport, fanout, group_size);
            
            neems[i].setHandler(new Big(i));
            if (neems[i] != null) {
                neems[i].connect();
            } else {
                neems[i] = new Neem(args[0], port, g_syncport, m_syncport,
                        fanout, group_size);
            }
            inet_s_arr[i] = neems[i].getTransportId();
        }
	
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {}

        for (int i = 0; i < group_size; i++) {
            for (int j = 0; j < fanout; j++) {
                neems[i].add(args[1], (12345 + random.nextInt(group_size)));
                        
            }
        }

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {}
	
        int id = 0;

        while (true) {
            int index = random.nextInt(group_size);
            Neem gimpl = neems[index];

            sendMessage(
                    inet_s_arr[index].getHostName()
                            + " Patati Patata ola ole zzzzzzzzzzz wwwwwwwwww hhhhhhhhh",
                            gimpl,
                            id);
            id++;
            // id = id % 100;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {}
        }
        
    }

    protected static InetSocketAddress inet_s_arr[];

    /** the thing is, the connections are bidirectional, ie, 
     * the fanout is at least 2 but can be grater if 
     * impls[index] is chosen by more than one of its peers,
     * since every open connection, either for accept or connect
     * is stored in connections[] in Transport. :D
     */
}


;

// arch-tag: 95c1a028-daa7-4240-9ae7-488ad1edab8c
