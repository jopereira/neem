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

import net.sf.neem.impl.*;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Channel interface to a NeEM epidemic multicast group. This interface is
 * thread and aliasing safe, and thus can be used by multiple threads and all
 * buffers used as parameters can immediatly be reused by the application.
 * <p>
 * After creating the channel, it must be connected to at least one previously
 * known peer in the desired group. Such peer must be determined by the
 * application. A good strategy is to maintain a list of known peers and connect
 * to them all to cope with transient failures.
 */
public class MulticastChannel implements InterruptibleChannel,
        ReadableByteChannel, WritableByteChannel {

	/**
     * Creates a new instance of a multicast channel.
     * 
     * @param local
     *            the local address to bind to
     */
    public MulticastChannel(InetSocketAddress local) throws IOException {
        trans = new Transport(local);
        mimpls = new Overlay(trans, (short)2, (short)3);
        gimpls = new Gossip(trans, mimpls, (short)0, (short)1);
        gimpls.handler(new Application() {
            public void deliver(ByteBuffer[] buf) {
                enqueue(buf);
            }
        });
        t = new Thread(trans);
        t.setDaemon(true);
        t.start();
    }

    /**
     * True if the channel has not yet been (explicitly or implicitly) closed.
     * 
     * @return false if the channel has been closed
     */
    public synchronized boolean isOpen() {
        return !isClosed;
    }

    /**
     * Close the channel. This will wakeup all threads blocked on
     * {@link #read(ByteBuffer)}.
     */
    public synchronized void close() {
        if (isClosed)
            return;
        isClosed = true;
        notifyAll();
        trans.close();
    }

    /**
     * Send a message to members of the group. On return, there are no remaining
     * bytes in the supplied buffer.
     * 
     * @param msg
     *            bytes to be sent
     * @return the number of bytes written
     * @throws ClosedChannelException
     *             the channel has already been closed
     */
    public synchronized int write(ByteBuffer msg) throws ClosedChannelException {
        if (isClosed)
            throw new ClosedChannelException();
        final ByteBuffer cmsg = Buffers.compact(new ByteBuffer[] { msg });
        if (!loopback)
            enqueue(Buffers.clone(new ByteBuffer[] { cmsg }));
        int ret = cmsg.remaining();
        trans.queue(new Runnable() {
            public void run() {
                gimpls.multicast(new ByteBuffer[] { cmsg });
            }
        });
        return ret;
    }

    /**
     * Receive a message. If the truncate mode is not set (the default), the
     * buffer should be large enough to handle incoming messages. Otherwise, a
     * {@link BufferTooSmallException} will be thrown and the message will be
     * left untouched in the queue. If the truncate mode is set, the remainder
     * of the message will be silently discarded.
     * 
     * @param msg
     *            a byte buffer to be filled with the received message
     * @return the number of bytes read
     * @throws ClosedChannelException
     *             the channel was previously closed
     * @throws ClosedByInterruptException
     *             the waiting thread has been interrupted
     * @throws AsynchronousCloseException
     *             the channel has been closed while waiting
     */
    public synchronized int read(ByteBuffer msg) throws ClosedChannelException,
            ClosedByInterruptException, AsynchronousCloseException {
        if (isClosed)
            throw new ClosedChannelException();
        try {
            while (queue.isEmpty() && !isClosed)
                wait();
        } catch (InterruptedException ie) {
            close();
            throw new ClosedByInterruptException();
        }
        if (isClosed)
            throw new AsynchronousCloseException();
        ByteBuffer[] buf = queue.getFirst();
        if (msg.remaining() < Buffers.count(buf) && !truncate)
            throw new BufferOverflowException();
        buf = queue.removeFirst();
        return Buffers.copy(msg, buf);
    }

    private synchronized void enqueue(ByteBuffer[] buf) {
        queue.add(buf);
        notify();
    }

    /**
     * Get the address that is being advertised to peers.
     * 
     * @return the address being advertised to peers
     */
    public InetSocketAddress getLocalSocketAddress() {
        return this.trans.id();
    }

    /**
     * Add an address of a remote peer. This is used to add the address of peers
     * that act as rendezvous points when joining the group. Any peer can be
     * used, as the protocol is fully symmetrical. This can be called a number
     * of times to more quickly build a local membership.
     * 
     * @param peer
     *            The address of the peer.
     */
    public void connect(final InetSocketAddress peer) {
        trans.queue(new Runnable() {
            public void run() {
                trans.add(peer);
            }
        });
    }

    /*
     * Setting the loopback mode tries to reproduce the same feature on regular
     * java.net.MulticastSocket. Unfortunately (search Java Bug#4686717), Sun
     * has made a mess out of this and their documentation contradicts their
     * implementation as of JDK1.4. *sigh*
     */

    /**
     * Query local loopback mode status. When false, messages multicast locally
     * will be delivered back to the sender.
     * 
     * @return if true then loopback mode is disabled
     */
    public synchronized boolean getLoopbackMode() {
        return loopback;
    }

    /**
     * Disable/enable local loopback mode. When false, messages multicast
     * locally will be delivered back to the sender.
     * 
     * @param mode
     *            true to disable loopback mode
     */
    public synchronized void setLoopbackMode(boolean mode) {
        loopback = mode;
    }

    /**
     * Disable/enable local message truncate mode. When false and the buffer
     * supplied to read is too small, an exception is thrown and the message is
     * left untouched. When true, the remainder of the datagram is discarded.
     * 
     * @param mode
     *            true to enable truncate mode
     */
    public synchronized void setTruncateMode(boolean mode) {
        truncate = mode;
    }

    /**
     * Query local message truncate mode. When false and the buffer supplied to
     * read is too small, an exception is thrown and the message is left
     * untouched. When true, the remainder of the datagram is discarded.
     * 
     * @return true if truncate mode is enabled
     */
    public synchronized boolean getTruncateMode() {
        return truncate;
    }

    /**
     * Obtain a reference to a JMX compliant management bean. This can be used
     * to fine tune several protocol parameters.
     * 
     * @return the management bean
     */
    public ProtocolMBean getProtocolMBean() {
        return new Protocol(this);
    }

    /* Transport layer */
    Transport trans = null;

    /* Gossip layer */
    Gossip gimpls = null;

    /* ConnectionListener layer */
    Overlay mimpls = null;

	private boolean isClosed;

    private boolean loopback, truncate;

    private LinkedList<ByteBuffer[]> queue = new LinkedList<ByteBuffer[]>();

    private Thread t;
}

// arch-tag: cd998499-184b-4c75-a0a0-34180eb3c92c
