
package pson;

import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;

/**
 * Implementation of the NEEM network layer.
 */
public class Transport {
	public Transport(InetSocketAddress local) throws IOException {
		queued=new ArrayList();
		ssock=ServerSocketChannel.open();
		ssock.configureBlocking(false);

		InetAddress ip=InetAddress.getLocalHost();
		InetSocketAddress addr=new InetSocketAddress(ip, local.getPort());
		ssock.socket().bind(addr);
	
		selector=SelectorProvider.provider().openSelector();
		ssock.register(selector, SelectionKey.OP_ACCEPT);
		connections=new HashMap();
	}
	
	/**
	 * Get local id.
	 */
	public InetSocketAddress id() {
		return (InetSocketAddress)ssock.socket().getLocalSocketAddress();
	}

	/**
	 * Get ids of all direct peers.
	 */
	public InetSocketAddress[] peers() {
		return (InetSocketAddress[])connections.keySet().toArray(new InetSocketAddress[connections.size()]);
	}
	
	/**
	 * Get all connections.
	 */
	public Connection[] connections() {
		return (Connection[])connections.values().toArray(new Connection[connections.size()]);
	}

	/**
	 * Call periodically to garbage collect idle connections.
	 */
	public void gc() {
		Iterator i=connections.values().iterator();
		while(i.hasNext()) {
			Connection info=(Connection)i.next();
			if (info.dirty && info.outgoing==null)
				handleClose(info.key);
			else
				info.dirty=false;
		}
	}

	/**
	 * Queue processing task.
	 */
	public synchronized void queue(Runnable task) {
		queued.add(task);
		selector.wakeup();
	}

	/**
	 * Initiate connection to peer. This is effective only
	 * after the open callback.
	 */
	public Connection add(InetSocketAddress addr) {
		Connection info=(Connection)connections.get(addr);
		if (info==null) {
			try {
				SocketChannel sock=SocketChannel.open();
				sock.configureBlocking(false);
				sock.connect(addr);
				SelectionKey key=sock.register(selector, SelectionKey.OP_CONNECT);
				info=new Connection(addr, key, null);
				key.attach(info);
				connections.put(addr, info);
			} catch (IOException e) {
				// Don't care.
			}
		}
		return info;
	}

	/**
	 * Send message to peer.
	 */
	public void send(ByteBuffer[] msg, Connection info) {
		if (info==null)
			return;
		if (info.outgoing!=null)
			return;
		int size=0;
		for(int i=0;i<msg.length;i++)
			size+=msg[i].remaining();
		ByteBuffer header=ByteBuffer.allocate(4);
		header.putInt(size);
		info.outgoing=new ByteBuffer[msg.length+1];
		System.arraycopy(msg, 0, info.outgoing, 1,msg.length);
		info.outgoing[0]=header;
		handleWrite(info.key);
	}

	/**
	 * Connect event handler.
	 */
	public void handler(Gossip handler) {
		this.handler=handler;
	}

	/**
	 * Main loop.
	 */
	public void run() {
		try {
			while (true) {
				selector.select();

				// Execute pending tasks.
				ArrayList execq=null;
				synchronized(this) {
					if (!queued.isEmpty()) {
						execq=queued;
						queued=new ArrayList();
					}
				}
				if (execq!=null) {
					for(Iterator i=execq.iterator(); i.hasNext();) {
						Runnable task=(Runnable)i.next();
						task.run();
					}
				}
				
				// Execute pending event-handlers.
				for (Iterator i = selector.selectedKeys().iterator(); i
						.hasNext();) {
					SelectionKey key = (SelectionKey) i.next();
					if (key.isReadable())
						handleRead(key);
					else if (key.isAcceptable())
						handleAccept(key);
					else if (key.isConnectable())
						handleConnect(key);
					else if (key.isWritable())
						handleWrite(key);
					else
						handleClose(key);
				}
			}
		} catch (IOException e) {
			// This handles only exceptions thrown by the selector and the
			// server socket. Invidual connections are dropped silently.
			e.printStackTrace();
		}
	}

	//////// Event handlers

	private void handleWrite(SelectionKey key) {
		final Connection info=(Connection)key.attachment();
		try {
			if (info.outgoing!=null) {
				info.outremaining-=info.sock.write(info.outgoing, 0, info.outgoing.length);
				if (info.outremaining!=0)
					return;
			}
			info.writable=true;
			info.outgoing=null;
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			handleClose(key);
		}
	}

	private void handleRead(SelectionKey key) {
		final Connection info=(Connection)key.attachment();

		// New buffer?
		if (info.incoming==null || info.incoming.remaining()==0) {
			info.incoming=ByteBuffer.allocate(1024);
			info.copy=info.incoming.asReadOnlyBuffer();
		}

		// Read as much as we can with a single buffer.			
		try {
			while(info.sock.read(info.incoming)>0)
				;
			info.copy.limit(info.incoming.position());
		} catch (IOException e) {
			handleClose(key);
			return;
		}

		while (info.copy.hasRemaining()) {
			// Are we starting with a new message?
			if (info.msgsize==0) {
				// Read header, if enough bytes are available.
				// See below what happens when the current buffer
				// is too full to contain a header.
				if (info.copy.remaining()>=4)
					info.msgsize=info.copy.getInt()-4;
				else
					break;
			}

			// Now we can read a message
			int slicesize=info.msgsize;
			if (info.msgsize>info.copy.remaining())
				slicesize=info.copy.remaining();
			ByteBuffer slice=info.copy.slice();
			slice.limit(slicesize);
			info.copy.position(info.copy.position()+info.msgsize);

			// Is it a new message?
			if (info.incomingmb==null)
				info.incomingmb=new ArrayList();

			info.incomingmb.add(slice);
			info.msgsize-=slicesize;

			// Is the message complete?
			if (info.msgsize==0) {
				final ByteBuffer[] msg=(ByteBuffer[])info.incomingmb.toArray(new ByteBuffer[info.incomingmb.size()]);
				queue(new Runnable() {
					public void run() {
						handler.receive(msg, info);
					}
				});
				info.incomingmb=null;
				info.incoming=null;
				info.copy=null;
			}
		}

		// Avoid a fragmented header. If/when more data is
		// available select will call us back.
		if (info.incoming.remaining()+info.copy.remaining()<4) {
			ByteBuffer compacted=ByteBuffer.allocate(1024);
			while(info.copy.hasRemaining())
				compacted.put(info.copy.get());
			info.incoming=compacted;
			info.copy=info.incoming.asReadOnlyBuffer();
			info.copy.limit(info.incoming.position());
		}
	}

	private void handleAccept(SelectionKey key) throws IOException {
		SocketChannel sock=ssock.accept();
		if (sock==null)
			return;
		try {
			sock.configureBlocking(false);
			SelectionKey nkey=sock.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
			InetSocketAddress addr=(InetSocketAddress)sock.socket().getRemoteSocketAddress();
			final Connection info=new Connection(nkey);
			nkey.attach(info);
			connections.put(addr, info);
			queue(new Runnable() {
				public void run() {
					handler.open(info);
				}
			});
		} catch (IOException e) {
			// Just drop it.
		}
	}

	private void handleConnect(SelectionKey key) throws IOException {
		final Connection info=(Connection)key.attachment();
		try {
			if (info.sock.finishConnect()) {
				queue(new Runnable() {
					public void run() {
						handler.open(info);
					}
				});
				key.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
				return;
			}
		} catch (IOException e) {
			// Fall through, see below:
		}
		info.sock.close();
		connections.remove(info.addr);
	}

	private void handleClose(SelectionKey key) {
		Connection info=(Connection)key.attachment();
		final InetSocketAddress addr=info.addr;
		queue(new Runnable() {
			public void run() {
				handler.close(addr);
			}
		});
		connections.remove(info.addr);
		try {
			key.channel().close();
		} catch (IOException e) {
			// Don't care, we're cleaning up anyway...
		}
	}

	private ServerSocketChannel ssock;
	private Selector selector;
	private Map connections;
	private Gossip handler;
	private ArrayList queued;

	public static class Connection {
		Connection(InetSocketAddress addr, SelectionKey key, ByteBuffer[] outgoing) {
			this.addr=addr;
			this.key=key;
			this.outgoing=outgoing;
			this.sock=(SocketChannel)key.channel();
		}

		Connection(SelectionKey key) {
			this.key=key;
			this.sock=(SocketChannel)key.channel();
			this.addr=(InetSocketAddress)sock.socket().getRemoteSocketAddress();
		}

		public int hashCode() {
			return addr.hashCode();
		}

		public boolean equals(Object other) {
			return (other instanceof Connection) && addr.equals(((Connection)other).addr);
		}

		public String toString() {
			return "Connection to "+addr;
		}

		InetSocketAddress addr;
		SocketChannel sock;
		SelectionKey key;
		ByteBuffer incoming, copy;
		ArrayList incomingmb;
		int msgsize;
		ByteBuffer[] outgoing;
		int outremaining;
		boolean writable, dirty;
	};
};

// arch-tag: d500660f-d7f0-498f-8f49-eb548dbe39f5
