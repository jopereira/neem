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
 * MembershipImpl.java
 *
 * Created on March 30, 2005, 5:17 PM
 */

package neem.impl;


import java.nio.*;
import java.net.*;
import java.util.*;

/**
 *  This class implements the Membership interface. Its methods handle events
 * related with changes in local group membership as well as exchanging local
 * group information with its peers.
 *
 * @author psantos@GSD
 */
public class MembershipImpl extends AbstractGossipImpl implements Membership, DataListener, Runnable {
	/**
     *  Creates a new instance of MembershipImpl
     * @param net Instance of Transport that will be used to pass messages between peers
     * @param port Synchronization port, i.e., the port to wich membership related messages must be sent. Specifies a logic, not socket, port.
     * @param fanout The number of peers to be warned of local membership changes or local group members addresses.
     * @param grp_size The maximum number of members on the local group.
     */
    public MembershipImpl(Transport net, short port, int fanout, int grp_size) {
        this.net = net;
        this.fanout = fanout;
        this.grp_size = grp_size;
        this.syncport = port;
        this.peers = new HashMap<InetSocketAddress,Transport.Connection>();
        net.handler(this, this.syncport);
        net.handler(this, (short)3);
        net.membership_handler(this);
        
    }
    
    public void receive(ByteBuffer[] msg, Transport.Connection info, short port) {
        // System.out.println("Membership Receiving Message");
        try {
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(Buffers.sliceCompact(msg,6));

            if (port==syncport) {
            	 // System.out.println("Receive from "+info.addr+"+ address "+addr);
            	if (!peers.containsKey(addr) && !addr.equals(net.id()))
            		this.net.add(addr);
            } else {
            	// System.out.println("Discovered that "+info.addr+" is "+addr);
            	if (peers.containsKey(addr))
            		net.remove(info.addr);
            	else {
            		peers.put(addr, info);
            		info.id=addr;
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void open(Transport.Connection info) {
        if (this.firsttime) {
            net.schedule(this, 5000);
            firsttime = false;
        }
        net.send(new ByteBuffer[]{ AddressUtils.writeAddressToBuffer(net.id()) }, info, (short)3);
        probably_remove();
    }
    
    public void close(Transport.Connection info) {
        // System.out.println(
        // "CLOSE@" + net.id().toString() + " : " + addr.toString());
    	if (info.id!=null)
    		peers.remove(info.id);
    }
    
    /**
     *  If the number of connected peers equals maximum group size, must evict an
     * older member, as the new one has already been added to the local membership
     * by the transport layer. First we must create space for the new member by 
     * randomly selectig one of the local members (wich can be the newbie, because
     * it has already been added) then read from socket the address where it's 
     * accepting connections. Then increase the number of local members. If the
     * nb_members (current number of members in local membership) is less than 
     * grp_size (maximum number of members in local membership), this method only
     * increases by one the number of members.
     */
    private void probably_remove() {
        Transport.Connection[] conns = connections();
        int nc = conns.length;

        if (peers.size() >= grp_size) {
            Transport.Connection info = conns[rand.nextInt(nc)];
            info.id=null;
            this.net.remove(info.addr);
        }
    }
    
    public void run() {
    	// System.out.println("--- current peers: "+peers);
        if (peers.size()==0)
        	this.firsttime=true;
        else {
            distributeConnections();
        	net.schedule(this, 5000);
        }
    }

    /**
     *  Tell a fanout number of members of my local membership, that there is a
     * connection do the peer identified by its address, wich is sent to the 
     * peers.
     */ 
    private void distributeConnections() {
        Transport.Connection[] conns = connections();
       	InetSocketAddress addr = conns[rand.nextInt(conns.length)].id;

       	// System.out.println("Disseminating "+addr);
        relay(new ByteBuffer[] { AddressUtils.writeAddressToBuffer(addr)},
            this.fanout, this.syncport, conns);
    }
    
    /**
     * Get all connections.
     */
    public Transport.Connection[] connections() {
    	return peers.values().toArray(new Transport.Connection[peers.size()]);
    }
    
    
    private Map<InetSocketAddress,Transport.Connection> peers;
    private short syncport;
    private int fanout, grp_size;
    private boolean firsttime = true;
}

 
;
// arch-tag: e99e8d36-d4ba-42ad-908a-916aa6c182d9
