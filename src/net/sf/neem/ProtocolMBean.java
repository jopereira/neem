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

package net.sf.neem;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * Interface for a JMX management bean. This allows several protocol parameters
 * to be queried and set, in order to fine tune protocol behavior. Available
 * parameters will change as the protocol evolves, so this interface is far
 * from set in stone. Don't rely on it too much.
 */
public interface ProtocolMBean {
	/**
	 * Get the default size of buffer queues.
	 * @return number of messages
	 */
	public int getQueueSize();
	
	/**
	 * Set the default size of buffer queues. Currently, this does not modify
	 * existing queues, only those that are created thereafter.
	 * @param size number of messages
	 */
	public void setQueueSize(int size);
	
	/**
	 * Get the number of gossip target for each round.
	 * @return number of targets
	 */
	public int getFanout();
	
	/**
	 * Set the number of gossip target for each round.
	 * @param fanout number of targets
	 */
	public void setFanout(int fanout);
	
	/**
	 * Get the number of gossip target for each round.
	 * @return number of targets
	 */
	public int getMembershipFanout();
	
	/**
	 * Set the number of gossip target for each round.
	 * @param fanout number of targets
	 */
	public void setMembershipFanout(int fanout);
	
	/**
	 * Get the maximum number of cached message ids.
	 * @return number of ids
	 */
	public int getMaxIds();
	
	/**
	 * Set the maximum number of cached message ids. Setting this too
	 * low may result in duplicate message deliveries.
	 * @param max number of ids
	 */
	public void setMaxIds(int max);

	/**
	 * Get the delay between periodic membership gossip rounds.
	 * @return period in milliseconds
	 */
	public int getMembershipPeriod();
	
	/**
	 * Set the delay between periodic membership gossip rounds.
	 * @param period in milliseconds
	 */
	public void setMembershipPeriod(int period);

	/**
	 * Get the number of neighbors.
	 * @return number of neighbors
	 */
	public int getGroupSize();
	
	/**
	 * Set the number of neighbors.
	 * @param groupsize number of neighbors
	 */
	public void setGroupSize(int groupsize);
    
	/**
	 * Get list of currently connected peers.
	 * @return connected peers
	 */
    public InetSocketAddress[] getPeers();
    
    /**
     * Connect to a new peer.
     * @param addr hostname or address of peer
     * @param port listening port number
     */
    public void addPeer(String addr, int port);
    
	/**
	 * Get list of currently connected peer ids.
	 * @return connected peer ids
	 */
    public UUID[] getPeersUUIDs();

    /**
	 * Get globally unique local id.
	 * @return local id
	 */
    public UUID getID();
}

// arch-tag: 2c588950-1f71-46ed-be61-f801fb5c90f8
