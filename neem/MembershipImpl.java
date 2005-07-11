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

package neem;


import java.io.*;
import java.nio.*;
import java.net.*;
import java.lang.*;
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
        this.msgs = new HashSet<UUID>();
        // this.peers = new HashSet<InetSocketAddress>();
        net.handler(this, this.syncport);
        net.membership_handler(this);
        
    }
    
    public void receive(ByteBuffer[] msg, Transport.Connection info) {
        // System.out.println("Membership Receiving Message");
        try {
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(msg);

            // System.out.println("Receive Address: " + addr.toString());
            this.net.add(addr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void open(Transport.Connection info, int i) {
        if (this.firsttime) {
            net.schedule(this, 5000);
            firsttime = false;
        }
        probably_remove();
    }
    
    public void close(InetSocketAddress addr) {
        //System.out.println(
          //      "CLOSE@" + net.id().toString() + " : " + addr.toString());
        this.nb_members--;
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
        Transport.Connection[] conns = this.net.connections();
        int nc = conns.length;

        if (nb_members == grp_size-1) {
            Transport.Connection info = conns[rand.nextInt(nc)];

            this.net.remove(info.addr);
            nb_members++;
        } else {
            nb_members++;
        }
    }
    
    public void run() {
        distributeConnections();
        net.schedule(this, 5000);
    }

    /**
     *  Tell a fanout number of members of my local membership, that there is a
     * connection do the peer identified by its address, wich is sent to the 
     * peers.
     */ 
    private void distributeConnections() {
        Transport.Connection[] conns = this.net.connections();
        int nc = conns.length;
    if(nc > 0){
        InetSocketAddress addr = conns[rand.nextInt(nc)].addr;
        relay(new ByteBuffer[] { AddressUtils.writeAddressToBuffer(addr)},
                this.fanout, this.syncport);
    }
    }
    
    protected HashSet<UUID> msgs;
    private short syncport;
    private int fanout, grp_size;
    private int nb_members = 0;
    private boolean firsttime = true;
}

 
;
// arch-tag: e99e8d36-d4ba-42ad-908a-916aa6c182d9
