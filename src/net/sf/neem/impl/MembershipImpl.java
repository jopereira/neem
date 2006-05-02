/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2006, University of Minho
 * All rights reserved.
 *
 * Contributors:
 *  - Pedro Santos <psantos@gmail.com>
 *  - Jose Orlando Pereira <jop@di.uminho.pt>
 * 
 * Partially funded by FCT, project P-SON (POSC/EIA/60941/2004).
 * See http://pson.lsd.di.uminho.pt/ for more information.
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

package net.sf.neem.impl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * This class implements the Membership interface. Its methods handle events
 * related with changes in local group membership as well as exchanging local
 * group information with its peers.
 * 
 * @author psantos@GSD
 */
public class MembershipImpl implements Membership, DataListener {
    /**
     * Creates a new instance of MembershipImpl
     * 
     * @param net
     *            Instance of Transport that will be used to pass messages
     *            between peers
     * @param grp_size
     *            The maximum number of members on the local group.
     */
    public MembershipImpl(Transport net, short idport, int grp_size) {
        this.net = net;
        this.idport = idport; // ID passing port
        this.myId = UUID.randomUUID();
        this.grp_size = grp_size;
        this.peers = new HashMap<UUID, Connection>();;
        net.handler(this, this.idport);
        // join will do this!
        //net.membership_handler(this);
    }

    public void receive(ByteBuffer[] msg, Connection info, short port) {
        try {
            UUID id = UUIDUtils.readUUIDFromBuffer(msg);
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(msg);

            //System.err.println("--IDed "+addr+" at "+net.id());
            if (peers.containsKey(id))
				info.close();// only one connection to peer is allowed
			else
				synchronized (this) {
					info.id = id;
					info.listen = addr;
					peers.put(id, info);
				}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void open(Connection info) {
    	//System.err.println("Opened at "+net.id());
    	
    	info.send(new ByteBuffer[] { UUIDUtils.writeUUIDToBuffer(this.myId),
                AddressUtils.writeAddressToBuffer(net.id()) }, this.idport);
        probably_remove();
    }

    public synchronized void close(Connection info) {
        // "CLOSE@" + myId + " : " + addr.toString());
        if (info.id != null) {
            peers.remove(info.id);
        }
    }

    /**
     * If the number of connected peers equals maximum group size, must evict an
     * older member, as the new one has already been added to the local
     * membership by the transport layer. First we must create space for the new
     * member by randomly selectig one of the local members (wich can be the
     * newbie, because it has already been added) then read from socket the
     * address where it's accepting connections. Then increase the number of
     * local members. If the nb_members (current number of members in local
     * membership) is less than grp_size (maximum number of members in local
     * membership), this method only increases by one the number of members.
     */
    private void probably_remove() {
        Connection[] conns = connections();
        int nc = conns.length;
        //int curr_size = peers.size();

        // if (curr_size >= grp_size) {
        while( peers.size() - grp_size > 0) {
            Connection info = conns[rand.nextInt(nc)];
            peers.remove(info.id);
            info.close();
        }
        // info.id = null;
        // }
    }

    /**
     * Get all connections.
     */
    public synchronized Connection[] connections() {
        return peers.values().toArray(new Connection[peers.size()]);
    }

    public Connection getPeer(UUID peerId) {
    	return peers.get(peerId);
    }

    /**
     * Gets the current maximum size for the local membership.
     * 
     * @return The current local membership's maximum size
     */
    public int getGrp_size() {
        return grp_size;
    }

    /**
     * Sets a new value for the maximum size of the local membership.
     * 
     * @param grp_size
     *            The new maximum membership's size.
     */
    public void setGrp_size(int grp_size) {
        this.grp_size = grp_size;
    }


    /**
     * Get all connected peers.
     */
    public synchronized UUID[] getPeers() {
        UUID[] peers = new UUID[this.peers.size()];
        peers = this.peers.keySet().toArray(peers);
        return peers;
    }

    public UUID getId() {
        return myId;
    }

    public Transport net() {
        return this.net;
    }

    /**
     * The peers variable can be queried by an external thread for JMX
     * management. Therefore, all sections of the code that modify it must be
     * synchronized. Sections that read it from the protocol thread need not be
     * synchronized.
     */
    private HashMap<UUID, Connection> peers;

    private short idport;
    private int grp_size;
    
    private UUID myId;

    private Transport net = null;

    private Random rand = new Random();
};

// arch-tag: e99e8d36-d4ba-42ad-908a-916aa6c182d9
