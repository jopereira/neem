
package pson;

import java.net.*;
import java.nio.*;

public interface Gossip {
	public void open(Transport.Connection info);
	public void close(InetSocketAddress addr);
	public void receive(ByteBuffer[] msg, Transport.Connection info);
};

// arch-tag: 87a87e28-12f1-44ae-a156-6f4f6d5266b6
