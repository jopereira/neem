
package test;

import pson.*;
import java.nio.*;
import java.net.*;
import java.io.*;

public class Other implements Gossip {
	private Transport net;

	public Other(Transport net) {
		this.net=net;
	}

	public void open(Transport.Connection info) {
		System.out.println("OPEN: "+info.toString());
		ByteBuffer msg=ByteBuffer.allocate(4);
		msg.putInt(123);
		net.send(new ByteBuffer[]{msg}, info);
	}

	public void close(InetSocketAddress addr) {
		System.out.println("CLOSE: "+addr.toString());
	}

	public void receive(ByteBuffer[] msg, Transport.Connection info) {
		System.out.println("RECEIVE: "+info.toString()+" "+msg[0].getInt());
	}

	public static void main(String[] args) {
		try {
			Transport l=new Transport(new InetSocketAddress("localhost", 12346));
			l.add(new InetSocketAddress("localhost", 12345));
			l.handler(new Other(l));
			l.run();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
		
// arch-tag: ab387e4e-132e-4e36-9474-f01cf5e91850
