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
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the network layer.
 */
public class Transport implements Runnable {
	private static Logger logger = Logger.getLogger("net.sf.neem.impl.Transport");

	public Transport(Random rand, InetSocketAddress local) throws IOException, BindException {
		this.rand=rand;
		
		timers = new TreeMap<Long, Runnable>();
        handlers = new HashMap<Short, DataListener>();
        selector = SelectorProvider.provider().openSelector();

        connections = new HashSet<Connection>();
        idinfo = new Acceptor(this, local);
        
        this.bind = new InetSocketAddress(local.getAddress(), 0);
    }
      
	/**
     * Get local address.
     */
    public InetSocketAddress getLocalSocketAddress() {
    	return idinfo.getLocalSocketAddress();
    }
  
    /**
     * Get all connections.
     */
    public Connection[] connections() {
        return connections.toArray(new Connection[connections.size()]);
    }

	/**
     * Get addresses of all connected peers.
     */
    public InetSocketAddress[] getPeers() {
    	List<InetSocketAddress> addrs=new ArrayList<InetSocketAddress>();
    	for(Connection info: connections) {
    		InetSocketAddress addr=info.getPeer();
    		if (addr!=null)
    			addrs.add(addr);
    	}
    	return addrs.toArray(new InetSocketAddress[addrs.size()]);
    }

    /**
     * Call periodically to garbage collect idle connections.
     */
    public void gc() {
    	for(Connection info: connections)
            info.handleGC();
    }

    /**
     * Close all socket connections and release polling thread.
     */
    public synchronized void close() {
        if (closed)
            return;
        closed=true;
        timers.clear();
        chandler=null;
        selector.wakeup();
        for(Connection info: connections)
            info.handleClose();
        idinfo.handleClose();
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
        Long key = System.nanoTime() + delay*1000000;

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
    public void add(InetSocketAddress addr) {
        try {
           new Connection(this, bind, addr);
        } catch (IOException e) {
        	logger.log(Level.WARNING, "failed to add peer "+addr, e);
        }
    }

    /**
     * Add a reference to a message handler.
     */
    public void setDataListener(DataListener handler, short port) {
        this.handlers.put(port, handler);
    }
        
    /**
     * Sets the reference to the connection handler.
     */
    public void setConnectionListener(ConnectionListener handler) {
        this.chandler = handler;
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
                 
                    Iterator<SelectionKey> keys=selector.selectedKeys().iterator();
                    while(keys.hasNext()) {
                    	SelectionKey key=keys.next();
                        Handler info = (Handler) key.attachment();

                        if (!key.isValid()) {
                            info.handleClose();
                            continue;
                        }
                        if (key.isReadable()) {
                            info.handleRead();
                        } else if (key.isAcceptable()) {
                            info.handleAccept();
                        } else if (key.isConnectable()) {
                            info.handleConnect();
                        } else if (key.isWritable()) {
                            info.handleWrite();
                        }
                        keys.remove();
                    }
                }
                        
            } catch (IOException e) {
                // This handles only exceptions thrown by the selector and the
                // server socket. Individual connections are dropped silently.
            	logger.log(Level.SEVERE, "main loop failed, terminating", e);
            	close();
            } catch (CancelledKeyException cke) {
            	// Don't care
            }
        }
    }

	void notifyOpen(final Connection info) {
        synchronized(this) {
        	connections.add(info);
        }
        queue(new Runnable() {
			public void run() {
				if (chandler != null)
					chandler.open(info);
			}
		});
	}
    
	void notifyClose(final Connection info) {
        if (closed) {
			return;
		}
		synchronized (this) {
			connections.remove(info);
		}
		queue(new Runnable() {
			public void run() {
				if (chandler!=null)
					chandler.close(info);
			}
		});
	}

	void deliver(final Connection source, final Short prt, final ByteBuffer[] msg) {
		final DataListener handler = handlers.get(prt);
		if (handler==null) {
			// unknown handler
			return;
		}
		queue(new Runnable() {
			public void run() {
				try {
					handler.receive(msg, source, prt);
				} catch(BufferUnderflowException e) {
	            	logger.log(Level.WARNING, "corrupt or truncated message from "+source.getRemoteAddress(), e);
					source.handleClose();
				}
			}
		});
	}

	private InetSocketAddress bind;
	
    private Acceptor idinfo;

    /**
     * Selector for events
     */
    Selector selector;

    /** Storage for open connections to other group members
     * This variable can be queried by an external thread for JMX
     * management. Therefore, all sections of the code that modify it must
     * be synchronized. Sections that read it from the protocol thread need
     * not be synchronized.
     */
    private Set<Connection> connections;

    /** 
     * Queue for tasks
     */
    private SortedMap<Long, Runnable> timers;

    /** 
     * Storage for DataListener protocol events handlers
     */
    private Map<Short, DataListener> handlers;

    /** 
     * Reference for ConnectionListener events handler
     */
    private ConnectionListener chandler; // bing!

    /**
     * If we're not responding any more
     */
    private boolean closed;
    
    /**
     * Shared random number generator
     */
    Random rand;
    
    // Configuration parameters
    
    /**
     * Execution queue size
     */
    private int queueSize = 10;
    private int bufferSize = 1024;

    public int getQueueSize() {
		return queueSize;
	}

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	// Statistics
	
    public int accepted, connected;
    public int pktOut, pktIn;
    public int bytesOut, bytesIn;

    public void resetCounters() {
        accepted=connected=pktOut=pktIn=bytesOut=bytesIn=0;
	}
}

