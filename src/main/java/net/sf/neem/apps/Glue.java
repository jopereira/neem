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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.UUID;

import net.sf.neem.impl.Connection;
import net.sf.neem.impl.Overlay;
import net.sf.neem.impl.Transport;

public class Glue {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: apps.Glue target peer1 .. peerN");
            System.exit(-1);
        }
        
        try {
			final Transport net = new Transport(new Random(), new InetSocketAddress(0));
			InetSocketAddress pub = new InetSocketAddress(InetAddress.getLocalHost(), net.getLocalSocketAddress().getPort());
			Overlay mipl = new Overlay(new Random(), pub, UUID.randomUUID(), net, (short) 1, (short) 2, (short)3);
			
			InetSocketAddress target = Addresses.parse(args[0], false);
			net.add(target);

			Thread t=new Thread(net);
			t.setDaemon(true);
			t.start();

			Thread.sleep(1000);

			Connection[] targets = mipl.connections();
			if (targets.length==0) {
				System.err.println("No connection to target.");
				System.exit(2);
			}
						
			for (int i = 1; i < args.length; i++) {
				final InetSocketAddress addr = Addresses.parse(args[i], false);
				net.queue(new Runnable() {
					public void run() {
						net.add(addr);						
					}
				});
			}

			Thread.sleep(1000);

			Connection[] conns = mipl.connections();
			for (int i = 0; i < conns.length; i++) {
				System.out.println("Sending "+conns[i].listen+" to "+targets[0].listen);
				if (conns[i]!=targets[0])
					mipl.tradePeers(targets[0], conns[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}

