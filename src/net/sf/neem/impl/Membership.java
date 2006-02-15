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
 * Membership.java
 *
 * Created on March 17, 2005, 4:07 PM
 */
package net.sf.neem.impl;

/**
 *  This interface defines the methods to handle events related with changes in 
 * local group. Events about new connections, closing of open connections 
 * and selection of peers for fanout from the members of the group must be
 * handled by these methods.
 *
 * @author psantos@GSD
 */
public interface Membership {
    
    /**
     *  This method is called from Transport whenever a member joins the group.
     * When called, if it's the first time it's called starts 
     * periodically telling our peers of our open 
     * connections. Then it'll randomly select a peer to be evicted from our local 
     * membership. If it's not the first time this method is called, only the 2nd 
     * step will be executed.
     * @param info The connection to the new peer.
     */
    public void open(Connection info); // event

    /**
     *  This method is called from Transport whenever a member leaves the group.
     * When called, decreases the number of connected members by one, as the connection
     * to the now disconnected peer has already been removed at the transport layer.
     * @param info The recently closed connection.
     */
    public void close(Connection info); // event
    
    /**
     * Gets the current fanout size. The fanout is the number of local group
     * members to send a message to.
     * @return The current fanout.
     */
    public int getFanout();

    /**
     * Sets the new fanout value.
     * @param fanout The new fanout value
     */
	public void setFanout(int fanout);

	/**
	 * Gets the current maximum size for the local membership.
	 * @return The current local membership's maximum size
	 */
	public int getGrp_size();

	/**
	 * Sets a new value for the maximum size of the local membership.
	 * @param grp_size The new maximum membership's size.
	 */
	public void setGrp_size(int grp_size);
}


; // arch-tag: ffede092-c2f3-43d3-a370-e70051be1ede
