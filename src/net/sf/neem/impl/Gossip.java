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

package net.sf.neem.impl;


import java.nio.*;


/** Methods necessary for implementing this Gossip Multicast Protocol.
 * Classes that intend to implement this Gossip Multicast Protocol MUST implement
 * all methods defined here.
 *
 *@author psantos@GSD
 */
public interface Gossip extends DataListener {

    /**
     *  Sets the application to wich every received 
     * message must be delivered.
     * @param app The application wich is interessed in the messages.
     */
    public void handler(App app);
    
    /**
     *  This method is called by this class's instance handler whenever it wishes 
     * to send a messages using the neem multicast protocol. This method sends a 
     * copy of the message to each registred peer.
     * @param msg Message to be multicasted.
     */
    public void multicast(ByteBuffer[] msg);
}

// arch-tag: 87a87e28-12f1-44ae-a156-6f4f6d5266b6
