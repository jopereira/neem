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

//import net.sf.neem.impl.Connection;
import net.sf.neem.impl.GossipImpl;
import net.sf.neem.impl.MembershipImpl;
import net.sf.neem.impl.Transport;

/**
 * Implementation of the NeEM management bean.
 */
public class Protocol implements ProtocolMBean {
	Protocol(MulticastChannel neem) {
		this.neem = neem;
        this.net = neem.trans;
		this.g_impl = (GossipImpl) neem.gimpls;
		this.m_impl = neem.mimpls;
	}
	
    public int getQueueSize() {
        return net.getDefault_Q_size();
    }

    public void setQueueSize(int size) {
        net.setDefault_Q_size(size);
    }

    public int getFanout() {
        return this.g_impl.getFanout();
    }

    public void setFanout(int fanout) {
        this.g_impl.setFanout(fanout);
    }

    /*public int getMembershipFanout() {
        return this.m_impl.getFanout();
    }

    public void setMembershipFanout(int fanout) {
        this.m_impl.setFanout(fanout);
    }*/

    public int getMaxIds() {
        return g_impl.getMaxIds();
    }

    public void setMaxIds(int max) {
        g_impl.setMaxIds(max);
    }

    public int getGroupSize() {
        return this.m_impl.getGrp_size();
    }

    public void setGroupSize(int groupsize) {
        this.m_impl.setGrp_size(groupsize);
    }
    
    public int getMembershipPeriod() {
        return m_impl.getDistConnsPeriod();
    }

    public void setMembershipPeriod(int period) {
        m_impl.setDistConnsPeriod(period);
    }

    public InetSocketAddress[] getPeers() {
        return this.net.getPeers();
    } 
    
    public UUID[] getPeersUUIDs() {
		return this.m_impl.getPeers();
	}
	
	public UUID getID() {
		return this.m_impl.getId();
	}
    
    public synchronized void addPeer(String addr, int port) {
        this.neem.connect(new InetSocketAddress(addr,port));
    }
    
    private MulticastChannel neem;
	private Transport net;
	private GossipImpl g_impl;
	private MembershipImpl m_impl;
};

// arch-tag: 08505269-5fca-435f-a7ae-8a87af222676 
