/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package searchServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import p2psearch.Searcher;
import p2psearch.chord.Node;

/**
 *
 * The servlet for search
 */
public class Search extends HttpServlet {

    private ServletContext context = null;
    private Node myself = null;
    private String fileAbsPath = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        context = getServletContext();

        //get the backend chord node
        myself = (Node) context.getAttribute("myself");
        fileAbsPath = (String) context.getAttribute("fileAbsPath");
    }

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
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String keyword = request.getParameter("keyword");
        String cache = request.getParameter("cache");
        String pageStr = request.getParameter("page");
        String result = "";
        int page = 0;

        try {
            if (keyword != null && cache == null) {
                if (pageStr == null) {
                    page = 1;
                } else {
                    page = Integer.parseInt(pageStr);
                }
                result = processSearch(keyword, page);
            } else if (keyword != null && cache != null) {
                result = processCache(keyword, cache);
            } else {
                result = "not a valid request I'm afraid:(";
            }
            out.println(result);
        } catch (Exception e) {
        } finally {
            out.close();
        }

    }

    protected String processSearch(String keyword, int page) throws UnknownHostException {

        String result = "";
        result += ("<html>");
        result += ("<head>");
        result += ("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        result += ("<link href=\"bootstrap.css\" rel=\"stylesheet\">");

        result += ("<title>search result</title>");
        result += ("</head>");
        result += ("<body>");

        Searcher searcher = new Searcher(myself, fileAbsPath);
        Calendar cal = new GregorianCalendar();
        double diff = cal.getTimeInMillis();
        result += searcher.search(keyword, page);
        diff = cal.getTimeInMillis() - diff;
        result += "<p> search time used about : " + diff + " ms</p>";
        result += ("</body>");
        result += ("</html>");

        return result;
    }

    protected String processCache(String keyword, String cacheName)
            throws ServletException, IOException {

        String result = "";
        Searcher searcher = new Searcher(myself, fileAbsPath);

        result = searcher.getCache(keyword, cacheName);
        return result;

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
}
