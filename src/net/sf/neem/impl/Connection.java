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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Socket manipulation utilities.
 */
public class Connection {
	/**
	 * Create a new connection.
	 * 
	 * @param trans transport object
	 * @param bind local address to bind to, if any
	 * @param conn allow simultaneous outgoing connection
	 * @throws IOException 
	 */
	Connection(Transport trans, InetSocketAddress bind, boolean conn) throws IOException {
    	this.transport = trans;
		if (conn) {
			sock = SocketChannel.open();
			sock.configureBlocking(false);
			if (bind != null) {
				sock.socket().setReuseAddress(true);
			}
			sock.socket().bind(bind);
		}
		if (bind != null) {
			ssock = ServerSocketChannel.open();
			ssock.configureBlocking(false);
			ssock.socket().bind(bind);

			skey = ssock.register(transport.selector,
					SelectionKey.OP_ACCEPT);
			skey.attach(this);
		}
	}
	
	/**
	 * Initiate connect to remote address.
	 * 
	 * @param remote address of target
	 * @throws IOException
	 */
	void connect(InetSocketAddress remote) throws IOException {
        sock.connect(remote);
		key = sock.register(transport.selector,
				SelectionKey.OP_CONNECT);
		key.attach(this);
		msg_q = new Queue(transport);
	}

	/**
	 * Create a new connection with a pre-existing socket.
	 * @param trans
	 * @param sock
	 * @throws IOException
	 */
	Connection(Transport trans, SocketChannel sock) throws IOException {
    	this.transport = trans;
    	this.sock = sock;
		sock.configureBlocking(false);
		sock.socket().setSendBufferSize(1024);
		sock.socket().setReceiveBufferSize(1024);
		key = sock.register(transport.selector,
				SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		key.attach(this);
		msg_q = new Queue(transport);
	}

    /*public String toString() {
        return "Connection to " + addr;
    }*/
    
    private Transport transport;
    private SocketChannel sock;
    private SelectionKey key;

    private ByteBuffer incoming, copy;
    private ArrayList<ByteBuffer> incomingmb;
    private int msgsize;

    private ByteBuffer[] outgoing;
    private int outremaining;
    private short port;
 
    private boolean dirty, connected;

    /**
	 * Socket used to listen for connections
	 */
    private ServerSocketChannel ssock;
    private SelectionKey skey;

    /** Message queue
     */
    public Queue msg_q;

    /**
     * Used by membership management to assign an unique id to the
     * remote process. Currently, this is an address.
     */
	public UUID id;
	
	/**
	 * Used by membership management to keep the socket where
	 * this peer can be contacted.
	 */
	public InetSocketAddress listen;
	
    /**
     * Send message to 1 (one) peer identified by @arg: info.
     */
    public void send(ByteBuffer[] msg, short port) {
        // Header order:
        /* --------
         *|msg size| <- from here
         * --------
         *|  uuid  | <- from DataListener
         * --------
         *|  msg   | <- from App
         * --------
         */
    	if (!key.isValid())
    		return;
    	
        Bucket b = new Bucket(Buffers.clone(msg), (int)port);
        
        msg_q.push((Object) b);
        handleWrite();
    }

    public void close() {
    	handleClose();
    }

    // ////// Event handlers
    
	void handleGC() {
		if (!dirty && outgoing == null) {
		    handleClose();
		} else {
		    dirty = false;
		}
	}
    
    /** Write event handler.
     * There's something waiting to be written.
     */
    void handleWrite() {
        if (msg_q.isEmpty() && outgoing == null) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        try {
            if (outgoing == null) {
                Bucket b = (Bucket) msg_q.pop();
                Integer portI = b.getPort();
                
                ByteBuffer[] msg = b.getMsg();

                if (msg == null || portI == null) {
                    return;
                }
                short port = portI.shortValue();
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

                outremaining -= n;
                if (outremaining == 0) {
                    outgoing = null;
                }
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }            
        } catch (IOException e) {
            handleClose();
            return;
        } catch (CancelledKeyException cke) {
            transport.notifyClose(this);
        }
        
    }

    void handleRead() {
        // New buffer?
        if (incoming == null || incoming.remaining() == 0) {
            
            incoming = ByteBuffer.allocate(1024);
            copy = incoming.asReadOnlyBuffer();
        }
        // Read as much as we can with a single buffer.            
        try {
            long read = 0;

            while ((read = sock.read(incoming)) > 0) {
            	;
            }
            if (read < 0) {
                handleClose();
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
                    // System.out.println("Port: " + port);
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

            try {
                slice.limit(slicesize);
                copy.position(copy.position() + slicesize);
            } catch (Exception e) {
                System.out.println("GOTCHA"); // if anything happens here we want to know about it, but drop & go
            }

            // Is it a new message?
            if (incomingmb == null) {
                incomingmb = new ArrayList<ByteBuffer>();
            }

            incomingmb.add(slice);
            msgsize -= slicesize;
            final Short prt = new Short(port);

            // Is the message complete?
            if (msgsize == 0) {
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
            ByteBuffer compacted = ByteBuffer.allocate(1024);

            while (copy.hasRemaining()) {
                compacted.put(copy.get());
            }
            incoming = compacted;
            copy = incoming.asReadOnlyBuffer();
            copy.limit(incoming.position());
        }
        
    }

    /** Open connection event hadler.
     * When the hanlder behaves as server.
     */
    void handleAccept() throws IOException {
                
        SocketChannel nsock = ssock.accept();
       
        if (nsock == null) {
            return;
        }
        
        try {
            transport.deliverSocket(nsock);
        } catch (IOException e) {// Just drop it.
        }
    
    }

    /**
     * Open connection event hadler.
     * When the hanlder behaves as client.
     */
    void handleConnect() throws IOException {
        try {
            if (sock.finishConnect()) {
            	connected=true;
                sock.socket().setReceiveBufferSize(1024);
                sock.socket().setSendBufferSize(1024);

                transport.notifyOpen(this);
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                return;
            }
        } catch (Exception e) {// Fall through, see below:
        } 
        handleClose();
    }

    /**
     * Closed connection event handler.
     * Either by membership or death of peer.
     */
    void handleClose() {
    	if (key!=null) {
    		try {
    			connected=false;
				key.channel().close();
				key.cancel();
				sock.close();
			} catch (IOException e) {
				// Don't care, we're cleaning up anyway...
			}
			key = null;
			sock = null;
			transport.notifyClose(this);
    	}
    	if (skey!=null) {
            try {
                skey.channel().close();
                skey.cancel();        
                ssock.close();
            } catch (IOException e) {
            	// Don't care, we're cleaning up anyway...
            }
    	}
    }

	public InetSocketAddress getPeer() {
		if (connected)
			return (InetSocketAddress) sock.socket().getRemoteSocketAddress();
		return null;
	}

	public InetSocketAddress getLocal() {
		if (ssock!=null)
			return (InetSocketAddress) ssock.socket().getLocalSocketAddress();
		return null;
	}
}

// arch-tag: 31ba16d7-de61-4cce-98e4-26c590632002