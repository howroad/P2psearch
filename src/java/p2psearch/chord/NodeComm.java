/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch.chord;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import Configuration.DebugConfig;

/**
 * The proactive communication layer of Chord, that takes charge of all the remote inter-node communication for Chord functions
 * A lower layer than Node but higher than NodeServer
 */
class NodeComm implements Runnable {

    private NodeServer chordServer;
    private int port = Chord.ServerPort;
    private volatile Node myself;
    private boolean running;

    public NodeComm(int port, boolean running, Node myself) throws IOException {
        this.chordServer = new NodeServer(port, running, myself);
        this.port = port;
        this.running = running;
        this.myself = myself;
        DebugConfig.printAndLog(NodeComm.class.getName(), "Com Layer starting..");
        Thread serverThread = new Thread(chordServer);
        serverThread.start();
    }

    private Node connect(InetAddress nodeIP, int port, Node carrier) throws IOException {
        Node response = null;
        Node nodeMessage = null;

        Socket client = null;
        ObjectOutputStream oos = null;
        OutputStream outToServer = null;
        InputStream inFromServer = null;
        ObjectInputStream in = null;
        try {

            client = new Socket(nodeIP, port);
            outToServer = client.getOutputStream();
            oos = new ObjectOutputStream(outToServer);
            //built connection
            nodeMessage = carrier;
            oos.writeObject(nodeMessage);//send message

            inFromServer = client.getInputStream();
            in = new ObjectInputStream(inFromServer);
            response = (Node) in.readObject();//get response
            if (response == null) {
                return null;
            }
            DebugConfig.printAndLog(NodeComm.class.getName(), myself.nodeID+"Comm Layer : got response from : "
                    + response.myFinger.nodeID + " with message :" + response.message[0]);

        } catch (Exception ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "connect :", ex);
            return null;
        } finally {
            if (client != null) {

                if (oos != null) {
                    oos.close();
                }
                if (outToServer != null) {
                    outToServer.close();
                }
                if (inFromServer != null) {
                    inFromServer.close();
                }
                if (in != null) {
                    in.close();
                }
                client.close();
            }
        }
        return response;
    }

    protected InetAddress getMyIP() throws UnknownHostException {//get my ip address
        InetAddress ip = InetAddress.getLocalHost();
        return ip;
    }
// <editor-fold defaultstate="collapsed" desc=" get the successor of the specified ID. used in findSuccessor method ">

    Node getSuccessor(Finger remoteNode, long id) {
        String[] message = new String[2];
        message[0] = "getSuccessor";
        message[1] = Long.toString(id);
        Node carrier = new Node(myself.myFinger, message);
        try {
            Node response = connect(remoteNode.nodeIP, remoteNode.port, carrier);
            return response;
        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "getSuccessor", ex);
            return null;
        }
    }

    Node getSuccessor(Node sourceNode, long id) {
        String[] message = new String[2];
        message[0] = "getSuccessor";
        message[1] = Long.toString(id);
        Node carrier = new Node(myself.myFinger, message);
        try {
            return connect(sourceNode.myFinger.nodeIP, sourceNode.myFinger.port, carrier);


        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "getSuccessor", ex);
            return null;
        }
    }

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc=" mechanism to find an existing node ">
    Node getSourceNode() throws UnknownHostException, IOException {//use some mechanism to fetch an existing node in the grid
        String myAddr = getMyIP().getHostAddress();
        String[] LanAdd = myAddr.split(Pattern.quote("."));
        String subnet = LanAdd[0] + "." + LanAdd[1] + "." + LanAdd[2];
        int timeout = 1000;

        Node responseNode = null;
        for (int i = 1; i < 254; i++) {
            if (running == false) {
                return null;
            }
            String target = subnet + "." + i;
            DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer checking source node: " + target);


            try {
                //filter the closed ones
                if (InetAddress.getByName(target).isReachable(timeout) == false) {
                    continue;
                }
                DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer checking possible source node : " + target);
                InetAddress targetAddr = InetAddress.getByName(target);
                //skip my own address.
                if (targetAddr.equals(getMyIP())) {
                    continue;
                }
                //send a message to the possible source node to see if it is, or just happened to open that port.
                String[] message = new String[1];
                message[0] = "isNode";
                Node carrier = new Node(myself.myFinger, message);
                responseNode = connect(targetAddr, port, carrier);
                if (responseNode != null || responseNode.message[0].equals("sourceNodeReply")) {
                    break;
                }
            } catch (Exception ex) {
                continue;
            }
        }
        //if didn't find any node
        if (responseNode == null) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer found no source. Stand alone.");
        }

        return responseNode;
    }

// </editor-fold>
    void notifyAsPredecessor(Finger successor, Node me) {
        String[] message = new String[1];
        message[0] = "notifyAsPre";
        Node carrier = new Node(me.getNodeID(), me.myFinger.nodeIP, me.predecessor, me.successor, me.myFinger, message);
        try {
            connect(successor.nodeIP, successor.port, carrier);


        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer notifyAsPredecessor : couldn't notify", ex);
        }
    }

    boolean checkPredecessor(Node me) {//simply check if predecessor is alive
        String[] message = new String[1];
        message[0] = "isAlive";
        Node carrier = new Node(me.getNodeID(), me.myFinger.nodeIP, me.predecessor, me.successor, me.myFinger, message);
        try {
            Node response = connect(me.predecessor.nodeIP, me.predecessor.port, carrier);
            if (response == null) {
                return false;
            } else {
                return true;
            }
        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "CheckPredecessor", ex);
            return false;
        }

    }

    @Override
    public void run() {
        DebugConfig.printAndLog(NodeComm.class.getName(), "Com Layer starting..");
        Thread serverThread = new Thread(chordServer);
        serverThread.start();
    }

    Node getNode(Finger node) {//get the node presentation of that node
        String[] message = new String[1];
        message[0] = "getNode";
        Node carrier = new Node(myself.myFinger, message);
        try {
            Node response = connect(node.nodeIP, node.port, carrier);
            return response;
        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "getNode", ex);
            return null;
        }

    }

    String getSearchResult(Node resultHolder, String keyword, int page) {//communicate with that node to get the search result
        String[] message = new String[3];
        message[0] = "getSearch";
        message[1] = keyword;
        message[2] = Integer.toString(page);
        Node carrier = new Node(myself.myFinger, message);
        try {
            Node response = connect(resultHolder.nodeIP, resultHolder.myFinger.port, carrier);
            if (response.message[0].equals("SearchReply")) {
                return response.message[1];
            } else {
                DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer getSearchResult : error retrieving result");
                return null;
            }
        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "getSearchResult : error retrieving result", ex);
            return null;
        }

    }

    String getCache(Node resultHolder, String keyword, String cacheName) {//communicate with that node to get the cached page
        String[] message = new String[3];
        message[0] = "getCache";
        message[1] = keyword;
        message[2] = cacheName;
        Node carrier = new Node(myself.myFinger, message);
        try {
            Node response = connect(resultHolder.nodeIP, resultHolder.myFinger.port, carrier);
            if (response.message[0].equals("cacheReply")) {
                return response.message[1];
            } else {
                DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer getcacheResult : error retrieving cache");
                return null;
            }
        } catch (IOException ex) {
            DebugConfig.printAndLog(NodeComm.class.getName(), "Comm Layer getcacheResult : error retrieving cache", ex);
            return null;

        }

    }
}
