/*
 * ConfigLoader.java
 *
 * Created on July 26, 2005, 3:04 PM
 *
 */


package test;

import neem.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;


/**
 * This class provides an easy configuration method for creating Neem class
 * instance objects, by loading Neem parameters from a file.
 * @author psantos@lsd.di.uminho.pt
 */
public abstract class ConfigLoader {
    
    /**
     * Reads parameter configuration from a file and returns a Neem object.
     *@param filename Configuration file
     */
    public static Neem load(String filename) {
        String opn = new String();

        try {
            FileReader fis = new FileReader(filename);
            BufferedReader ois = new BufferedReader(fis);

            while (opn != null) {
                opn = ois.readLine();
                if (!opn.startsWith("# ")) {
                    // StringTokenizer tknzr1=new StringTokenizer(opn,"=",false);
                    // System.out.println(tknzr1.nextToken());
                    // System.out.println(tknzr1.nextToken());
                    if (opn.startsWith("local")) {
                        StringTokenizer tknzr = new StringTokenizer(opn,
                                "local=", false);

                        local = tknzr.nextToken();
                        // System.out.println(local);
                    } else if (opn.startsWith("port")) {
                        StringTokenizer tknzr = new StringTokenizer(opn, "port=",
                                false);

                        port = Integer.parseInt(tknzr.nextToken());
                        // System.out.println(port);
                    } else if (opn.startsWith("g_syncport")) {
                        StringTokenizer tknzr = new StringTokenizer(opn,
                                "g_syncport=", false);

                        g_syncport = Short.parseShort(tknzr.nextToken());
                        // System.out.println(g_syncport);
                    } else if (opn.startsWith("m_syncport")) {
                        StringTokenizer tknzr = new StringTokenizer(opn,
                                "m_syncport=", false);

                        m_syncport = Short.parseShort(tknzr.nextToken());
                        // System.out.println(m_syncport);
                    } else if (opn.startsWith("fanout")) {
                        StringTokenizer tknzr = new StringTokenizer(opn,
                                "fanout=", false);

                        fanout = Integer.parseInt(tknzr.nextToken());
                        // System.out.println(fanout);
                    } else if (opn.startsWith("group_size")) {
                        StringTokenizer tknzr = new StringTokenizer(opn,
                                "group_size=", false);

                        group_size = Integer.parseInt(tknzr.nextToken());
                        // System.out.println(group_size);
                    } else {
                        ;
                    }
                }
		
            }
            ois.close();
            fis.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println(
                    "** File not found. Please enter a valid filename. **");
            return null;
        } catch (IOException ioe) {
            System.out.println("ioe");
        } catch (NullPointerException npe) {}
	
        if (local.equalsIgnoreCase("") && port == 0 && g_syncport == (short) 0
                && m_syncport == (short) 0 && fanout == 0 && group_size == 0) {
            return null;
        }
        return new Neem(local, port, g_syncport, m_syncport, fanout, group_size);
    }
    
    public static void main(String[] args) {
        Neem net = ConfigLoader.load(args[0]);

        if (net != null) {
            String s = net.getTransportIdAsString();

            System.out.println(s);
            net.connect();
        }
	
    }
    
    private static String local;
    private static int port;
    private static short g_syncport;
    private static short m_syncport;
    private static int fanout;
    private static int group_size;
    
}


;

// arch-tag: 335ad432-97af-4de6-8340-42b56eb8dca3
