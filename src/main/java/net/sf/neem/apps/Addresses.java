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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Utility class to parse addresses.
 */
public final class Addresses {
	private Addresses() {}

	/**
	 * Parses address strings. Accepts things as "localhost:12345", "127.0.0.1:12345", 
	 * ":12345" or "12345". The later result in INADDR_ANY or 127.0.0.1 being used for
	 * the address, depending on the second parameter. 
	 * 
	 * @param arg a textual representation of the address
	 * @param isany sets defaults to INADDR_ANY:0
	 * @return the address
	 * @throws UnknownHostException unable to parse the address
	 */
	public static InetSocketAddress parse(String arg, boolean isany) throws UnknownHostException {
		try {
			if (arg.equals(":") && isany)
				return new InetSocketAddress(0);
			String[] parms = arg.split(":");
			if (parms.length==2) {
				int port=0;
				if (parms[1].length()>0)
					port=Integer.parseInt(parms[1]);
				if (port!=0 || isany)
					if (parms[0].length()>0)
						return new InetSocketAddress(parms[0], port);
					else
						return new InetSocketAddress(port);
			} else if (parms.length==1)
				return new InetSocketAddress(Integer.parseInt(parms[0]));
		} catch(Exception e) {
			// fall through...
		}
        throw new UnknownHostException(arg);
	}
}
