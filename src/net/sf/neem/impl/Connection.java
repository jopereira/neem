/*	
 * NeEM - Network-friendly Epidemic Multicast
 * Copyright (c) 2005-2007, University of Minho
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Connection with a peer. This class provides event handlers
 * for a connection. It also implements multiplexing and queuing.
 */
public class Connection extends Handler {
	/**
	 * Create a new connection.
	 * 
	 * @param net transport object
	 * @param bind local address to bind to, if any
	 * @param remote target address
	 * @throws IOException 
	 */
	Connection(Transport trans, InetSocketAddress bind, InetSocketAddress remote) throws IOException {
    	super(trans);
		sock = SocketChannel.open();
		sock.configureBlocking(false);
		if (bind != null) {
			sock.socket().bind(bind);
		}
        sock.connect(remote);

        key = sock.register(transport.selector, SelectionKey.OP_CONNECT);
		key.attach(this);
		queue = new Queue(transport.getQueueSize(), transport.rand);
		
		if (logger.isLoggable(Level.INFO))
			logger.log(Level.INFO, "opening connection to "+remote);
	}
    
    /**
     * Create a new connection from an existing socket (used to associate a Connection to 
     * an incoming connection request).
     * @param net Transport layer instance that received the connect request
     * @param sock The accepting socket.
     * @throws IOException If an I/O operation did not succeed.
     */
    Connection(Transport trans, SocketChannel sock) throws IOException {
        super(trans);
        this.sock = sock;
        sock.configureBlocking(false);
        sock.socket().setSendBufferSize(transport.getBufferSize());
        sock.socket().setReceiveBufferSize(transport.getBufferSize());
        
        key = sock.register(transport.selector,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE); 
        key.attach(this);
        queue = new Queue(transport.getQueueSize(), transport.rand);
        connected=true;

        if (logger.isLoggable(Level.INFO))
        	logger.log(Level.INFO, "accepted connection from "+getPeer());
    }
    	
    /**
     * Send message to peers
     * @param msg The message to be sent.
     * @param port Port, at transport layer, where the message must be delivered.
     */
    public void send(ByteBuffer[] msg, short port) {
    	if (key==null)
            return;
    	
        Queued b = new Queued(msg, port);
        queue.push(b);
        handleWrite();
    }

    // --- Event handlers
    
	void handleGC() {
		if (!dirty && outgoing == null) {
		    handleClose();
		} else {
		    dirty = false;
		}
	}
    
    /**
     * Write event handler.
     * There's something waiting to be written.
     */
    void handleWrite() {
        if (queue.isEmpty() && outgoing == null) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        try {
            if (outgoing == null) {
                Queued b = (Queued) queue.pop();
                
                ByteBuffer[] msg = b.getMsg();

                if (msg == null) {
                    return;
                }
                short port = b.getPort();
                int size = 0;

                for (int i = 0; i < msg.length; i++) {
                    size += msg[i].remaining();
                }

                ByteBuffer header = ByteBuffer.allocate(6);

                header.putInt(size);
                header.putShort(port);
                header.flip();
                outgoing = new ByteBuffer[msg.length + 1];
                outgoing[0] = header;
                System.arraycopy(msg, 0, outgoing, 1, msg.length);
                outremaining = size + 6;
            }
            
            if (outgoing != null) {
                long n = sock.write(outgoing, 0, outgoing.length);
                dirty=true;
                transport.bytesOut+=n;

                outremaining -= n;
                if (outremaining == 0) {
                	transport.pktOut++;
                    outgoing = null;
                }
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }            
        } catch (IOException e) {
            handleClose();
        } catch (CancelledKeyException cke) {
        	// Make sure connection is closed
            transport.notifyClose(this);
        }
        
    }

    void handleRead() {
        // New buffer?
        if (incoming == null || incoming.remaining() == 0) {
            incoming = ByteBuffer.allocate(transport.getBufferSize());
            copy = incoming.asReadOnlyBuffer();
        }
        // Read as much as we can with a single buffer.            
        try {
            long read = 0;

            while ((read = sock.read(incoming)) > 0) {
            	transport.bytesIn+=read;
            }
            if (read < 0) {
                handleClose();
                return;
            }
            dirty=true;
            copy.limit(incoming.position());
        } catch (IOException e) {
            handleClose();
            return;
        }
        int number = 0;

        while (copy.hasRemaining()) {
            // Are we starting with a new message?
            if (msgsize == 0) {
                // Read header, if enough bytes are available.
                // See below what happens when the current buffer
                // is too full to contain a header.
                if (copy.remaining() >= 6) {
                    msgsize = copy.getInt();
                    port = copy.getShort();
                } else {
                    break;
                }
            }
            if (incomingmb == null && msgsize == 0) {
                break;
            }
            // Now we can read a message
            int slicesize = msgsize;

            if (msgsize > copy.remaining()) {
                slicesize = copy.remaining();
            }
            ByteBuffer slice = copy.slice();

            slice.limit(slicesize);
            copy.position(copy.position() + slicesize);

            // Is it a new message?
            if (incomingmb == null) {
                incomingmb = new ArrayList<ByteBuffer>();
            }

            incomingmb.add(slice);
            msgsize -= slicesize;
            final Short prt = port;

            // Is the message complete?
            if (msgsize == 0) {
            	transport.pktIn++;
                final ByteBuffer[] msg = (ByteBuffer[]) incomingmb.toArray(
                        new ByteBuffer[incomingmb.size()]);
                transport.deliver(this, prt, msg);
                incomingmb = null;
            }
            number++;
        }

        // Avoid a fragmented header. If/when more data is
        // available select will call us back.
        if (incoming.remaining() + copy.remaining() < 6) {
            ByteBuffer compacted = ByteBuffer.allocate(transport.getBufferSize());

            while (copy.hasRemaining()) {
                compacted.put(copy.get());
            }
            incoming = compacted;
            copy = incoming.asReadOnlyBuffer();
            copy.flip();
        }
        
    }

    /**
     * Open connection event handler.
     * When the handler behaves as server.
     */
    void handleAccept() throws IOException {
    }

    /**
     * Open connection event handler.
     * When the handler behaves as client.
     */
    void handleConnect() {
        try {	
            if (sock.finishConnect()) {
            	transport.connected++;
            	
            	connected=true;
                sock.socket().setReceiveBufferSize(transport.getBufferSize());
                sock.socket().setSendBufferSize(transport.getBufferSize());

                transport.notifyOpen(this);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

                if (logger.isLoggable(Level.INFO))
        			logger.log(Level.INFO, "connected to "+getPeer());
            }
        } catch (IOException e) {
        	logger.log(Level.WARNING, "connect failed", e);
            handleClose();
        } 
    }

    /**
     * Closed connection event handler.
     * Either by overlay management or death of peer.
     */
    void handleClose() {
    	if (key!=null) {
    		if (logger.isLoggable(Level.INFO))
    			logger.log(Level.INFO, "closed connection with "+getPeer());

    		try {
    			connected=false;
				key.channel().close();
				key.cancel();
				sock.close();
			} catch (IOException e) {
				// Don't care, we're cleaning up anyway...
		       	logger.log(Level.WARNING, "cleanup failed", e);
		    }
			key = null;
			sock = null;
			transport.notifyClose(this);
    	}
    }

	public InetSocketAddress getPeer() {
		if (connected)
			return (InetSocketAddress) sock.socket().getRemoteSocketAddress();
		return null;
	}

    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress) this.sock.socket().getRemoteSocketAddress()).getAddress();
    }
    
    protected SocketChannel sock;

    private ByteBuffer incoming, copy;
    private ArrayList<ByteBuffer> incomingmb;
    private int msgsize;

    private ByteBuffer[] outgoing;
    private int outremaining;
    private short port;
 
    private boolean dirty, connected;

    /**
     * Message queue
     */
    public Queue queue;

    /**
     * Used by overlay management to assign an unique id to the
     * remote process.
     */
    public UUID id;
    
    /**
     * Used by overlay management to keep the socket where
     * this peer can be contacted.
     */
    public InetSocketAddress listen;
}

