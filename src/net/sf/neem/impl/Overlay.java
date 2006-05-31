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

package net.sf.neem.impl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Implementation of overlay management. This class combines a
 * number of random walks upon initial join (liek SCAMP) with
 * periodic shuffling.
 */
public class Overlay implements ConnectionListener, DataListener, Runnable {
    /**
     * Creates a new instance of Overlay
     */
    public Overlay(Transport net, short idport, short shuffleport) {
        this.net = net;
        this.idport = idport;
        this.shuffleport = shuffleport;
   
        this.maxPeers = 10;
        this.shufflePeriod = 1000;

        this.myId = UUID.randomUUID();
        this.peers = new HashMap<UUID, Connection>();;

        net.setDataListener(this, this.shuffleport);
        net.setDataListener(this, this.idport);
        net.setConnectionListener(this);
    }

    public void receive(ByteBuffer[] msg, Connection info, short port) {
    	if (port==this.idport)
    		handleId(msg, info);
    	else
    		handleShuffle(msg);
    }
    
    private void handleId(ByteBuffer[] msg, Connection info) {
        UUID id = UUIDs.readUUIDFromBuffer(msg);
		InetSocketAddress addr = Addresses.readAddressFromBuffer(msg);

		// System.err.println("--IDed "+addr+" at "+net.id());
		if (peers.containsKey(id))
			info.close();
		else
			synchronized (this) {
				info.id = id;
				info.listen = addr;
				peers.put(id, info);
			}
    }

    private void handleShuffle(ByteBuffer[] msg) {
    	ByteBuffer[] beacon = Buffers.clone(msg);

		UUID id = UUIDs.readUUIDFromBuffer(msg);
		InetSocketAddress addr = Addresses.readAddressFromBuffer(msg);

		if (peers.containsKey(id))
			return;

		// Flip a coin...
		if (peers.size() == 0 || rand.nextFloat() > 0.5) {
			// System.err.println("Open locally!");
			net.add(addr);
		} else {
			// System.err.println("Forward remotely!");
			Connection[] conns = connections();
			int idx = rand.nextInt(conns.length);
			conns[idx].send(Buffers.clone(beacon), this.shuffleport);
		}
    }

    public void open(Connection info) {
		// System.err.println("Opened at "+net.id());
    	if (firsttime) {
			for (int i = 0; i < maxPeers / 2; i++) {
				info.send(new ByteBuffer[] {
						UUIDs.writeUUIDToBuffer(myId),
						Addresses.writeAddressToBuffer(net.id()) },
						this.shuffleport);
			}
			firsttime=false;
			net.schedule(this, this.shufflePeriod);
		}
    	
    	info.send(new ByteBuffer[] { UUIDs.writeUUIDToBuffer(this.myId),
                Addresses.writeAddressToBuffer(net.id()) }, this.idport);
        purgeConnections();
    }

    public synchronized void close(Connection info) {
        // "CLOSE@" + myId + " : " + addr.toString());
        if (info.id != null) {
            peers.remove(info.id);
        }
    }

    private void purgeConnections() {
        Connection[] conns = connections();
        int nc = conns.length;
        //int curr_size = peers.size();

        // if (curr_size >= maxPeers) {
        while( peers.size() - maxPeers > 0) {
            Connection info = conns[rand.nextInt(nc)];
            peers.remove(info.id);
            info.close();
        }
        // info.id = null;
        // }
    }

    public void run() {
        if (peers.isEmpty()) {
        	firsttime=true;
        	return;
        }
    	distributeConnections();
        net.schedule(this, this.shufflePeriod);
    }

    /**
     * Tell a neighbor, that there is a connection do the peer
     * identified by its address, wich is sent to the
     * peers.
     */
    private void distributeConnections() {
        Connection[] conns = connections();
        if (conns.length<2)
        	return;
		Connection toSend = conns[rand.nextInt(conns.length)];
		Connection toReceive = conns[rand.nextInt(conns.length)];

		if (toSend.id == null)
			return;

		this.tradePeers(toReceive, toSend);
    }
    
    /**
     * Connect two other peers by informing one of the other.
     * 
     * @param target The connection peer.
     * @param arrow The accepting peer.
     */
    public void tradePeers(Connection target, Connection arrow) {
        target.send(new ByteBuffer[] {
                UUIDs.writeUUIDToBuffer(arrow.id),
                Addresses.writeAddressToBuffer(arrow.listen) },
                this.shuffleport);
    }

    /**
     * Get all connections that have been validated.
     */
    public synchronized Connection[] connections() {
        return peers.values().toArray(new Connection[peers.size()]);
    }

    /**
     * Get all connected peers.
     */
    public synchronized UUID[] getPeers() {
        UUID[] peers = new UUID[this.peers.size()];
        peers = this.peers.keySet().toArray(peers);
        return peers;
    }

    /**
     * Get all peer addresses.
     */
    public synchronized InetSocketAddress[] getPeerAddresses() {
        InetSocketAddress[] addrs = new InetSocketAddress[this.peers.size()];
        int i=0;
        for(Connection peer: peers.values())
        	addrs[i++]=peer.listen;
        return addrs;
    }
    
    /**
     * Get globally unique ID in the overlay.
     */
    public UUID getId() {
        return myId;
    }

    private Transport net;
    private short shuffleport;
    private short idport;
    
    /**
     * The peers variable can be queried by an external thread for JMX
     * management. Therefore, all sections of the code that modify it must be
     * synchronized. Sections that read it from the protocol thread need not be
     * synchronized.
     */
    private HashMap<UUID, Connection> peers;

    private UUID myId;
    private boolean firsttime=true;
    private Random rand = new Random();

    // Configuration parameters
    
    private int shufflePeriod;
    private int maxPeers;
    
    public int getMaxPeers() {
        return maxPeers;
    }

    public void setMaxPeers(int maxPeers) {
        this.maxPeers = maxPeers;
    }

    public int getShufflePeriod() {
        return shufflePeriod;
    }

    public void setShufflePeriod(int shufflePeriod) {
        this.shufflePeriod = shufflePeriod;
    }
}

// arch-tag: e99e8d36-d4ba-42ad-908a-916aa6c182d9
