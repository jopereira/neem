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

import java.net.InetSocketAddress;

import neem.ProtocolMBean;


public class Protocol implements ProtocolMBean {
	public Protocol(Transport net, GossipImpl gossip, MembershipImpl membership) {
		this.net = net;
		this.g_impl = gossip;
		this.m_impl = membership;
	}
	
	public int getFanout() {
		return this.m_impl.getFanout();
	}
	
	public void setFanout(int fanout) {
		this.m_impl.setFanout(fanout);
	}
	
	public int getGroupSize() {
		return this.m_impl.getGrp_size();
	}
	
	public void setGroupSize(int groupsize) {
		this.m_impl.setGrp_size(groupsize);
	}
    
    public InetSocketAddress[] getPeers() {
        return this.net.getPeers();
    }
	
	@SuppressWarnings("unused")
	private Transport net;
	@SuppressWarnings("unused")
	private GossipImpl g_impl;
	private MembershipImpl m_impl;
}
;

// arch-tag: 08505269-5fca-435f-a7ae-8a87af222676 
