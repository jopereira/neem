package neem;

import neem.impl.Gossip;
import neem.impl.Membership;
import neem.impl.Transport;

public class Protocol implements ProtocolMBean {
	public Protocol(Transport net, Gossip gossip, Membership membership) {
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
	
	@SuppressWarnings("unused")
	private Transport net;
	@SuppressWarnings("unused")
	private Gossip g_impl;
	private Membership m_impl;
}
;

// arch-tag: 08505269-5fca-435f-a7ae-8a87af222676 
