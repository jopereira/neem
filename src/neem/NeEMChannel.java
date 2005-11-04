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
 * Neem.java
 *
 * Created on July 15, 2005, 3:46 PM
 *
 * @author psantos@lsd.di.uminho.pt
 */

package neem;

import neem.impl.*;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


/**
 * Encapsulates Transport, Gossip and Membership layers into an easy-to-use/instanciate class
 * @author psantos@gsd.di.uminho.pt
 * 
 */
public class NeEMChannel implements InterruptibleChannel, ReadableByteChannel, WritableByteChannel {
    
    /** Creates a new instance of Neem */
    public NeEMChannel(InetSocketAddress local, int fanout, int group_size) throws IOException {
        trans = new Transport(local);
        gimpls = new GossipImpl(trans, (short)0, fanout, group_size);
        mimpls = new MembershipImpl(trans, (short)1, fanout, group_size);
        gimpls.handler(new App() {
            public void deliver(ByteBuffer[] buf, Gossip gimpl) {
                enqueue(buf);
            }
        });
        t = new Thread(trans);
        t.setDaemon(true);
        t.start();
    }

    public synchronized boolean isOpen() {
        return !isClosed;
    }

    public synchronized void close() {
        if (isClosed)
            return;
        isClosed=true;
        notifyAll();
        trans.close();
    }

    public synchronized int write(ByteBuffer msg) throws IOException {
        if (isClosed)
            throw new ClosedChannelException();
        final ByteBuffer cmsg=Buffers.compact(new ByteBuffer[]{msg});
        int ret=cmsg.remaining();
        trans.queue(new Runnable() {
            public void run() {
                gimpls.multicast(new ByteBuffer[]{cmsg});
            }
        });
        return ret;
    }

    public synchronized int read(ByteBuffer msg) throws IOException {
        if (isClosed)
            throw new ClosedChannelException();
        try {
            while(queue.isEmpty() && !isClosed)
                wait();
        } catch(InterruptedException ie) {
            close();
            throw new ClosedByInterruptException();
        }
        if (isClosed)
            throw new AsynchronousCloseException();
        ByteBuffer[] buf=queue.removeFirst();
        return Buffers.copy(msg, buf);
    }

    private synchronized void enqueue(ByteBuffer[] buf) {
        queue.add(buf);
        notify();
    }
    
    /**
     * Get the address that is being advertised to peers.
     */
    public InetSocketAddress getLocalSocketAddress() {
        return this.trans.id();
    }
    
    /**
     * Add an address of a remote peer. This is used to add the address of
     * peers that act as rendezvous points when joining the group. Any peer
     * can be used, as the protocol is fully symmetrical. This can be called
     * a number of times to more quickly build a local membership.
     *
     * @param peer The address of the peer.
     */
    public void connect(final InetSocketAddress peer) {
        trans.queue(new Runnable() {
            public void run() {
                trans.add(peer);
            }
        });
    }
    
    /** Transport layer*/
    private Transport trans = null;

    /** Gossip layer*/
    private GossipImpl gimpls = null;

    /** Membership layer*/
    private Membership mimpls = null;

    private boolean isClosed;

    private LinkedList<ByteBuffer[]> queue=new LinkedList<ByteBuffer[]>();

    private Thread t;
}

 
// arch-tag: cd998499-184b-4c75-a0a0-34180eb3c92c
