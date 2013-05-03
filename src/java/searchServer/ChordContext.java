package searchServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import p2psearch.chord.Chord;
import p2psearch.chord.Node;
import Configuration.DebugConfig;

/**
 * This is the class for the context of the P2psearch server. And the funtion of
 * this class is to initiate the service before user does anything. And things
 * to initiate is mainly the P2p layer. The node of this machine is created and
 * initialized.
 *
 */
public class ChordContext implements ServletContextListener {

    //the Chord instance
    private Node myself;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();
        myself = null;
        boolean DEBUG = DebugConfig.DEBUG;
        String fileAbsPath = "";

        //create the Chord backend protocol system
        if (DEBUG == false) {
            DebugConfig.printAndLog(ChordContext.class.getName(), "Product mode. Engine runs with p2p layer");
            myself = new Node();
            Thread nodeThread = new Thread(myself);
            nodeThread.start();
        } else {
            DebugConfig.printAndLog(ChordContext.class.getName(), "Debug mode. Engine runs standing alone");
            myself = null;
        }
        context.setAttribute("myself", myself);

        //set the file absolute path, depending on the IP of the node.
        String myInetAddr;
        try {
            myInetAddr = InetAddress.getLocalHost().getHostAddress();
            fileAbsPath = context.getRealPath("/") + myInetAddr;
        } catch (UnknownHostException ex) {
            DebugConfig.printAndLog(ChordContext.class.getName(), "couldn't get host address:", ex);
        }
        if (myself != null) {
            myself.fileAbsPath = fileAbsPath;
        }
        context.setAttribute("fileAbsPath", fileAbsPath);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        if (myself != null) {
            myself.terminate();
        }
        DebugConfig.printAndLog(ChordContext.class.getName(), "P2P engine Context Destroyed");
    }


}
