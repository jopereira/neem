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


import java.util.*;
import java.nio.*;


/**
 * This class implements the Gossip interface. Its methods provide message 
 * exchanging between group members. Provides methods for applications to send
 * messages to the group as well as delivery of incoming messages to them.
 * 
 * @author psantos@GSD
 */

public class GossipImpl implements Gossip, DataListener {

	/**
     *  Creates a new instance of GossipImpl.
     */
    public GossipImpl(Transport net, MembershipImpl memb, short port, short ctrlport) {
        this.memb = memb;
        this.syncport = port;
        this.ctrlport = ctrlport;

        this.fanout = 4;
        this.maxHops = 10;
        this.minHops = 3;
        this.minSize = 64;

        this.msgs = new LinkedHashSet<UUID>();
        this.cache = new LinkedHashMap<UUID,ByteBuffer[]>();
        this.queued = new LinkedHashSet<UUID>();

        net.handler(this, this.syncport);
        net.handler(this, this.ctrlport);
    }
    
    public void handler(App handler) {
        this.handler = handler;
    }
        
    public void multicast(ByteBuffer[] msg) {
    	handleData(msg, UUID.randomUUID(), (byte)0);
    }
    
    public void receive(ByteBuffer[] msg, Connection info, short port) { 
    	// System.out.println("Receive@Gossip: " + msg.length);
    	UUID uuid = UUIDUtils.readUUIDFromBuffer(msg);
    	byte hops = Buffers.sliceCompact(msg, 1).get();

    	if (port == this.syncport)
			handleData(msg, uuid, hops);
		else if (port == this.ctrlport)
			handleControl(uuid, hops, info);
	}
    
    private void handleData(ByteBuffer[] msg, UUID uuid, byte hops) {    
		if (!msgs.add(uuid))
			return;
		
		queued.remove(uuid);
		
		ByteBuffer[] copy = Buffers.clone(msg);

		// only pass to application a clean message
		this.handler.deliver(msg);
		purgeMsgs();

		hops++;
		
		if (hops>maxHops)
			return;
		
		ByteBuffer[] out = new ByteBuffer[msg.length + 2];
		out[0] = UUIDUtils.writeUUIDToBuffer(uuid);
		out[1] = ByteBuffer.wrap(new byte[] { hops });
		System.arraycopy(copy, 0, out, 2, copy.length);

		if (hops<=minHops || Buffers.count(msg)<minSize)
			relay(out, this.fanout, this.syncport, memb.connections());               			
		else {
			// Cache message
			cache.put(uuid, out);
			purgeCache();
			
			// Send out advertisements
			out = new ByteBuffer[2];
			out[0] = UUIDUtils.writeUUIDToBuffer(uuid);;
			out[1] = ByteBuffer.wrap(new byte[] { hops });
			relay(out, this.fanout, this.ctrlport, memb.connections());               
		}
    }

    private void handleControl(UUID uuid, byte hops, Connection info) {
        if (hops == 0 && msgs.contains(uuid)) {
			// It is a nack and we (still) have it.
			ByteBuffer[] copy = cache.get(uuid);
			copy = Buffers.clone(copy);

			info.send(copy, this.syncport);
		} else if (hops > 0 && !msgs.contains(uuid) && queued.add(uuid)) {
			// It is an advertisement and we don't have it.
			ByteBuffer uuid_bytes = UUIDUtils.writeUUIDToBuffer(uuid);
			ByteBuffer[] out = new ByteBuffer[2];

			out[0] = uuid_bytes;
			out[1] = ByteBuffer.wrap(new byte[] { 0 });
			info.send(out, this.ctrlport);
			
			purgeQueued();
		}
    }

    /**
     * This method sends a copy of the original message to a fanout of peers of
     * the local memberhip.
     * 
     * @param msg
     *            The original message
     * @param fanout
     *            Number of peers to send the copy of the message to.
     * @param syncport
     *            The synchronization port (Gossip or Memberhip) which the
     *            message is to be delivered to.
     * @param conns
     *            Available connections
     */
    private void relay(ByteBuffer[] msg, int fanout, short syncport,
            Connection[] conns) {
        if (conns.length < 1) {
            return;
        }

        if (fanout > conns.length)
            fanout = conns.length;
        
        int index;
        for (int i = 0; i < fanout; i++) {
            /*
             * System.out.println( "Message from " + net.id().toString() + " to : " +
             * info.addr.toString());
             */
            index = rand.nextInt(fanout);
            if (conns[index] != null)
                conns[index].send(Buffers.clone(msg), syncport);
        }
    }
    
    private void purgeMsgs() {
    	Iterator<UUID> i=msgs.iterator();
    	while(i.hasNext() && msgs.size()>maxIds) {
    		i.next();
    		i.remove();
    	}
    }

    private void purgeCache() {
    	Iterator<UUID> i=cache.keySet().iterator();
    	while(i.hasNext() && cache.size()>maxIds) {
    		i.next();
    		i.remove();
    	}
    }

    private void purgeQueued() {
    	Iterator<UUID> i=queued.iterator();
    	while(i.hasNext() && queued.size()>maxIds) {
    		i.next();
    		i.remove();
    	}
    }

    /**
     * Membership management module.
     */
    private MembershipImpl memb;

    /**
     *  Represents the class to which messages must be delivered.
     */
    private App handler;

    /**
     *  Set of received messages identifiers.
     */
    private LinkedHashSet<UUID> msgs;

    /**
     *  The Transport port used by the Gossip class instances to exchange messages. 
     */
    private short syncport, ctrlport;
    
    /**
     *  Map of advertised messages.
     */
    private LinkedHashMap<UUID,ByteBuffer[]> cache;

    /**
     * Pending retransmissions.
     */
    private LinkedHashSet<UUID> queued;

    /**
     * Random number generator for selecting targets.
     */
    private Random rand = new Random();

    // Configuration parameters
    
    /**
     *  Number of peers to relay messages to.
     */
    private int fanout;

    /**
     * Maximum number of stored ids.
     */
    private int maxIds = 100;

    /**
     * Configuration of retransmission policy.
     */
    private int maxHops, minHops, minSize;
    
    public int getFanout() {
        return fanout;
    }

    public void setFanout(int fanout) {
        this.fanout = fanout;
    }

    public int getMaxIds() {
        return maxIds;
    }

    public void setMaxIds(int maxIds) {
        this.maxIds = maxIds;
    }
}

// arch-tag: 4a3a77be-0f72-4416-88ee-c6639fe68e90
