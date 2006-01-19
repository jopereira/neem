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
import java.util.UUID;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
 *  This class implements the Membership interface. Its methods handle events
 * related with changes in local group membership as well as exchanging local
 * group information with its peers.
 *
 * @author psantos@GSD
 */
public class MembershipImpl extends AbstractGossipImpl implements Membership, DataListener, Runnable {
	/**
     *  Creates a new instance of MembershipImpl
     * @param net Instance of Transport that will be used to pass messages between peers
	 * @param port Synchronization port, i.e., the port to wich membership related messages must be sent. Specifies a logic, not socket, port.
	 * @param idport Identification port, i.e., the port to which identification messages must be sent.
	 * @param fanout The number of peers to be warned of local membership changes or local group members addresses.
	 * @param grp_size The maximum number of members on the local group.
     */
    public MembershipImpl(Transport net, short port, short idport, int fanout, int grp_size) {
        this.net = net;
        this.fanout = fanout;
        this.grp_size = grp_size;
        this.syncport = port;
        this.idport = idport;
        this.myId=UUID.randomUUID();
        this.peers = new HashMap<UUID,Transport.Connection>();
        net.handler(this, this.syncport);
        net.handler(this, this.idport);
        net.membership_handler(this);
    }
    
    public void receive(ByteBuffer[] msg, Transport.Connection info, short port) {
        // System.out.println("Membership Receiving Message");
        try {
            UUID id = UUIDUtils.readUUIDFromBuffer(msg);
            InetSocketAddress addr = AddressUtils.readAddressFromBuffer(msg);

            if (port==syncport) {
            	// System.out.println("Receive from "+info.addr+"+ id "+id);
            	if (!peers.containsKey(id) && !id.equals(myId))
            		this.net.add(addr);
            } else if (port==idport) {
                // System.out.println("Discovered that "+info.addr+" is "+id);
            	if (peers.containsKey(id))
            		net.remove(info.addr);
            	else synchronized(this) {
            		peers.put(id, info);
            		info.id=id;
            		info.listen=addr;
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void open(Transport.Connection info) {
        if (this.firsttime) {
            net.schedule(this, this.distConnsPeriod);
            firsttime = false;
        }
        net.send(new ByteBuffer[]{
        		UUIDUtils.writeUUIDToBuffer(myId),
        		AddressUtils.writeAddressToBuffer(net.id())
        	}, info, idport);
        probably_remove();
    }
    
    public synchronized void close(Transport.Connection info) {
        // System.out.println(
        // "CLOSE@" + myId + " : " + addr.toString());
    	if (info.id!=null)
    		peers.remove(info.id);
    }
    
    /**
     *  If the number of connected peers equals maximum group size, must evict an
     * older member, as the new one has already been added to the local membership
     * by the transport layer. First we must create space for the new member by 
     * randomly selectig one of the local members (wich can be the newbie, because
     * it has already been added) then read from socket the address where it's 
     * accepting connections. Then increase the number of local members. If the
     * nb_members (current number of members in local membership) is less than 
     * grp_size (maximum number of members in local membership), this method only
     * increases by one the number of members.
     */
    private void probably_remove() {
        Transport.Connection[] conns = connections();
        int nc = conns.length;

        if (peers.size() >= grp_size) {
            Transport.Connection info = conns[rand.nextInt(nc)];
            info.id=null;
            this.net.remove(info.addr);
        }
    }
    
    public void run() {
    	// System.out.println("--- current peers: "+peers);
        if (peers.size()==0)
        	this.firsttime=true;
        else {
            distributeConnections();
        	net.schedule(this, this.distConnsPeriod);
        }
    }

    /**
     *  Tell a fanout number of members of my local membership, that there is a
     * connection do the peer identified by its address, wich is sent to the 
     * peers.
     */ 
    private void distributeConnections() {
        Transport.Connection[] conns = connections();
       	Transport.Connection info = conns[rand.nextInt(conns.length)];
     
       	if (info.id==null)
       		return;
        
       	// System.out.println("Disseminating "+addr);
        relay(new ByteBuffer[] {
        		UUIDUtils.writeUUIDToBuffer(info.id),
        		AddressUtils.writeAddressToBuffer(info.listen)
        	}, this.fanout, this.syncport, conns);
    }
    
    /**
     * Get all connections.
     */
    public Transport.Connection[] connections() {
    	return peers.values().toArray(new Transport.Connection[peers.size()]);
    }
        
    /**
     * Gets the current fanout size. The fanout is the number of local group
     * members to send a message to.
     * @return The current fanout.
     */
    public int getFanout() {
		return fanout;
	}

    /**
     * Sets the new fanout value.
     * @param fanout The new fanout value
     */
	public void setFanout(int fanout) {
		this.fanout = fanout;
	}

    /**
     * Gets the current maximum size for the local membership.
     * @return The current local membership's maximum size
     */
	public int getGrp_size() {
		return grp_size;
	}

    /**
     * Sets a new value for the maximum size of the local membership.
     * @param grp_size The new maximum membership's size.
     */
	public void setGrp_size(int grp_size) {
		this.grp_size = grp_size;
	}

    /**
     * Gets the current period of the call to distributeConnections
     * @return The current period
     */
    public int getDistConnsPeriod() {
        return distConnsPeriod;
    }

    /**
     * Sets a new period of the call to distributeConnections
     * @param distConnsPeriod the new period
     */
    public void setDistConnsPeriod(int distConnsPeriod) {
        this.distConnsPeriod = distConnsPeriod;
    }

    /**
     * Get all connected peers.
     */
    public synchronized UUID[] getPeers() {
    	return peers.keySet().toArray(new UUID[peers.size()]);
    }
    
    /**
     * The peers variable can be queried by an external thread for JMX
     * management. Therefore, all sections of the code that modify it must
     * be synchronized. Sections that read it from the protocol thread need
     * not be synchronized.
     */
    private Map<UUID,Transport.Connection> peers;
    private short syncport, idport;
    private int fanout, grp_size;
    protected HashSet<UUID> msgs;
    private boolean firsttime = true;
	private UUID myId;
    private int distConnsPeriod = 5000;
}

 
;
// arch-tag: e99e8d36-d4ba-42ad-908a-916aa6c182d9
