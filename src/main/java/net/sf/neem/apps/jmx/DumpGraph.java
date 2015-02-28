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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import net.sf.neem.ProtocolMBean;

public class DumpGraph {
	
	/**
	 * Names of attributes used to decorate nodes.
	 */
	private static String[] nodeAttr={"Delivered", "DataReceived", "BytesSent", "BytesReceived"};
	
	static class Node {
		public AttributeList attrs;
		
		public String toString() {
			String res="";
			if (attrs==null)
				return res;
			for(Object item: attrs) {
				Attribute attr=(Attribute)item;
				if (res.length()>0)
					res+=",";
				res+=attr.getName()+"="+attr.getValue();
			}
			return res;
		}
	};
	
	static class Link {
		public Object n1, n2;
		
		public Link(Object center, Object i) {
			n1=center;
			n2=i;
		}

		public boolean equals(Object other) {
			if (!(other instanceof Link))
				return false;
			Link link=(Link)other;
			return (n1.equals(link.n1)&&n2.equals(link.n2))||
				(n1.equals(link.n2)&&n2.equals(link.n1));
		}
		
		public int hashCode(){
			return n1.hashCode()^n2.hashCode();
		}
		
		public String toString() {
			return "\""+n1+"\" -- \""+n2+"\"";
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void dumpAddrs(String host, Map<Object,Node> nodes, Set<Link> links) throws IOException, JMException {
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+host+"/jmxrmi");
		JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
		MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
		Set<ObjectName> beans=mbsc.queryNames(new ObjectName("net.sf.neem:type=Protocol,*"), null);
		for(ObjectName b: beans) {
			ProtocolMBean proto=(ProtocolMBean)MBeanServerInvocationHandler.newProxyInstance(mbsc, b, ProtocolMBean.class, false);
			InetSocketAddress center=proto.getLocalAddress();
			// Silly routine to force remove spurious host names...
			center=new InetSocketAddress(center.getAddress().getHostAddress(), center.getPort());
			System.err.println("Dumping Addresses: "+center+" at "+host);
			Node node=nodes.get(center);
			if (node==null) {
				node=new Node();
				nodes.put(center, node);
			}
			node.attrs=mbsc.getAttributes(b, nodeAttr);
			for(InetSocketAddress i: proto.getPeerAddresses()) {
				// The same thing again. *sigh*
				InetSocketAddress j=new InetSocketAddress(i.getAddress().getHostAddress(), i.getPort());
				links.add(new Link(center, j));
				if (!nodes.containsKey(i))
					nodes.put(i, new Node());
			}
		}		
	}
	
	@SuppressWarnings("unchecked")
	private static void dumpUUIDs(String host, Map<Object,Node> nodes, Set<Link> links) throws IOException, JMException {
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://"+host+"/jmxrmi");
		JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
		MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
		Set<ObjectName> beans=mbsc.queryNames(new ObjectName("net.sf.neem:type=Protocol,*"), null);
		for(ObjectName b: beans) {
			ProtocolMBean proto=(ProtocolMBean)MBeanServerInvocationHandler.newProxyInstance(mbsc, b, ProtocolMBean.class, false);
			UUID center=proto.getLocalId();
			System.err.println("Dumping UUID: "+center+" at "+host);
			Node node=nodes.get(center);
			if (node==null) {
				node=new Node();
				nodes.put(center, node);
			}
			node.attrs=mbsc.getAttributes(b, nodeAttr);
			for(UUID i: proto.getPeerIds()) {
				links.add(new Link(center, i));
				if (!nodes.containsKey(i))
					nodes.put(i, new Node());
			}
		}		
	}
	
	public static void main(String[] args) {
		if (args.length < 1) {
            System.err.println("Usage: net.sf.neem.apps.jmx.DumpGraph [-u] host:port ...");
            System.exit(1);
		}
		
		try {
			Map<Object,Node> nodes=new HashMap<Object,Node>();
			Set<Link> links=new HashSet<Link>();
			if (args[0].equals("-u"))
				for(int i=1;i<args.length;i++)
					dumpUUIDs(args[i], nodes, links);
			else
				for(int i=0;i<args.length;i++)
					dumpAddrs(args[i], nodes, links);
			System.out.println("graph Links {");
			for(Object id: nodes.keySet()) {
				Node node=nodes.get(id);
				System.out.println("  \""+id+"\" ["+node+"];");
			}
			for(Link link: links)
				System.out.println("  "+link+";");
			System.out.println("}");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

