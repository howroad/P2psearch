
package p2psearch;

import com.google.common.collect.TreeMultimap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Map;
import p2psearch.chord.Node;
import Configuration.DebugConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author
 */
public class Searcher {

    private static final int DISPLAYNUM = 20;//number of results being displayed in one page
    private Node myself;
    private String fileAbsPath = "";

    public Searcher(Node myself, String absPath) {
        this.myself = myself;
        this.fileAbsPath = absPath;
    }
// <editor-fold defaultstate="collapsed" desc=" search for the keyword ">

    public String search(String keyword, int page) throws UnknownHostException{
        keyword=keyword.toLowerCase();
        if (myself == null || myself.takesChargeOf(keyword)) {
            return getResultFromLocal(keyword, page);
        } else {
            return myself.getSearchResult(keyword, page);//invoke the Chord layer to find the result from other machine
        }
    }

    private String getResultFromLocal(String keyword, int page) throws UnknownHostException  {
        String result = "";
        String keywordHash = MiscUtil.getHash(keyword);
        File postingListFile = new File(fileAbsPath + "pageRepo/keywords", keywordHash + "Tree");
        ObjectInputStream inStream = null;
        try {
            TreeMultimap<Integer, PageInfo> postingList;

// <editor-fold defaultstate="collapsed" desc=" result header ">



            //                                                                                                        1   2   3   4   5   6   7   8
            //                                                                                                        |   |
            result += "<div class=\"container\">";                                              //div------------------   |
            result += " <div class=\"header\">";                                                //div----------------------
            // <editor-fold defaultstate="collapsed" desc=" header ">
            result += "<a href=\"/P2psearch/\" class=\"brand\">P2P search home</a> "
                    + "<ul class=\"nav-bar pull-right\"> <li> <form class=\"navbar-search\" action=\"search\">"
                    + " <input type=\"text\" class=\"search-query\" placeholder=\"Search\" name=\"keyword\">"
                    + " <button class=\"btn btn-small btn-inverse\" type=\"submit\">"
                    + "<i class=\"icon-search icon-white\"></i>"
                    + "</button></form> </li>  </ul> ";

// </editor-fold>
            result += "</div>";                                                                 //div--------------------/2   3   4
            result += "<div class=\"content\">";                                                //div--------------------------   |
            result += "<div class=\"results-holder\">";                                         //                                |
            result += "<div class=\"page-header\">";                                            //div------------------------------
            result += "<div class=\"pagination pull-right\"> <ul> <script>document.write("
                    + "'<a class=\"active\" href=\"' + document.referrer + '\">Go Back</a>'"
                    + ");</script> </ul> </div>";

            inStream = new ObjectInputStream(new FileInputStream(postingListFile));
            postingList = (TreeMultimap<Integer, PageInfo>) inStream.readObject();
            int totalResultNum = postingList.size();
            int totalPageNum = totalResultNum / DISPLAYNUM + 1;
            result += "<h1>" + totalResultNum + " Results for <strong>";
            result += keyword+"</strong>";
            if(myself!=null)result += " from node "+myself.getNodeIP().getHostAddress();
            result+="</h1>";
            // </editor-fold>            

            if (postingList == null || page > totalPageNum) {
                result += "<div class=\"alert alert-block\"><h4 class=\"text-center\">Grr..</h4>"
                        + "<p class=\" text-center\">What's that? Please note that we aren't able to process keywords <= 3 letters </p></div>"
                        + "</div> </div></div>";
                return result;
            }
            result += "</div>";                                                                 //div----------------------------/4

            // <editor-fold defaultstate="collapsed" desc=" show results ">
            int items = 0;
            int displayStart = (page - 1) * DISPLAYNUM;

            result += "<ul class=\"search-results unstyled\">";

            for (Map.Entry<Integer, Collection<PageInfo>> entry : postingList.asMap().entrySet()) {
                for (PageInfo info : entry.getValue()) {
                    if (items >= displayStart + DISPLAYNUM) {
                        break;
                    } else {
                        items++;
                        if (items <= displayStart) {
                            continue;
                        }

                    }
                    result += "<li class=\"search-result\" > <div class=\"search-content\">";
                    result += "<b>" + (items) + "</b>";
                    result += "<a class=\"search-link\" href=\"" + info.url + "\">  <h3>" + info.title + "</h3> </a>";
                    result += "<a class=\"search-link\" href=\"" + info.url + "\">  <p class=\"text-success\">" + info.url + "</p> </a>";
                    result += "<p>Number of keyword occurrence : " + entry.getKey() + "</p>";
                    result += "<p><a class=\"search-link\" href=\"/P2psearch/search?keyword=" + keyword + "&amp;cache=" + info.docID + "\">cached</a></p>";
                    result += "  </div> </li>";
                    result += "<br><hr>";
                }
                if (items >= displayStart + DISPLAYNUM) {
                    break;
                }
            }
            result += "</ul>";

// </editor-fold>
            result += " <div class=\"search-footer\"> <div class=\"pagination pull-left\"> <ul>";

            if (page == 2) {
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page - 1) + "\">Previous</a> </li> ";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page - 1) + "\">" + (page - 1) + "</a> </li> ";

            } else if (page > 2) {
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page - 1) + "\">Previous</a> </li> ";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page - 2) + "\">" + (page - 2) + "</a> </li> ";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page - 1) + "\">" + (page - 1) + "</a> </li> ";

            }
            result += "<li class=\"active\"> <a href=\"#\">" + (page) + "</a> </li> ";


            if (page <= totalPageNum - 2) {
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page + 1) + "\">" + (page + 1) + "</a></li>";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page + 2) + "\">" + (page + 2) + "</a></li>";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page + 1) + "\">Next</a> </li>";
            } else if (page <= totalPageNum - 1) {
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page + 1) + "\">" + (page + 1) + "</a></li>";
                result += "<li class=\"\"> <a href=\"/P2psearch/search?keyword=" + keyword + "&amp;page=" + (page + 1) + "\">Next</a> </li>";
            }



            result += " </div></div></div></div>";

        } catch (FileNotFoundException ex) {
            result += "<h1>  Results for <strong>";
            result += keyword;
            result += "</strong></h1>";

            result += "<div class=\"alert alert-block\"><h4 class=\"text-center\">Grr..</h4>"
                    + "<p class=\" text-center\">What's that? Please note that we we aren't able to process keywords <= 3 letters </p><p text-align=\"right\">--Sincerely,node "+InetAddress.getLocalHost().getHostAddress()+"</p></div>"
                    + "</div> </div></div>";
            return result;
        } catch (Exception ex) {
            return ex.toString();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ex) {
                    DebugConfig.printAndLog(Searcher.class.getName(), "searcher:",ex);
                }
            }
        }
        return result;
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc=" get cached page file ">
    public String getCache(String keyword, String cacheName) throws FileNotFoundException, IOException {
        keyword=keyword.toLowerCase();
        if (myself == null || myself.takesChargeOf(keyword)) {
            return getCacheFromLocal(keyword, cacheName);
        } else {
            return myself.getCache(keyword, cacheName);
        }
    }

    private String getCacheFromLocal(String keyword, String cacheName) throws FileNotFoundException, IOException {
        File cacheFile = new File(fileAbsPath + "pageRepo/original/", cacheName + ".html");
        InputStream in = new FileInputStream(cacheFile);
        String result = "";
        if (in != null) {
            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(isr);
            String text = "";
            while ((text = reader.readLine()) != null) {
                result += text;
            }
        }
        return result;
    }

// </editor-fold>
}
