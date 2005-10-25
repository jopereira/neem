/*
 * FileOps.java
 *
 * Created on May 27, 2005, 3:09 PM
 *
 */

/**
 *  Logging utilities class.
 * @author psantos@lsd.di.uminho.pt
 */

package test;


import java.io.*;
import java.lang.*;
import java.util.*;


public abstract class FileOps {
    
    /** Write a string to a file.
     * @param o The string to be written.
     * @param file_name File to write to.
     */
    public static void write(String o, String file_name) {
        try {
            FileOutputStream fos = new FileOutputStream(file_name, true);
            PrintWriter oos = new PrintWriter(fos);

            oos.write(o);
            oos.close();
            fos.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {}
    }
    
    /** Read a string from a file.
     * @param file_name File to read from.
     * @return The string read.
     */
    public static String read(String file_name) {
        String res = null;

        try {
            FileReader fis = new FileReader(file_name);
            BufferedReader ois = new BufferedReader(fis);

            res = ois.readLine();
        } catch (FileNotFoundException fnfe) {} catch (IOException ioe) {}
        return res;
    }
}


;

// arch-tag: 9628a2a6-a377-49b1-b5d6-015cb557aa9c
