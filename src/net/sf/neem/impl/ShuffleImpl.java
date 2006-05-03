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
import java.util.Random;
import java.util.UUID;

/**
 * This class implements a simple membership shuffling protocol.
 */
public class ShuffleImpl implements DataListener, Membership, Runnable {
    private MembershipImpl memb;

	public ShuffleImpl(Transport net, MembershipImpl memb, short syncport, int grp_size) {
        this.net = net;
        this.memb = memb;
        this.grp_size = grp_size;
        this.syncport = syncport; // Connection setup port
        net.handler(this, this.syncport);
        net.membership_handler(this);
    }

    public void receive(ByteBuffer[] msg, Connection info, short port) {
        try {
        	ByteBuffer[] beacon=Buffers.clone(msg);
        	
            UUID id = UUIDUtils.readUUIDFromBuffer(msg);
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(msg);

            if (memb.getPeer(id)!=null)
                return;

            Connection[] peers=memb.connections();
            
            // Flip a coin...
            if (peers.length==0 || rand.nextFloat()>0.5) {
                //System.err.println("Open locally!");
            	net.add(addr);
            } else {
                //System.err.println("Forward remotely!");
                int idx=rand.nextInt(peers.length);
                peers[idx].send(Buffers.clone(beacon), this.syncport);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
    	distributeConnections();
        net.schedule(this, this.distConnsPeriod);
    }

    /**
     * Tell a member of my local membership, that there is a
     * connection do the peer identified by its address, wich is sent to the
     * peers.
     */
    private void distributeConnections() {
        Connection[] conns = memb.connections();
        if (conns.length<2)
        	return;
		Connection toSend = conns[rand.nextInt(conns.length)];
		Connection toReceive = conns[rand.nextInt(conns.length)];

		if (toSend.id == null)
			return;

		this.tradePeers(toReceive, toSend);
    }
    
    public void tradePeers(Connection target, Connection arrow) {
        target.send(new ByteBuffer[] {
                UUIDUtils.writeUUIDToBuffer(arrow.id),
                AddressUtils.writeAddressToBuffer(arrow.listen) },
                this.syncport);
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
     * Gets the current period of the call to distributeConnections
     * 
     * @return The current period
     */
    public int getDistConnsPeriod() {
        return distConnsPeriod;
    }

    /**
     * Sets a new period of the call to distributeConnections
     * 
     * @param distConnsPeriod
     *            the new period
     */
    public void setDistConnsPeriod(int distConnsPeriod) {
        this.distConnsPeriod = distConnsPeriod;
    }

    private short syncport;
    private int grp_size;
    
    private int distConnsPeriod = 1000;

    private Transport net = null;

    private Random rand = new Random();

	public void open(Connection info) {
		for(int i=0;i<grp_size/2;i++) {
			info.send(new ByteBuffer[] {
                UUIDUtils.writeUUIDToBuffer(memb.getId()),
                AddressUtils.writeAddressToBuffer(net.id()) },
                this.syncport);
		}
        net.schedule(this, this.distConnsPeriod);
        net.membership_handler(memb);
        memb.open(info);
	}

	public void close(Connection info) {
		// should never happen...
	}
};

// arch-tag: bacc9982-5ada-4d8f-a1d0-18b4b2b12f7d
