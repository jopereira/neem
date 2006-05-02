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
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;

/**
 * This class implements SCAMP, a protocol to join an existing
 * random network. It is designed to run once while the network
 * is booted.
 */
public class JoinImpl implements DataListener, Membership {
    private Transport net;
	private MembershipImpl memb;

	public JoinImpl(Transport net, MembershipImpl memb, short syncport) {
        this.net = net;
        this.memb = memb;
        this.syncport = syncport;
        this.constant = (byte)3;
        net.handler(this, this.syncport);
        net.membership_handler(this);
    }

    public void receive(ByteBuffer[] msg, Connection info, short port) {
        try {
            UUID id = UUIDUtils.readUUIDFromBuffer(msg);
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(msg);
            byte c = Buffers.sliceCompact(msg, 1).get();
            
            ByteBuffer[] beacon = new ByteBuffer[] {
					UUIDUtils.writeUUIDToBuffer(id),
					AddressUtils.writeAddressToBuffer(addr),
					ByteBuffer.wrap(new byte[] { 0 })
				};
        	Connection[] peers=memb.connections();
            
            if (c==0) {
            	// If already known, drop it.
            	if (memb.getPeer(id)!=null)
            		return;
            	
            	// Flip a coin...
            	if (rand.nextFloat()>0.5)
            		net.add(addr);
            	else {
            		int idx=rand.nextInt(peers.length);
        			peers[idx].send(Buffers.clone(beacon), this.syncport);
            	}
            } else {
            	// Joining! Forward as many as desired.
            	for(int i=0;i<peers.length;i++)
            		peers[i].send(Buffers.clone(beacon), this.syncport);
            	for(int i=0;i<c && peers.length>0;i++) {
            		int idx=rand.nextInt(peers.length);
            		peers[idx].send(Buffers.clone(beacon), this.syncport);
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private short syncport;

    private byte constant;
    
    private Random rand = new Random();

	public void open(Connection info) {
		System.out.println("Cheguei aqui!");
        ByteBuffer[] beacon=new ByteBuffer[] {
            	UUIDUtils.writeUUIDToBuffer(memb.getId()),
            	AddressUtils.writeAddressToBuffer(net.id()),
            	ByteBuffer.wrap(new byte[]{constant})
           	};
        info.send(beacon, this.syncport);
		net.membership_handler(memb);
		memb.open(info);
	}

	public void close(Connection info) {
		// does not happen.
	}
};

// arch-tag: 4a08030d-2ab7-4e8c-ab7b-a61ba31e0ad5
