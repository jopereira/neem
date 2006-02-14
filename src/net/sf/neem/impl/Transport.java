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
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;


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
                info.handleClose();
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
        Long key = new Long(System.nanoTime() + delay*1000000);

        while(timers.containsKey(key))
        	key=key+1;
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

                info = new Connection(this, addr, key, null, default_Q_size);
                key.attach(info);
                // connections.put(addr, info);
            }
        } catch (IOException e) {}
        return info;
    }
    
    public void remove(InetSocketAddress addr) {
        // System.out.println("Closing: " + addr.toString());
        Connection info = info = connections.get(addr);
        
        if (info==null)
        	return;
        
        info.handleClose();
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
        info.handleWrite();
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
                        long now = System.nanoTime();
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
                	if (delay>0 && delay<1000000)
                		delay=1;
                	else
                		delay/=1000000;

                	selector.select(delay);
                    if (closed)
                        break;
                            
                    // Execute pending event-handlers.
                            
                    for (Iterator j = selector.selectedKeys().iterator(); j.hasNext();) {
                        SelectionKey key = (SelectionKey) j.next();
                        Connection info = (Connection) key.attachment();

                        if (!key.isValid()) {
                            info.handleClose();
                            continue;
                        }
                        if (key.isReadable()) {
                            info.handleRead();
                        } else if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isConnectable()) {
                            info.handleConnect();
                        } else if (key.isWritable()) {
                            info.handleWrite();
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

    /** Open connection event hadler.
     * When the hanlder behaves as server.
     */
    private void handleAccept(SelectionKey key) throws IOException {
                
        SocketChannel sock = ssock.accept();
       
        if (sock == null) {
            return;
        }
        
        try {
            sock.configureBlocking(false);
            sock.socket().setSendBufferSize(1024);
            sock.socket().setReceiveBufferSize(1024);
            SelectionKey nkey = sock.register(selector,
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            InetSocketAddress addr=(InetSocketAddress)sock.socket().getRemoteSocketAddress();
            final Connection info = new Connection(this, addr, nkey, null, default_Q_size);

            nkey.attach(info);

            synchronized(this) {
            	this.connections.put(addr, info); // adiciona o addr recebido as connections
            }
            queue(new Runnable() {
                public void run() {
                	membership_handler.open(info);
                }
            });
            this.accepted++;
        } catch (IOException e) {// Just drop it.
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
     * This variable can be queried by an external thread for JMX
     * management. Therefore, all sections of the code that modify it must
     * be synchronized. Sections that read it from the protocol thread need
     * not be synchronized.
     */
    Hashtable<InetSocketAddress, Connection> connections;

    /** Queue for tasks
     */
    private SortedMap<Long, Runnable> timers;

    /** don't know rigth now
     */
    public int accepted = 0;

    /** Storage for DataListener protocol events handlers
     */
    Hashtable<Short, DataListener> handlers;

    /** Reference for Membership events handler
     */
    Membership membership_handler;

    private boolean closed;
    
    private int default_Q_size = 10;

    public int getDefault_Q_size() {
		return default_Q_size;
	}

	public void setDefault_Q_size(int default_Q_size) {
		this.default_Q_size = default_Q_size;
	}

	/**
     * Get ids of all direct peers.
     */
    public InetSocketAddress[] getPeers() {
        return (InetSocketAddress[]) connections.keySet().toArray(
                new InetSocketAddress[connections.size()]);
    }
}

// arch-tag: d500660f-d7f0-498f-8f49-eb548dbe39f5
