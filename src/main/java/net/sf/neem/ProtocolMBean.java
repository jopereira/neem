/*
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2007, University of Minho
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
 * Interface for a JMX management bean. This allows several protocol
 * parameters to be queried and set, in order to fine tune protocol
 * behavior. Available parameters will change as the protocol evolves,
 * so this interface is far from set in stone. Don't rely on it too much.
 */
public interface ProtocolMBean {
	// --- Gossip
	
    /**
	 * Get the number of gossip target for each round.
	 * @return number of targets
	 */
	public int getGossipFanout();
	
	/**
	 * Set the number of gossip target for each round.
	 * @param fanout number of targets
	 */
	public void setGossipFanout(int fanout);
	
	/**
	 * Get the maximum number of times that a message is relayed.
	 * @return number of hops
	 */
	public int getTimeToLive();

	/**
	 * Set the maximum number of times that a message is relayed.
	 * @param ttl number of hops
	 */
	public void setTimeToLive(int ttl);
	
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
	 * Get the maximum number of times that a message is pushed.
	 * @return number of hops
	 */
	public int getPushTimeToLive();

	/**
	 * Set the maximum number of times that a message is pushed.
	 * 0 disables pushing. A large value (at least, larger than
	 * time-to-live) disables pulling.
	 * @param pushttl number of hops
	 */
	public void setPushTimeToLive(int pushttl);

	/**
	 * Get the minimum size of messages that can be pulled.
	 * @return size in bytes
	 */
	public int getMinPullSize();

	/**
	 * Set the minimum size of messages that can be pulled. Smaller
	 * messages are always pushed. Setting it to a large value disables
	 * pulling.
	 * @param minPullSize size in bytes
	 */
	public void setMinPullSize(int minPullSize);

	/**
	 * Get period for retrying to pull known messages.
	 * @return period in milliseconds
	 */
	public int getPullPeriod();

	/**
	 * Set period for retrying to pull known messages.
	 * @param pullPeriod period in milliseconds
	 */
	public void setPullPeriod(int pullPeriod);

	/**
	 * Get number of messages delivered to the application.
	 */
	public int getDelivered();
    
	/**
	 * Get number of messages multicast locally by the application.
	 */
    public int getMulticast();
    
    /**
     * Get number of data packets received.
     */
    public int getDataReceived();
    
    /**
     * Get number of data packets transmitted.
     */
    public int getDataSent();
    
    /**
     * Get number of packet hints received.
     */
    public int getHintsReceived();
    
    /**
     * Get number of packet hints transmitted.
     */
    public int getHintsSent();
    
    /**
     * Get number of pull request transmitted.
     */
    public int getPullReceived();
    
    /**
     * Get number of pull requests transmitted.
     */
    public int getPullSent();

	// --- Overlay parameters

	/**
	 * Get globally unique local id.
	 * @return local id
	 */
    public UUID getLocalId();
	
	/**
	 * Get list of currently connected peer ids.
	 * @return connected peer ids
	 */
    public UUID[] getPeerIds();
    
    /**
     * Get the address being advertised to peers.
     * @return the address
     */
    public InetSocketAddress getPublicAddress();

	/**
	 * Get the delay between periodic shuffle.
	 * @return period in milliseconds
	 */
	public int getShufflePeriod();
	
	/**
	 * Set the delay between periodic shuffle.
	 * @param period in milliseconds
	 */
	public void setShufflePeriod(int period);

	/**
	 * Get the number of neighbors.
	 * @return number of neighbors
	 */
	public int getOverlayFanout();
	
	/**
	 * Set the number of neighbors.
	 * @param fanout number of neighbors
	 */
	public void setOverlayFanout(int fanout);
    
	/**
	 * Get number of direct join requests received.
	 */
	public int getJoinRequests();
	
	/**
	 * Get number of connections purged after overflowing
	 * local neighborhood.
	 */
	public int getPurgedConnections();
	
	/**
	 * Get number of shuffle requests received.
	 */
	public int getShufflesReceived();
	
	/**
	 * Get number of shuffle requests transmitted.
	 */
	public int getShufflesSent();
	
	// --- Transport
	
	/**
     * Local listening socket.
	 */
    public InetSocketAddress getLocalAddress();
    
	/**
	 * Get list of currently connected peers.
	 * @return connected peers
	 */
    public InetSocketAddress[] getPeerAddresses();

    /**
     * Connect to a new peer.
     * @param addr hostname or address of peer
     * @param port listening port number
     */
    public void addPeer(String addr, int port);
    
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
	 * Get the default size of socket buffers.
	 * @return size in bytes
	 */
	public int getBufferSize();
	
	/**
	 * Set the default size of socket buffers.
	 * @param size size in bytes
	 */
	public void setBufferSize(int size);
	
	/**
	 * Get number of socket connections accepted.
	 */
    public int getAcceptedSocks();

    /**
     * Get number of successful sockets connected.
     */
    public int getConnectedSocks();
    
    /**
     * Get number of packets received.
     */
    public int getPacketsReceived();
    
    /**
     * Get number of packets transmited.
     */
    public int getPacketsSent();
    
    /**
     * Get number of raw bytes received.
     */
    public int getBytesReceived();
    
    /**
     * Get number of raw bytes transmitted.
     */
    public int getBytesSent();
    
    // --- Global
    
    /**
     * Resets all counters.
     */
    public void resetCounters();
}

