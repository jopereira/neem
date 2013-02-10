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

package net.sf.neem.apps.jmx;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class Reconfig {
	@SuppressWarnings("unchecked")
	private static void dump(String host, Properties props) throws IOException, JMException {
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+host+"/jmxrmi");
		JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
		MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
		Set<ObjectName> beans=mbsc.queryNames(new ObjectName("net.sf.neem:type=Protocol,*"), null);
		AttributeList attrs=new AttributeList();
		for(Object attr: props.keySet()) {
			String key=(String)attr;
			Integer value=new Integer(props.getProperty(key));
			attrs.add(new Attribute(key, value));
		}
		for(ObjectName b: beans) {
			try {
				mbsc.setAttributes(b, attrs);
				System.out.println(" - "+b);
			} catch (Exception e) {
				System.err.println("Cannot set attributes on " + b);
				e.printStackTrace();
			}
		}		
	}
	
	public static void main(String[] args) {
		if (args.length < 2) {
            System.err.println("Usage: net.sf.neem.apps.jmx.Reconfig properties host:port ...");
            System.exit(1);
		}
		
		try {
			Properties props=new Properties();
			props.load(new FileInputStream(args[0]));
			System.out.println("Loading: "+props);
			for(int i=1;i<args.length;i++)
				dump(args[i], props);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

