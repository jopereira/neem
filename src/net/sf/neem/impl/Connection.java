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
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Socket manipulation utilities.
 */
public class Connection {
    private Transport transport;

	Connection(Transport trans, InetSocketAddress addr, SelectionKey key, ByteBuffer[] outgoing, int q_size) {
    	this.transport = trans;
        this.msg_q = new Queue(q_size);
        this.q_size = q_size;
        this.addr = addr;
        this.key = key;
        this.outgoing = outgoing;
        this.sock = (SocketChannel) key.channel();
    }

    Connection(SelectionKey key, int q_size) {
        this.msg_q = new Queue(q_size);
        this.key = key;
        this.sock = (SocketChannel) key.channel();
    }

    public int hashCode() {
        return addr.hashCode();
    }

    public boolean equals(Object other) {
        return (other instanceof Connection)
                && addr.equals(((Connection) other).addr);
    }

    public String toString() {
        return "Connection to " + addr;
    }
    
    InetSocketAddress addr;
    SocketChannel sock;
    SelectionKey key;
    ByteBuffer incoming, copy;
    ArrayList<ByteBuffer> incomingmb;
    int msgsize;
    ByteBuffer[] outgoing;
    int outremaining;
    boolean writable, dirty;
    short port;

    /** Message queue
     */
    public Queue msg_q;
    int q_size;

	public int getQ_size() {
		return q_size;
	}

	public void setQ_size(int q_size) {
		this.q_size = q_size;
	}

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
	
    // ////// Event handlers
    
    /** Write event handler.
     * There's something waiting to be written.
     */
    void handleWrite() {
        final Connection info = this;
        
        if (info.msg_q.isEmpty() && info.outgoing == null) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        try {
            if (info.outgoing == null) {
                Bucket b = (Bucket) info.msg_q.pop();
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
                info.outgoing = new ByteBuffer[msg.length + 1];
                info.outgoing[0] = header;
                System.arraycopy(msg, 0, info.outgoing, 1, msg.length);
                info.outremaining = size + 6;
            }
            
            if (info.outgoing != null) {
                long n = info.sock.write(info.outgoing, 0, info.outgoing.length);

                info.outremaining -= n;
                if (info.outremaining == 0) {
                    info.writable = true;
                    info.outgoing = null;
                }
                key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
            }            
        } catch (IOException e) {
            handleClose();
            return;
        } catch (CancelledKeyException cke) {
            transport.membership_handler.close(info);
        }
        
    }

    void handleRead() {
        final Connection info = this;
        // New buffer?
        if (info.incoming == null || info.incoming.remaining() == 0) {
            
            info.incoming = ByteBuffer.allocate(1024);
            info.copy = info.incoming.asReadOnlyBuffer();
        }
        // Read as much as we can with a single buffer.            
        try {
            long read = 0;

            while ((read = info.sock.read(info.incoming)) > 0) {
            	;
            }
            if (read < 0) {
                handleClose();
            }
            info.copy.limit(info.incoming.position());
        } catch (IOException e) {
            handleClose();
            return;
        }
        int number = 0;

        while (info.copy.hasRemaining()) {
            // Are we starting with a new message?
            if (info.msgsize == 0) {
                // Read header, if enough bytes are available.
                // See below what happens when the current buffer
                // is too full to contain a header.
                if (info.copy.remaining() >= 6) {
                    info.msgsize = info.copy.getInt();
                    info.port = info.copy.getShort();
                    // System.out.println("Port: " + info.port);
                } else {
                    break;
                }
            }
            if (info.incomingmb == null && info.msgsize == 0) {
                break;
            }
            // Now we can read a message
            int slicesize = info.msgsize;

            if (info.msgsize > info.copy.remaining()) {
                slicesize = info.copy.remaining();
            }
            ByteBuffer slice = info.copy.slice();

            try {
                slice.limit(slicesize);
                info.copy.position(info.copy.position() + slicesize);
            } catch (Exception e) {
                System.out.println("GOTCHA"); // if anything happens here we want to know about it, but drop & go
            }

            // Is it a new message?
            if (info.incomingmb == null) {
                info.incomingmb = new ArrayList<ByteBuffer>();
            }

            info.incomingmb.add(slice);
            info.msgsize -= slicesize;
            final Short prt = new Short(info.port);

            // Is the message complete?
            if (info.msgsize == 0) {
                final ByteBuffer[] msg = (ByteBuffer[]) info.incomingmb.toArray(
                        new ByteBuffer[info.incomingmb.size()]);
                final DataListener handler = transport.handlers.get(prt);

                transport.queue(
                        new Runnable() {
                    public void run() {
                        try {
                            handler.receive(msg, info, prt);    
                        } catch (NullPointerException npe) {
                            // npe.printStackTrace();
                            System.out.println(
                                    "DataListener@port not found: "
                                            + prt.shortValue()); // there wasn't a gossip layer registered here at that port
                        }
                    }
                });
                info.incomingmb = null;
            }
            number++;
        }

        // Avoid a fragmented header. If/when more data is
        // available select will call us back.
        if (info.incoming.remaining() + info.copy.remaining() < 6) {
            ByteBuffer compacted = ByteBuffer.allocate(1024);

            while (info.copy.hasRemaining()) {
                compacted.put(info.copy.get());
            }
            info.incoming = compacted;
            info.copy = info.incoming.asReadOnlyBuffer();
            info.copy.limit(info.incoming.position());
        }
        
    }

    /** Open connection event hadler.
     * When the hanlder behaves as client.
     */
    void handleConnect() throws IOException {
        
        final Connection info = this;
        
        try {
            if (info.sock.finishConnect()) {
                info.sock.socket().setReceiveBufferSize(1024);
                info.sock.socket().setSendBufferSize(1024);

                Connection other;
                synchronized(this) {
                	other = transport.connections.put(info.addr, info);
                }

                if (other == null) {
                    transport.queue(new Runnable() {
                        public void run() {
                            transport.membership_handler.open(info);
                        }
                    });
                    key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                } else {
                    other.sock.close();
                    other.key.channel().close();
                    other.key.cancel();        
                    
                    // membership_handler.open(info, 2); not done here because we're just replacing the connection, it's not a new one
                }
                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                return;
            }
        } catch (Exception e) {// Fall through, see below:
        } 
        synchronized(this) {
        	transport.connections.remove(info.addr);
        }
        handleClose();
    }

    /** Closed connection event hadler.
     * Either by membership or death of peer.
     */
    void handleClose() {
        Connection info = this;
        final InetSocketAddress addr = info.addr;

        try {
            key.channel().close();
            key.cancel();        
            info.sock.close();
        } catch (IOException e) {// Don't care, we're cleaning up anyway...
        }
        if (addr == null) {
            return;
        }     
        final Connection outra = transport.connections.get(addr);

        if (info == outra) {
        	synchronized(this) {
        		transport.connections.remove(info.addr);
        	}
            transport.queue(new Runnable() {
                public void run() {
                    transport.membership_handler.close(outra);
                }
            });
        }
    }
}

// arch-tag: 31ba16d7-de61-4cce-98e4-26c590632002
