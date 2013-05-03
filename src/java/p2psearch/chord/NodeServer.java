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
import java.net.ServerSocket;
import java.net.Socket;
import p2psearch.Searcher;

import Configuration.DebugConfig;

/**
 * The server layer for Chord protocol, which serves all the communication coming from other nodes.
 * The lowest layer in this Chord protocol implementation.
 * 
 */
public class NodeServer implements Runnable {

    private volatile Node myself;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public NodeServer(int port, boolean running, Node myself) throws IOException {
        serverSocket = new ServerSocket(port);
        this.running = running;
        this.myself = myself;
    }

    @Override
    public void run() {        
        DebugConfig.printAndLog(NodeServer.class.getName(), "Server Layer starting..");
        while (true) {
            if (running == false) {                
                DebugConfig.printAndLog(NodeServer.class.getName(), "Chord Node Server layer terminated");
                return;
            }
            try {
                Socket server = serverSocket.accept();
                NodeHandler handler = new NodeHandler(server);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();

            } catch (Exception ex) {                
                DebugConfig.printAndLog(NodeServer.class.getName(), null,ex);
            }
        }
    }

    private class NodeHandler implements Runnable {

        private Socket server = null;

        NodeHandler(Socket server) {
            this.server = server;
        }

        @Override
        public void run() {
            InputStream is = null;
            ObjectInputStream ois = null;
            OutputStream os = null;
            ObjectOutputStream oos = null;
            try {
                is = server.getInputStream();
                ois = new ObjectInputStream(is);
                Node nodeMessage = (Node) ois.readObject();
                Node response = null;
                if (nodeMessage != null) {
//                    if(nodeMessage.myFinger.equals(myself.myFinger)){
//                        response=null;
//                    }
                   
                    DebugConfig.printAndLog(NodeServer.class.getName(), "The Server Layer of "+myself.nodeID+" got message from : "
                            +nodeMessage.myFinger.nodeID+"  :"+nodeMessage.message[0]);
                    if (nodeMessage.message[0].equals("getSuccessor")) {
                        long id = Long.parseLong(nodeMessage.message[1]);
                        response = myself.findSuccessor(id);                                                
                    } else if (nodeMessage.message[0].equals("notifyAsPre")) {
                        boolean processed=myself.processNotification(nodeMessage);
                        String[]message = new String[1];
                        message[0]="Predecessor"+myself.nodeIP+"notificationProcessed:you are my "+processed+" predecessor";
                        
                        response = new Node(myself.myFinger,message);
                    } else if (nodeMessage.message[0].equals("isAlive")) {
                        String[]message = new String[1];
                        message[0]="Predecessor"+myself.nodeIP+"is alive";
                        
                        response = new Node(myself.myFinger,message);
                    } else if (nodeMessage.message[0].equals("isNode")) {
                        String[]message = new String[1];
                        message[0]="sourceNodeReply";
                        response = new Node(myself.nodeID,myself.nodeIP,myself.myFinger,message);
                    } else if (nodeMessage.message[0].equals("getNode")) {
                        String[]message = new String[1];
                        message[0]="getNodeReply";
                        if(myself.predecessor==null){
                            myself.predecessor=myself.myFinger;
                        }
                        response = new Node(myself.nodeID,myself.nodeIP,myself.predecessor,myself.successor,myself.myFinger,message);
                    } else if (nodeMessage.message[0].equals("getSearch")) {
                        String keyword = nodeMessage.message[1];
                        int page = Integer.parseInt(nodeMessage.message[2]);                        
                        String []message = new String[2];
                        message[0]="SearchReply";
                        Searcher searcher = new Searcher(myself,myself.fileAbsPath);
                        message[1]=searcher.search(keyword, page);
                        response=new Node(myself.myFinger,message);
                    } else if (nodeMessage.message[0].equals("getCache")) {
                        String keyword = nodeMessage.message[1];
                        String cacheName = nodeMessage.message[2];                        
                        
                        String []message = new String[2];
                        message[0]="cacheReply";
                        Searcher searcher = new Searcher(myself,myself.fileAbsPath);
                        message[1]=searcher.getCache(keyword, cacheName);
                        response=new Node(myself.myFinger,message);                        
                    }

                }
                os = server.getOutputStream();
                oos = new ObjectOutputStream(os);
                oos.writeObject(response);

            } catch (Exception ex) {                
                 DebugConfig.printAndLog(NodeServer.class.getName(),null,ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                        ois.close();
                        if(os!=null)os.close();
                        if(oos!=null)oos.close();
                        server.close();
                    } catch (IOException ex) {
                         DebugConfig.printAndLog(NodeServer.class.getName(),null,ex);
                    }
                }

            }
        }
        
        
        
        
    }
}
