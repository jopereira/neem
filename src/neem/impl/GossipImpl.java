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

package neem.impl;


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
        this.maxIds = 100;
        this.msgs = new LinkedHashSet<UUID>();
        net.handler(this, this.syncport);
    }
    
    public void handler(App handler) {
        this.handler = handler;
    }
        
    public void multicast(ByteBuffer[] msg) {
        // Create uuid && add it to message (another header!!!)
        UUID uuid = UUID.randomUUID();
        ByteBuffer uuid_bytes = UUIDUtils.writeUUIDToBuffer(uuid);

        ByteBuffer[] out = new ByteBuffer[msg.length + 1];

        out[0] = uuid_bytes;

        System.arraycopy(msg, 0, out, 1, msg.length);
                
        // send to a fanout of the groupview members
        
        relay(out.clone(), fanout, this.syncport, memb.connections());
        msgs.add(uuid);
        purgeMsgs();
    }
    
    public void receive(ByteBuffer[] msg, Transport.Connection info, short port) {
        // Check uuid
        try {
            // System.out.println("Receive@Gossip: " + msg.length);
            ByteBuffer[] in = Buffers.clone(msg);
            ByteBuffer[] out = Buffers.clone(msg);
            
            UUID uuid = UUIDUtils.readAddressFromBuffer(in);

            if (msgs.add(uuid)) {
            	purgeMsgs();
                // only pass to application a clean message => NO HEADERS FROM LOWER LAYERS
                this.handler.deliver(in, this);
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
    private short syncport = 1;
    
    /**
     * Maximum number of stored ids.
     */
    private int maxIds;
}


;

// arch-tag: 4a3a77be-0f72-4416-88ee-c6639fe68e90
