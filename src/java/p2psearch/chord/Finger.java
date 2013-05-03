/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch.chord;

import java.io.Serializable;
import java.net.InetAddress;

/**
 *
 * @author 
 */
public class Finger implements Serializable {

    public int port = Chord.ServerPort;
    public transient long start;
    public InetAddress nodeIP;
    public long nodeID;

    Finger(int i, long myNodeID) {
        if (i != 0) {
            this.start = (long) (myNodeID + Math.pow(2, (double) (i - 1)));
        } else {
            this.start = myNodeID;//not accurate,avoid using this.
        }
    }

    Finger(long nodeID,InetAddress nodeIP){
        this.nodeID=nodeID;
        this.nodeIP=nodeIP;
    }
    public void setNodeAddr(InetAddress nodeAddr) {
        this.nodeIP = nodeAddr;
    }
    
       public boolean equals(Finger fin){
           if(fin.nodeID==this.nodeID){
               return true;
           }else{
               return false;
           }
       }

}
