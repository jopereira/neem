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

package net.sf.neem.apps.perf;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.neem.MulticastChannel;
import net.sf.neem.ProtocolMBean;
import net.sf.neem.apps.Addresses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a crowd of NeEM peers. All messages received are discarded. No
 * messages are sent. Use JMX to manipulate paramters.
 */
public class Crowd extends Thread {
	private static Logger logger = LoggerFactory.getLogger(Crowd.class); 
	
    private MulticastChannel neem;
    
	public Crowd(MulticastChannel neem) {
        this.neem = neem;
    }
    
    public void run() {
        try {
            ByteBuffer bb = ByteBuffer.allocate(1);
            while (true) {
            	bb.clear();
                neem.read(bb);
            }
        } catch (Exception e) {
        	logger.error("exception caught by worker", e);
        }
    }
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: net.sf.neem.apps.perf.Crowd baseport instances peer1 ... peerN");
            System.exit(1);
        }
     
        try {
			int port = Integer.parseInt(args[0]);
			int instances = Integer.parseInt(args[1]);

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

			Crowd[] smalls=new Crowd[instances];
			for (int i = 0; i < instances; i++) {
				smalls[i] = new Crowd(new MulticastChannel(
						new InetSocketAddress(port + i)));
				smalls[i].neem.setTruncateMode(true);
				ProtocolMBean mbean = smalls[i].neem.getProtocolMBean();
				ObjectName name = new ObjectName(
						"net.sf.neem:type=Protocol,id=" + mbean.getLocalId());
				mbs.registerMBean(mbean, name);
				smalls[i].start();
			}
			for (int i = 2; i < args.length; i++)
                smalls[0].neem.connect(Addresses.parse(args[i], false));
			for (int i = 1; i < instances; i++) {
				Thread.sleep(50);
				smalls[i].neem.connect(smalls[0].neem.getLocalSocketAddress());
			}
		} catch (Exception e) {
        	logger.error("exception caught", e);
		}
    }
}

