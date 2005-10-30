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

package neem;


import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;


/**
 * Implementation of the NEEM network layer.
 */
public class Transport implements Runnable {
    public Transport(InetSocketAddress local) throws IOException, BindException {
        timers = new TreeMap<Long, Runnable>();
        handlers = new Hashtable<Short, DataListener>();
        ssock = ServerSocketChannel.open();
        ssock.configureBlocking(false);
        
        ssock.socket().bind(local);
                
        selector = SelectorProvider.provider().openSelector();
        ssock.register(selector, SelectionKey.OP_ACCEPT);
        connections = new Hashtable<InetSocketAddress, Connection>();
        id = new InetSocketAddress(InetAddress.getLocalHost(), local.getPort());
                
    }
    
    /**
     * Get local id.
     */
    public InetSocketAddress id() {
        return id;
    }

    /**
     * Get ids of all direct peers.
     */
    public InetSocketAddress[] peers() {
        return (InetSocketAddress[]) connections.keySet().toArray(
                new InetSocketAddress[connections.size()]);
    }
    
    /**
     * Get all connections.
     */
    public Connection[] connections() {
        return (Connection[]) connections.values().toArray(
                new Connection[connections.size()]);
    }

    /**
     * Call periodically to garbage collect idle connections.
     */
    public void gc() {
        Iterator i = connections.values().iterator();

        while (i.hasNext()) {
            Connection info = (Connection) i.next();

            if (info.dirty && info.outgoing == null) {
                handleClose(info.key);
            } else {
                info.dirty = false;
            }
        }
    }

    /**
     * Close all socket connections and release polling thread.
     */
    public synchronized void close() {
        if (closed)
            return;
        closed=true;
        selector.wakeup();
        for(Connection info: connections.values())
            try {
                info.sock.close();
            } catch(IOException e) {
                // nada
            }
        try {
            ssock.close();
        } catch(IOException e) {
            // nada
        }
    }

    /**
     * Queue processing task.
     */
    public synchronized void queue(Runnable task) {
        schedule(task, 0);
    }

    /**
     * Schedule processing task.
     * @param delay delay before execution
     */
    public synchronized void schedule(Runnable task, long delay) {
        Long key = new Long(System.currentTimeMillis() + delay);

        timers.put(key, task);
        if (key == timers.firstKey()) {
            selector.wakeup();
        }
    }

    /**
     * Initiate connection to peer. This is effective only
     * after the open callback.
     */
    public Connection add(InetSocketAddress addr) {
        Connection info = (Connection) connections.get(addr);
    
        try {
            if (info == null) {
                SocketChannel sock = SocketChannel.open();

                sock.configureBlocking(false);
                sock.connect(addr);
                SelectionKey key = sock.register(selector,
                        SelectionKey.OP_CONNECT);

                info = new Connection(addr, key, null);
                key.attach(info);
                // connections.put(addr, info);
            }
        } catch (IOException e) {}
        return info;
    }
    
    public void remove(InetSocketAddress addr) {
        // System.out.println("Closing: " + addr.toString());
        Connection info = info = connections.get(addr);
        
        SelectionKey key = info.key;

        this.handleClose(key);
    }

    /**
     * Send message to 1 (one) peer identified by @arg: info.
     */
    public synchronized void send(ByteBuffer[] msg, Connection info, short port) {
        // Header order:
        /* --------
         *|msg size| <- from here
         * --------
         *|  uuid  | <- from DataListener
         * --------
         *|  msg   | <- from App
         * --------
         */
        if (info == null) { 
            return;
        }

        Integer prt = new Integer(port);
        Bucket b = new Bucket(Buffers.clone(msg), prt);

        info.msg_q.push((Object) b);
        handleWrite(info.key);
    }

    /**
     * Adds a reference to a gossip event handler.
     */
    public void handler(DataListener handler, short port) {
        this.handlers.put(new Short(port), handler);
    }
        
    /** Sets the reference to the membership event handler.
     */
    public void membership_handler(Membership handler) {
        this.membership_handler = handler;
    }

    /**
     * Main loop.
     */
    public void run() {
        while (true) {
            try {
                // Execute pending tasks.
                Runnable task = null;
                long delay = 0;

                synchronized (this) {
                    if (!timers.isEmpty()) {
                        long now = System.currentTimeMillis();
                        Long key = timers.firstKey();

                        if (key <= now) {
                            task = timers.remove(key);
                        } else {
                            delay = key - now;
                        }
                    }
                }
            
                if (task != null) {
                    task.run();
                } else {    
                    int s = selector.select(delay);
                    if (closed)
                        break;
                            
                    // Execute pending event-handlers.
                            
                    for (Iterator j = selector.selectedKeys().iterator(); j.hasNext();) {
                        SelectionKey key = (SelectionKey) j.next();

                        if (!key.isValid()) {
                            handleClose(key);
                            continue;
                        }
                        if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isConnectable()) {
                            handleConnect(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        } 
                    }
                }
                        
            } catch (IOException e) {
                // This handles only exceptions thrown by the selector and the
                // server socket. Invidual connections are dropped silently.
                e.printStackTrace();
            } catch (CancelledKeyException cke) {
                System.out.println("The selected key was closed.");
            }
        }
    }

    // ////// Event handlers
    
    /** Write event handler.
     * There's something waiting to be written.
     */
    private void handleWrite(SelectionKey key) {
        final Connection info = (Connection) key.attachment();
        
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
            handleClose(key);
            return;
        } catch (CancelledKeyException cke) {
            membership_handler.close(info.addr);
        }
        
    }

    private void handleRead(SelectionKey key) {
        final Connection info = (Connection) key.attachment();
        InetSocketAddress addr = null;
        
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
                handleClose(key);
            }
            info.copy.limit(info.incoming.position());
        } catch (IOException e) {
            handleClose(key);
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
                    if (info.firsttime) {
                        addr = AddressUtils.readAddressFromBuffer(info.copy); // n devia ser criado um novo info? este está associado a um endereço diferente
                        // System.out.println("READ: " + addr.toString()); // n devia haver problema, a não ser o n  de connections
                        info.addr = addr;
                        info.firsttime = false;
                        Connection outro = null;

                        try {
                            outro = this.connections.put(addr, info); // adiciona o addr recebido as connections
                        } catch (NullPointerException npe) {}
                        // info.incoming=null;
                        if (outro == null) {
                            queue(new Runnable() {
                                public void run() {
                                    membership_handler.open(info, 2);
                                }
                            });
                        } else {
                            try {
                                outro.key.channel().close();
                                outro.key.cancel();        
                                outro.sock.close();
                            } catch (IOException e) {}
                            // membership_handler.open(info, 2); not done here because we're just replacing the connection, it's not a new one
                        }
                        
                    } else {
                        info.msgsize = info.copy.getInt();
                        info.port = info.copy.getShort();
                        // System.out.println("Port: " + info.port);
                    }
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

                queue(
                        new Runnable() {
                    public void run() {
                        DataListener handler = handlers.get(prt);

                        try {
                            handler.receive(msg, info);    
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
     * When the hanlder behaves as server.
     */
    private void handleAccept(SelectionKey key) throws IOException {
                
        SocketChannel sock = ssock.accept();
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        
        if (sock == null) {
            return;
        }
        
        try {
            sock.configureBlocking(false);
            sock.socket().setSendBufferSize(1024);
            sock.socket().setReceiveBufferSize(1024);
            SelectionKey nkey = sock.register(selector,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            InetSocketAddress addr = (InetSocketAddress) sock.socket().getRemoteSocketAddress();
            final Connection info = new Connection(nkey);

            nkey.attach(info);
            info.firsttime = true;
            this.accepted++;
        } catch (IOException e) {// Just drop it.
        }
    
    }

    /** Open connection event hadler.
     * When the hanlder behaves as client.
     */
    private void handleConnect(SelectionKey key) throws IOException {
        
        final Connection info = (Connection) key.attachment();
        
        try {
            if (info.sock.finishConnect()) {
                info.sock.socket().setReceiveBufferSize(1024);
                info.sock.socket().setSendBufferSize(1024);
                // Write my accepting socket to socket
                info.sock.write(AddressUtils.writeAddressToBuffer(this.id()));
                Connection other = connections.put(info.addr, info);

                if (other == null) {
                    queue(new Runnable() {
                        public void run() {
                            membership_handler.open(info, 1);
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
        connections.remove(info.addr);
        handleClose(key);
    }

    /** Closed connection event hadler.
     * Either by membership or death of peer.
     */
    private void handleClose(SelectionKey key) {
        Connection info = (Connection) key.attachment();
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
        Connection outra = connections.get(addr);

        if (info == outra) {
            connections.remove(info.addr);
            queue(new Runnable() {
                public void run() {
                    membership_handler.close(addr);
                }
            });
        }
    }

    /** Local id for each instance
     */
    private InetSocketAddress id;
    
    /** Socket used to listen for connections
     */
    private ServerSocketChannel ssock;

    /** DUH, it's a selector (whatever that is)
     */
    private Selector selector;

    /** Storage for open connections to other group members
     */
    private Hashtable<InetSocketAddress, Connection> connections;

    /** Queue for tasks
     */
    private SortedMap<Long, Runnable> timers;

    /** DUH nb 2
     */
    private int busy;

    /** don't know rigth now
     */
    public int accepted = 0;

    /** Storage for DataListener protocol events handlers
     */
    private Hashtable<Short, DataListener> handlers;

    /** Reference for Membership events handler
     */
    private Membership membership_handler;

    private boolean closed;

    /**
     * Socket manipulation utilities.
     */
    public static class Connection {
        Connection(InetSocketAddress addr, SelectionKey key, ByteBuffer[] outgoing) {
            this.msg_q = new Queue(10);
            this.addr = addr;
            this.key = key;
            this.outgoing = outgoing;
            this.sock = (SocketChannel) key.channel();
        }

        Connection(SelectionKey key) {
            this.msg_q = new Queue(10);
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
        boolean writable, dirty, firsttime;
        short port;

        /** Message queue
         */
        public Queue msg_q;
    }


    ;
}


;

// arch-tag: d500660f-d7f0-498f-8f49-eb548dbe39f5
