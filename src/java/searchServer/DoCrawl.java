/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package searchServer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import p2psearch.Crawler;
import p2psearch.chord.Node;
import Configuration.DebugConfig;

/**
 * This is the servlet for the crawling function.
 *
 */
public class DoCrawl extends HttpServlet {

    Thread crawlThread = null;
    Crawler crawler = null;
    String fileAbsPath = "";
    private Node myself = null;

// <editor-fold defaultstate="collapsed" desc=" init and destroy ">
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = getServletContext();

        //get the backend chord node
        myself = (Node) context.getAttribute("myself");
        fileAbsPath = (String) context.getAttribute("fileAbsPath");
    }

    @Override
    public void destroy() {
        if (crawlThread != null && crawlThread.getState() != Thread.State.TERMINATED) {
            crawler.terminate();
        }
    }

// </editor-fold>
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = null;
        String status = request.getParameter("status");//the parameter to see if user wants to see the crawling status
        String result = "";

        try {
            out = response.getWriter();
            try {
                //return the status checking interface
                if (status != null && status.equals("stop") && crawlThread.isAlive()) {
                    crawler.terminate();
                    result += ("<html>");
                    result += ("<head>");
                    result += ("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
                    result += ("<link href=\"bootstrap.css\" rel=\"stylesheet\">");
                    result += ("<title>Crawl stopped</title>");
                    result += ("</head>");
                    result += ("<body>");
                    result += ("<h1>         crawling stopped</h1 >");
                    result += ("<a href=\"/P2psearch/\" class=\"brand\">P2P search home</a> ");
                    result += ("</body>");
                    result += ("</html>");

                } else if (status != null && status.equals("status")) {
                    result = getCrawlStatus();
                } else //if no crawling is ongoing,do the crawling
                if (crawlThread == null || crawlThread.getState() == Thread.State.TERMINATED) {
                    String seed = request.getParameter("seed");
                    if (seed.contains("http://") == false) {
                        seed = "http://" + seed;
                    }
                    //filter, to see if that seed url really works
                    Jsoup.connect(seed).timeout(10000).get();

                    //get crawler thread number
                    int threadNum = Integer.parseInt(request.getParameter("threadNum"));
                    if (threadNum > 6) {
                        threadNum = 6;
                    }
                    //get domain filter
                    String domain = request.getParameter("domain");
                    //result is basically a page telling user that the crawl starts
                    result = startCrawl(seed, domain, threadNum);
                } else {
                    //divert to status interface
                    result = getCrawlStatus();
                }
            } catch (Exception ex) {
                String error = "sorry, something is wrong: " + ex.toString();
                out.println(error);
                return;
            }
            //output
            out.println(result);
        } catch (Exception ex) {
            ex.printStackTrace();

        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

// <editor-fold defaultstate="collapsed" desc=" crawl functions ">
    /**
     * Start the crawling
     *
     * @param seed The seed url to start crawling with
     * @param domain The domain filter for crawler to crawl only web pages that
     * have this domain name
     * @param threadNum The number of parser threads
     */
    private String startCrawl(String seed, String domain, int threadNum) {
        String result = "";

        crawler = new Crawler(seed, domain, threadNum, fileAbsPath, myself);
        crawlThread = new Thread(crawler);
        crawlThread.start();

        result += ("<html>");
        result += ("<head>");
        result += ("        <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        result += ("<link href=\"bootstrap.css\" rel=\"stylesheet\">");
        result += ("<title>Servlet DoCrawl</title>");
        result += ("</head>");
        result += ("<body>");
        result += ("<h1>         crawling...</h1 >");
        result += ("<h2>         " + seed + "</h2 >");
        result += ("<h2>         refresh to check the status    </h2 >");
        result += ("</body>");
        result += ("</html>");

        return result;

    }

    private String getCrawlStatus() {
        String result = "";
        if (crawler != null&&crawlThread.isAlive()) {
            long num = crawler.getPageCrawledNum();
            result += ("<html>");
            result += ("<head>");
            result += ("<link href=\"bootstrap.css\" rel=\"stylesheet\">");
            result += ("<title>Crawling status</title>");
            result += "<meta http-equiv=\"refresh\" content=\"5\" >";
            result += ("</head>");
            result += ("<body>");
            result += ("<h1>         Crawling Status</h1 >");
            result += ("<h2>         " + num + " pages have been crawled    </h2 >");
            result += "<div class=\"progress progress-striped active\">"
                    + "  <div class=\"bar\" style=\"width: "
                    + ((float) (num % 1000) / 6)
                    + "%;\"></div>"
                    + "</div>";
            result += "<div class=\"progress progress-striped active\">"
                    + "  <div class=\"bar\" style=\"width: "
                    + (float) (num / 6000)
                    + "%;\"></div>"
                    + "</div>";
            result += "<form  align=\"middle\" name=\"Crawl Sop Form\" action=\"doCrawl\">"
                    + "<input value=\"stop\" name=\"status\" hidden=\"true\"/>"
                    + "<input class=\"btn btn-info\" type=\"submit\" value=\"Stop\" />"
                    + "</form>"
                    + "<a href=\"/P2psearch/\" class=\"brand\">P2P search home</a> ";





            result += ("</body>");
            result += ("</html>");
        } else {
            result = "crawler not running";
        }
        return result;

    }
// </editor-fold>
}
