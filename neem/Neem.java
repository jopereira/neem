/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005, University of Minho
 * All rights reserved.
 *
 * Contributors:
 *  - Pedro Santos <psantos@gmail.com>
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  - Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 * 
 *  - Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 * 
 *  - Neither the name of the University of Minho nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Neem.java
 *
 * Created on July 15, 2005, 3:46 PM
 *
 * @author psantos@lsd.di.uminho.pt
 */

package neem;


import java.net.*;
import java.io.*;
import java.nio.*;


/**
 * Encapsulates Transport, Gossip and Membership layers into an easy-to-use/instanciate class
 * @author psantos@gsd.di.uminho.pt
 * 
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
    }
    
    public void connect() {
        Thread t = new Thread(trans);

        t.setDaemon(true);
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
