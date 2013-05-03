/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch.chord;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import Configuration.DebugConfig;

/**
 *
 * @author
 */
public class Node implements Serializable, Runnable {

    private transient static final boolean isLogNRouting = Chord.isLogNRouting;//logN or linear node routing. In another word, use the finger table or not.
    private transient static final long MaintainPeriod = 1000 * 60 * 1;//stabilize every ?? min
    private transient static final int m = Chord.m;//there're 2 to the power of m items on the keyword ID ring
    private transient volatile boolean running = true;//flag for upper layer to stop this running
    private transient NodeComm commLayer;               //lower layer for real communication between nodes
    private transient ArrayList<Finger> fingerTable = new ArrayList<Finger>(m + 1);// the finger table
    private transient int next = 1;//used to periodically update finger table
    private transient Timer timer;
    public transient String fileAbsPath;
    private transient boolean initDone = false;
    //data field of this node
    protected long nodeID;                                //my node ID
    protected InetAddress nodeIP;                       //my IP
    protected Finger predecessor;                       //the Finger data of my predecessor
    protected Finger successor;                         //the Finger data of my successor
    protected Finger myFinger;                          //the Finger data of myself
    protected String[] message;                         //optional field, used to send message in server-client communication

    public Node() {//used for instanciating the Node object for the current machine        
        running = true;
        next = 1;
    }

    public Node(Finger myFinger, String[] message) {
        this.myFinger = myFinger;
        this.message = message;
    }

    public Node(long nodeID, InetAddress nodeIP, Finger myFinger, String[] message) {
        this.nodeID = nodeID;
        this.nodeIP = nodeIP;
        this.myFinger = myFinger;
        this.message = message;
    }

    public Node(long nodeID, InetAddress nodeIP, Finger predecessor, Finger myFinger, String[] message) {
        this(nodeID, nodeIP, myFinger, message);
        this.predecessor = predecessor;
    }

    public Node(long nodeID, InetAddress nodeIP, Finger predecessor, Finger successor, Finger myFinger, String[] message) {
        this(nodeID, nodeIP, predecessor, myFinger, message);
        this.successor = successor;
    }
    /*
     * public interface
     * 
     */

    public InetAddress getNodeIP() {
        return nodeIP;
    }

    public long getNodeID() {
        return nodeID;
    }

    public void terminate() {
        timer.cancel();
        running = false;
    }
// <editor-fold defaultstate="collapsed" desc=" find successor ">
//--- finding a successor

    public Node findSuccessor(long id) {
        if (Chord.belongToRange(id, nodeID, successor.nodeID)) {
            String[] message = new String[1];
            message[0] = "getSuccessorReply";
            Node successorNode = new Node(successor.nodeID, successor.nodeIP, myFinger, successor, message);
            return successorNode;
        } else {
            Finger remoteNode;
            if (isLogNRouting) {
                remoteNode = closestPrecedingFinger(id);//logN, use Finger Table to determine the nearest node it knows.
            } else {
                remoteNode = successor;//linear,which doesn't use the Finger Table at all. just ask its successor, and its successor asks its successor, and so on.
            }
            return commLayer.getSuccessor(remoteNode, id);
        }
    }

    private Finger closestPrecedingFinger(long id) {

        for (int i = m; i > 0; i--) {
            Finger nodeAtIndex = fingerTable.get(i);
            if (nodeAtIndex.nodeID == -1) {
                continue;
            } else if (Chord.belongToRange(nodeAtIndex.nodeID, nodeID, id)) {
                return nodeAtIndex;
            }
        }
        return myFinger;
    }

// </editor-fold>
    public boolean takesChargeOf(String keyword) {
        long id = Chord.getIdentifier(keyword);
        if (initDone == false) {
            return true;
        }
        return Chord.belongToRange(id, predecessor.nodeID, nodeID);
    }

    public String getSearchResult(String keyword, int page) {
        long id = Chord.getIdentifier(keyword);
        //find the node that has the result
        Node resultHolder = findSuccessor(id);
        if (resultHolder == null) {
            DebugConfig.printAndLog(Node.class.getName(), "getSearchResult:no message received");
            return "no message received";
        } else {
            //get search result from that guy
            String result = commLayer.getSearchResult(resultHolder, keyword, page);
            System.out.println("resultHolder is " + resultHolder.myFinger.nodeIP);
            return result;
        }
    }

    public String getCache(String keyword, String cacheName) {
        long id = Chord.getIdentifier(keyword);
        Node resultHolder = findSuccessor(id);
        if (resultHolder == null) {
            DebugConfig.printAndLog(Node.class.getName(), "Node " + nodeID + " main Layer : getCache function got no message received");
            return "no message received";
        } else if (resultHolder.message[0].equals("getSuccessorReply") == false) {
            DebugConfig.printAndLog(Node.class.getName(), "getCache:message wrong");
            return resultHolder.message[0];
        } else {
            String result = commLayer.getCache(resultHolder, keyword, cacheName);
            return result;
        }
    }

    boolean processNotification(Node senderNode) {//being called by the predecessor node,and thus,the communication layer
        if (predecessor == null || Chord.belongToRange(senderNode.nodeID, predecessor.nodeID, nodeID)) {
            predecessor = senderNode.myFinger;
            DebugConfig.printAndLog(Node.class.getName(), "Node " + nodeID + " main Layer : Got notified : predecessor now is changed to : " + senderNode.nodeID);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {//thread 1
        initialize();
        joinGrid();//communicate with the grid
        timer = new Timer();
        if (running == false) {//check if running is false in case user stops it at this point
            return;
        }
        Maintanence maint = new Maintanence(this);
        while (initDone == false) {//wait until init is done
            try {
                Thread.sleep(1000 * 30);
            } catch (InterruptedException ex) {
                DebugConfig.printAndLog(Node.class.getName(), "Node " + nodeID + " main Layer run:", ex);
            }
        }
        if (running == false) {//check if running is false in case user stops it at this point
            return;
        }
        timer.scheduleAtFixedRate(maint, (long) 1000 * 30, MaintainPeriod);
        DebugConfig.printAndLog(Node.class.getName(), "Node " + nodeID + " Main layer: Chord built. Node ID=" + nodeID + " Node IP=" + nodeIP);

    }

    /*
     * private Chord helper functions and maintainence
     */
    // <editor-fold defaultstate="collapsed" desc=" private Chord helper functions and maintainence ">
/*
     * private Chord helper functions and maintainence
     * 
     * Some of them are error-prone because they strongly depend on that the FingerTable is well-built.
     * This is a big assumption.
     */
    private void initialize() {
        try {
            //create the active communication layer(including the server)
            DebugConfig.printAndLog(Node.class.getName(), "Initializing..");
            commLayer = new NodeComm(Chord.ServerPort, running, this);

            this.nodeIP = commLayer.getMyIP();
            //this is hardcoded ID assignment
            //node 1
            this.nodeID = Chord.getIdentifier(this);//nodeId is a hash. use ip as key for hashing

            myFinger = new Finger(0, nodeID);//finger of myself
            myFinger.setNodeAddr(nodeIP);
            myFinger.nodeID = this.nodeID;
            successor = new Finger(myFinger.nodeID, myFinger.nodeIP);//at first it is just myself
            successor.start = myFinger.start + 1;
            predecessor = new Finger(myFinger.nodeID, myFinger.nodeIP);//at first it is just myself
            fingerTable.add(0, myFinger);
            fingerTable.add(1, successor);
            for (int i = 2; i <= m; i++) {
                Finger fingerX = new Finger(i, nodeID);
                fingerX.nodeID = -1;
                fingerTable.add(i, fingerX);
            }//finger table now has nothing but the "start" field, and the nodes are all myself

            DebugConfig.printAndLog(Node.class.getName(), "Initialization done");
        } catch (Exception ex) {
            DebugConfig.printAndLog(Node.class.getName(), "Node " + nodeID + " main Layer: initialize: something wrong in initalization", ex);
        }

    }

    private void joinGrid() {
        DebugConfig.printAndLog(Node.class.getName(), "Joining Grid..");
        // <editor-fold defaultstate="collapsed" desc=" acuire source node ">
        Node sourceNode;
        try {
            sourceNode = commLayer.getSourceNode();
            if (sourceNode != null) {
                DebugConfig.printAndLog(Node.class.getName(), "Joining grid : got source node : " + sourceNode.nodeID + " from IP " + sourceNode.nodeIP.getHostAddress());
            }
        } catch (Exception ex) {
            DebugConfig.printAndLog(Node.class.getName(), "Joining grid: exception. Didn't get source node", ex);
            sourceNode = null;
        }

// </editor-fold>
        if (running == false) {
            return;
        }
        if (sourceNode != null) {
            Node successorNode = commLayer.getSuccessor(sourceNode, nodeID);
            successor = successorNode.myFinger;
            predecessor = new Finger(successorNode.predecessor.nodeID, successorNode.predecessor.nodeIP);
            DebugConfig.printAndLog(Node.class.getName(), "Joined grid : successor is " + successor.nodeID + "with IP : " + successor.nodeIP);
            DebugConfig.printAndLog(Node.class.getName(), "Joined grid : predecessor is " + predecessor.nodeID + "with IP : " + predecessor.nodeIP);
        }//get my successor by asking the source node
        else {//standing-alone
            DebugConfig.printAndLog(Node.class.getName(), "Joining grid : standing-alone mode.");
            successor = myFinger;
            predecessor = myFinger;
        }
        initDone = true;
    }

// </editor-fold>

    /*     
     *          maintanence Thread
     */
    //<editor-fold defaultstate="collapsed" desc="maintanence class">
    private class Maintanence extends TimerTask {

        private Node myself;

        Maintanence(Node me) {
            this.myself = me;
        }
        /*
         * maintainence functions
         */

        private void stabilize() {//called periodically
            if (successor == null) {
                DebugConfig.printAndLog(Node.class.getName(), "successor is null when stabilizing");
                return;
            } else if (successor.equals(myFinger)) {

                if (predecessor.equals(myFinger) == false) {
                    DebugConfig.printAndLog(Node.class.getName(), "Stabilizing : actually, now it's not standing-alone mode.");
                    successor = predecessor;
                    successor.start = nodeID + 1;
                    fingerTable.set(1, successor);
                } else {
                    DebugConfig.printAndLog(Node.class.getName(), "Stabilizing : standing-alone mode.");
                }
                return;
            } else {
                Node successorNode = commLayer.getNode(successor);//get the current node of successor, to see if there's any change.
                if (successorNode == null) {
                    DebugConfig.printAndLog(Node.class.getName(), "Stabilizing : successor node null");
                    successor = new Finger(myFinger.nodeID, myFinger.nodeIP);
                    return;
                }
                Finger myPotentialNewSuccessor = new Finger(successorNode.predecessor.nodeID, successorNode.predecessor.nodeIP);//the predecessor of this successor guy.

                if (myPotentialNewSuccessor.equals(myFinger)) {//doesn't change
                    DebugConfig.printAndLog(Node.class.getName(), "Stabilizing : no change.");
                    return;
                } else if (Chord.belongToRange(myPotentialNewSuccessor.nodeID, nodeID, successor.nodeID)) {//a new guy came in.
                    successor = myPotentialNewSuccessor;
                    successor.start = nodeID + 1;
                    fingerTable.set(1, successor);
                }
                notifySuccessor(successor);//see below. Tell the new successor that I think I'm your predecessor.
                DebugConfig.printAndLog(Node.class.getName(), "Stabilizing : get new successor Node ID : " + successor.nodeID);

            }
        }

        private void notifySuccessor(Finger successor) {//counterpart of the function above, for notify the successor that I think I'm its predecessor
            commLayer.notifyAsPredecessor(successor, myself);
            DebugConfig.printAndLog(Node.class.getName(), "Notifying Node " + successor.nodeID);
        }

        private void checkPredecessor() {//called periodically
            if (commLayer.checkPredecessor(myself) == false) {
                predecessor = myFinger;
            }
        }

        private void maintainFingerTable() {//called periodically, to check whether the table is not correct
            if (next >= m) {
                next = 1;
            } else {
                next++;
            }
            Finger targetFinger = fingerTable.get(next);
            Node neuFingerNode = findSuccessor(targetFinger.start);
            Finger neuFinger = new Finger(neuFingerNode.myFinger.nodeID, neuFingerNode.myFinger.nodeIP);
            neuFinger.start = targetFinger.start;
            neuFinger.port = targetFinger.port;

            fingerTable.set(next, neuFinger);
        }

        @Override
        public void run() {//thread 2
            if (running == false) {
                DebugConfig.printAndLog(Node.class.getName(), "Chord Maintanence Layer: Maintanence canceled");
                return;
            }
            stabilize();
            checkPredecessor();
            maintainFingerTable();
            if (predecessor == null || successor == null) {
                return;
            }
            DebugConfig.printAndLog(Node.class.getName(), "<<<Chord Maintanence Layer: Current status:>>>\n"
                    + "Node ID : " + nodeID + " Node IP : " + nodeIP
                    + "\n current Predecessor is " + predecessor.nodeID + " with IP :" + predecessor.nodeIP 
                    + " and successor is : " + successor.nodeID + " with IP" + successor.nodeIP);
        }
    }
    //</editor-fold>
}
