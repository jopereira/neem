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

/*
 * AbstractGossipImpl.java
 *
 * Created on April 11, 2005, 4:21 PM
 */

package neem;


import java.io.*;
import java.lang.*;
import java.nio.*;
import java.net.*;
import java.util.*;


/**
 *  This class provides a method to relay a message represented as an array of 
 * ByteBuffers to a fanout number of group members.
 *
 * @author psantos@GSD
 */
public abstract class AbstractGossipImpl {

    /**
     *  This method sends a copy of the original message to a fanout of peers of the local memberhip.
     * @param msg The original message
     * @param fanout Number of peers to send the copy of the message to.
     * @param syncport The synchronization port (Gossip or Memberhip) which the message is to be delivered to. 
     */
    public void relay(ByteBuffer[] msg, int fanout, short syncport) {
        // System.out.println("Relaying message");
        Transport.Connection info;
        Transport.Connection[] conns = net.connections();

        if (conns.length < 1) {
            return;
        }

        for (int i = 0; i < fanout; i++) {
            int index = rand.nextInt(conns.length);
            
            if (conns[index].key.isValid()) {
                info = conns[index];

                /* System.out.println(
                 "Message from " + net.id().toString() + " to : "
                 + info.addr.toString());*/
                net.send(msg, info, syncport);
            }
            
        }
    }

    public Transport net() {
        return this.net;
    }
    
    /**
     *  Transport instance through wich the message will be sent. 
     * It's a reference to the invoking class' transport layer instance.
     */
    protected Transport net;

    /**
     * Random number generator for selecting targets.
     */
    protected Random rand = new Random();
}


;

// arch-tag: 300b42e8-72a0-4788-b298-a9c377ec4d05
