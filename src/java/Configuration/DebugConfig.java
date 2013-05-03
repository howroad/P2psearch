/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains some misc functions that are used for debugging.
 * 
 */
public class DebugConfig {

    public static final boolean DEBUG = false;//this is to switch on/off the p2p layer attribute.
            
    public static void checkHosts(String subnet) throws UnknownHostException, IOException {
        int timeout = 1000;
        for (int i = 1; i < 254; i++) {

            String host = subnet + "." + i;
            System.out.println(host);
            if (InetAddress.getByName(host).isReachable(timeout)) {
                System.out.println(host + " is reachable");
            }
        }
    }

    //delete the files in the folder
    public static void deleteFileForTest(File folder) {
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    deleteFileForTest(file);
                } else {
                    file.delete();
                }
            }
        }
    }
    
    public static void printAndLog(String name,String mesg){
        Logger.getLogger(name).log(Level.ALL,mesg);
        System.out.println(mesg);
    }
    public static void printAndLog(String name,String mesg,Throwable ex){
        Logger.getLogger(name).log(Level.ALL,mesg,ex);
        System.out.println(mesg+ex.toString());
    }
}
