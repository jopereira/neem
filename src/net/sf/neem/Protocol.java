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

import net.sf.neem.impl.Gossip;
import net.sf.neem.impl.Overlay;
import net.sf.neem.impl.Transport;

/**
 * Implementation of the NeEM management bean.
 */
public class Protocol implements ProtocolMBean {
	Protocol(MulticastChannel neem) {
		this.neem = neem;
        this.net = neem.trans;
		this.g_impl = (Gossip) neem.gimpls;
		this.m_impl = neem.mimpls;
	}

	// Gossip
	
    public int getFanout() {
        return this.g_impl.getFanout();
    }

    public void setFanout(int fanout) {
        this.g_impl.setFanout(fanout);
    }

    public int getMaxIds() {
        return g_impl.getMaxIds();
    }

    public void setMaxIds(int max) {
        g_impl.setMaxIds(max);
    }
    
    public int getDelivered() {
    	return g_impl.deliv;
    }
    
    public int getMulticast() {
    	return g_impl.mcast;
    }
    
    public int getDataReceived() {
    	return g_impl.dataIn;
    }
    
    public int getDataSent() {
    	return g_impl.dataOut;
    }
    
    public int getHintsReceived() {
    	return g_impl.ackIn;
    }
    
    public int getHintsSent() {
    	return g_impl.ackOut;
    }
    
    public int getPullReceived() {
    	return g_impl.nackIn;
    }
    
    public int getPullSent() {
    	return g_impl.nackOut;
    }

    // --- Overlay

    public UUID getLocalId() {
		return this.m_impl.getId();
	}
    
    public UUID[] getPeerIds() {
		return this.m_impl.getPeers();
	}
	
    public int getMaxPeers() {
        return this.m_impl.getMaxPeers();
    }

    public void setMaxPeers(int groupsize) {
        this.m_impl.setMaxPeers(groupsize);
    }
    
    public int getShufflePeriod() {
        return m_impl.getShufflePeriod();
    }

    public void setShufflePeriod(int period) {
        m_impl.setShufflePeriod(period);
    }

    public int getJoinRequests() {
    	return m_impl.joins;
    }
	
	public int getPurgedConnections() {
		return m_impl.purged;
	}
	
	public int getShufflesReceived() {
		return m_impl.shuffleIn;
	}
	
	public int getShufflesSent() {
		return m_impl.shuffleOut;
	}

	// -- Transport
	
	public InetSocketAddress getLocalAddress() {
		return net.id();
	}
	
    public InetSocketAddress[] getPeerAddresses() {
        return this.m_impl.getPeerAddresses();
    } 

    public synchronized void addPeer(String addr, int port) {
        this.neem.connect(new InetSocketAddress(addr,port));
    }
	
    public int getQueueSize() {
        return net.getQueueSize();
    }

    public void setQueueSize(int size) {
        net.setQueueSize(size);
    }
    
    public int getAcceptedSocks() {
    	return net.accepted;
    }

    public int getConnectedSocks() {
    	return net.connected;
    }
    
    public int getPacketsReceived() {
    	return net.pktIn;
    }
    
    public int getPacketsSent() {
    	return net.pktOut;
    }
    
    public int getBytesReceived() {
    	return net.bytesIn;
    }
    
    public int getBytesSent() {
    	return net.bytesOut;
    }
    
    private MulticastChannel neem;
	private Transport net;
	private Gossip g_impl;
	private Overlay m_impl;
};

// arch-tag: 08505269-5fca-435f-a7ae-8a87af222676 
