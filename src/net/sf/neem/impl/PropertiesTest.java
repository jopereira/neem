/**
 * 
 */
package net.sf.neem.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

/**
 * @author psantos
 *
 */
public class PropertiesTest {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Properties p = new Properties();
        p.setProperty("arg1","value1");
        p.setProperty("arg2","value2");
        p.setProperty("arg1","value3");
        try {
            File f = new File("data/conf/test.properties"); 
            f.createNewFile();
            p.store(new FileOutputStream(f),"No comments");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        Properties r = new Properties();
        try {
            r.load(new FileInputStream("data/conf/test.properties"));
        } catch (InvalidPropertiesFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

// arch-tag: 2fcc89e6-26a5-44c2-915d-03ba0821f9f4
