/*
 * Neem.java
 *
 * Created on July 15, 2005, 3:46 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package neem;


import java.net.*;
import java.io.*;
import java.nio.*;


/**
 * Encapsulates Transport, Gossip and Membership layers into an easy-to-use/instanciate class
 * @author psantos@gsd.di.uminho.pt
 * @author si24803@gmail.com
 */
public class Neem {
    
    /** Creates a new instance of Neem */
    public Neem(String local, int port, short g_syncport, short m_syncport, int fanout, int group_size) {
        this.listenon = port;
        while (!connected) {
            this.listenon = port;
            try {
                trans = new Transport(
                        new InetSocketAddress(local, this.listenon));
                this.connected = true;
            } catch (BindException be) {
                port++;
                connected = false;
            } catch (IOException ie) {
                ie.printStackTrace();
                port++;
                connected = false;
            }
        }
		
        gimpls = new GossipImpl(trans, g_syncport, fanout, group_size);
	
        mimpls = new MembershipImpl(trans, m_syncport, fanout, group_size);
	
        Thread t = new Thread(trans);

        t.start();
        
    }
    
    /** If someone wants to find out wether i'm connected*/
    public boolean getConnected() {
        return this.connected;
    }
    
    /** If someone wants to find out where i'm connected*/
    public int getListenOn() {
        return this.listenon;
    }
    
    /** Get my Transport's id*/
    public InetSocketAddress getTransportId() {
        return this.trans.id();
    }
    
    /** Get my Transport's id as a printable string*/
    public String getTransportIdAsString() {
        return this.trans.idString();
    }
    
    /** Set my gossip related events handler*/
    public void setHandler(App handler) {
        this.gimpls.handler(handler);
    }
    
    public void add(String args1, int args2) {
        gimpls.add(new InetSocketAddress(args1, args2));
    }
    
    public void multicast(ByteBuffer[] msg) {
        gimpls.multicast(msg);
    }
    
    /** Transport layer*/
    private Transport trans = null;

    /** Gossip layer*/
    private GossipImpl gimpls = null;

    /** Membership layer*/
    private Membership mimpls = null;

    /** Am I Up?*/
    private boolean connected = false;

    /** Where am i up?*/
    private int listenon;
}

 
// arch-tag: cd998499-184b-4c75-a0a0-34180eb3c92c
