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

/*
 * AddressUtils.java
 *
 * Created on April 15, 2005, 5:02 PM
 */

package neem;


import java.io.*;
import java.net.*;
import java.nio.*;


/**
 *  This abstract class provides methods to read/write InetSocketAddress 
 * addresses from/to a ByteBuffer Object or a Transport.Connection.
 *
 * @author psantos@GSD
 */
public abstract class AddressUtils {

    /** Write a socket address to a ByteBuffer.
     * @param addr The address to be written.
     * @return The Buffer with the address written into.
     */
    public static ByteBuffer writeAddressToBuffer(InetSocketAddress addr) {
        ByteBuffer msg = null;

        try {
            msg = ByteBuffer.allocate(6);
            msg.put(addr.getAddress().getAddress());
            msg.putShort((short) addr.getPort());
            msg.flip();
            // info.sock.write(msg);
        } catch (Exception e) {}
        return msg;
    }
    
    /** Read a socket address from a Connection
     * @param info The connection to read the address from.
     * @return The address read.
     */
    public static InetSocketAddress readAddress(Transport.Connection info) {
        InetSocketAddress addr = null;

        try {
            byte[] dst = new byte[4];

            info.copy.get(dst, 0, dst.length);
            InetAddress ia = InetAddress.getByAddress(dst);
            short port = info.copy.getShort();

            addr = new InetSocketAddress(ia, (int) port);
            // System.out.println("READ: "+addr.toString());
        } catch (IOException e) {}
        return addr;
    }
    
    /** Read a socket address from an array of ByteBuffers into an InetSocketAddress.
     * @param msg The array from wich to read the address from.
     * @return The address read.
     */
    public static InetSocketAddress readAddressFromBuffer(ByteBuffer[] msg) {
        InetSocketAddress addr = null;
	short port=0;
	byte[] dst=null;
	InetAddress ia=null;
	
        try {
            dst = new byte[4];
            ByteBuffer tmp = Buffers.sliceCompact(msg, 6);

            tmp.get(dst, 0, dst.length);
            port = tmp.getShort();
	    ia=InetAddress.getByAddress(dst);
            addr = new InetSocketAddress(InetAddress.getByAddress(dst),
                    (int) port);
        } catch (IOException e) {}
	catch (IllegalArgumentException iae) {System.out.println("Prob: "+ia.toString()+":"+port);}
	//catch (UnknownHostException uhe) {}
        return addr;
    }
}


;

// arch-tag: f387d158-3ec6-4001-af1b-5d4a8fb441eb
