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

package net.sf.neem.apps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;


/**
 * Simple chat application.
 */
public class Chat extends Thread {
    public Chat(MulticastChannel neem) {
        this.neem = neem;
        neem.setLoopbackMode(false);
        setDaemon(true);
        start();
    }

    private MulticastChannel neem;

    public void run() {
        try {
            while (true) {
                byte[] buf = new byte[1000];
                ByteBuffer bb = ByteBuffer.wrap(buf);

                neem.read(bb);
                System.out.println("> " + new String(buf));
            }
        } catch (AsynchronousCloseException ace) {
        	// Exiting.
    	} catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void inputLoop() throws IOException {
        BufferedReader r =
        	new BufferedReader(new InputStreamReader(System.in));
        String line;

        while ((line = r.readLine())!=null) {
            neem.write(ByteBuffer.wrap(line.getBytes()));
        }    	
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: apps.Chat local peer1 ... peerN");
            System.exit(1);
        }

        try {

            MulticastChannel neem = new MulticastChannel(Addresses.parse(args[0], true));

            System.out.println("Started: " + neem.getLocalSocketAddress());
			
            if (neem.getLocalSocketAddress().getAddress().isLoopbackAddress())
                System.out.println("WARNING: Hostname resolves to loopback address! Please fix network configuration\nor expect only local peers to connect.");
            
            // Export JMX management bean when run with:
            //  java -Dcom.sun.management.jmxremote apps.Chat ...
            // or remote with:
            //  java -Dcom.sun.management.jmxremote.port=<some-port> \
            // 	 -Dcom.sun.management.jmxremote.authenticate=false \
            //   -Dcom.sun.management.jmxremote.ssl=false apps.Chat ...
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ProtocolMBean mbean = neem.getProtocolMBean();
            ObjectName name = new ObjectName("net.sf.neem:type=Protocol,id="+mbean.getLocalId());
            mbs.registerMBean(mbean, name);
            
            Chat chat = new Chat(neem);

            for (int i = 1; i < args.length; i++) 
            	neem.connect(Addresses.parse(args[i], false));

            chat.inputLoop();
            
            neem.close();

        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
}

