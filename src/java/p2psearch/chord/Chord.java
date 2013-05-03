/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch.chord;

import java.net.InetAddress;
import p2psearch.MiscUtil;

/**
 * The misc functions for Chord protocol
 * And the non-GUI version of Chord main method
 * 
 */
public class Chord {

    // take the first hashCharNum of character in the hash as its id    
    //the number of bits in the key/node identifiers.    
    //Identifiers are ordered in an identifier circle modulo 2 to the power of m
    public static final int m = 10;
    public static final int ServerPort = 7077;
    public static final boolean isLogNRouting = false;//logN or linear node routing. In another word, use the finger table or not.
    private static long maxKey = (long) Math.pow((double) 2, (double) m);

    public static long getIdentifier(Node node) {

        return Long.decode("0x" + MiscUtil.getHash(node.getNodeIP().getHostAddress()).substring(0, 10)) % maxKey;
    }

    public static long getIdentifier(String keyword) {

        return Long.decode("0x" + MiscUtil.getHash(keyword).substring(0, 10)) % maxKey;
    }

    /*
     * check if n1 is in between n2 and n3, in the identifier circle,clockwise direction.
     */
    public static boolean belongToRange(long n1, long n2, long n3) {
        if (n2 < n3) {
            if (n1 > n2 && n1 <= n3) {
                return true;
            } else {
                return false;
            }
        } else if (n2 > n3) {
            if (n1 > n2) {
                return true;
            } else if (n1 < n2) {
                if (n1 <= n3) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    
    //This can run a non-GUI version of p2p Chord layer.
    public static void main(String[] args) {
        Node myself = new Node();
        System.out.println("a non-GUI version of engine runs with p2p layer");
        Thread nodeThread = new Thread(myself);
        nodeThread.start();
    }
}
