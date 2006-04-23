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

public class GossipImpl extends AbstractGossipImpl implements Gossip, DataListener {

    /**
     *  Creates a new instance of GossipImpl.
     */
    public GossipImpl(MembershipImpl memb, short port, int fanout) {
        this.fanout = fanout;
        this.net = memb.net();
        this.memb = memb;
        this.syncport = port;
        //this.maxIds = 100;
        this.msgs = new LinkedHashSet<UUID>();
        this.cache = new LinkedHashMap<Short,ByteBuffer[]>();
        net.handler(this, this.syncport);
    }
    
    public void handler(App handler) {
        this.handler = handler;
    }
        
    public void multicast(ByteBuffer[] msg) {
        // Create uuid && add it to message (another header!!!)
        UUID uuid = UUID.randomUUID();
        ByteBuffer uuid_bytes = UUIDUtils.writeUUIDToBuffer(uuid);

        ByteBuffer[] out = new ByteBuffer[msg.length + 2];

        out[0] = uuid_bytes;
        out[1] = ByteBuffer.wrap(new byte[]{0});

        System.arraycopy(msg, 0, out, 2, msg.length);
                
        // send to a fanout of the groupview members
        
        relay(out, fanout, this.syncport, memb.connections());
        msgs.add(uuid);
        purgeMsgs();
    }
    
    public void receive(ByteBuffer[] msg, Connection info, short port) {
        // Check uuid
        try {
            // System.out.println("Receive@Gossip: " + msg.length);
            UUID uuid = UUIDUtils.readUUIDFromBuffer(msg);
            byte hops = Buffers.sliceCompact(msg, 1).get();

            if (msgs.add(uuid) && hops<5) {
                ByteBuffer[] copy = Buffers.clone(msg);

            	// only pass to application a clean message => NO HEADERS FROM LOWER LAYERS
                this.handler.deliver(msg, this);
            	purgeMsgs();
            	
                ByteBuffer uuid_bytes = UUIDUtils.writeUUIDToBuffer(uuid);

                ByteBuffer[] out = new ByteBuffer[msg.length + 2];

                out[0] = uuid_bytes;
                out[1] = ByteBuffer.wrap(new byte[]{++hops});

                System.arraycopy(copy, 0, out, 2, copy.length);                

                // --- TODO: cache usage
                cache.put((short)(uuid.getLeastSignificantBits()&0xffff), Buffers.clone(out));
                purgeCache();
                // ---
                
                relay(out, this.fanout, this.syncport, memb.connections());
                
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    	Iterator<Short> i=cache.keySet().iterator();
    	while(i.hasNext() && cache.size()>maxIds) {
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
     *  Number of peers to relay messages to.
     */
    private int fanout;

    /**
     *  The Transport port used by the Gossip class instances to exchange messages. 
     */
    private short syncport;
    
    /**
     * Maximum number of stored ids.
     */
    private int maxIds = 100;
    /**
     *  Set of received messages identifiers.
     */
    private LinkedHashMap<Short,ByteBuffer[]> cache;

//Getters and Setters ---------------------------------------------------
    
    public int getFanout() {
        return fanout;
    }

    public void setFanout(int fanout) {
        this.fanout = fanout;
    }

    /**
     * Get the maximum number of message ids to store locally.
     * @return the current maximum
     */
    public int getMaxIds() {
        return maxIds;
    }

    /**
     * Set the maximum number of message ids to store locally.
     * @param maxIds the new maximum
     */
    public void setMaxIds(int maxIds) {
        this.maxIds = maxIds;
    }
}


;

// arch-tag: 4a3a77be-0f72-4416-88ee-c6639fe68e90
