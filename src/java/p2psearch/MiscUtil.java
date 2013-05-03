/*
 * 
 * The misc functions for all classes
 */
package p2psearch;

import java.security.MessageDigest;
import Configuration.DebugConfig;

/**
 *
 * @author 
 */
public class MiscUtil {

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
 
   public static String getHash(String plain){
       try{
       byte[] plainInBytes;
       plainInBytes = plain.getBytes("UTF-8");

       MessageDigest md;              
       
           //get the hex file name
            md = MessageDigest.getInstance("SHA");
            byte[] digest = md.digest(plainInBytes);
            String hashed = bytesToHex(digest);//doc name!!
            return hashed;
       }catch(Exception ex){
           return ex.getMessage();
       }

   }
    
    
}
