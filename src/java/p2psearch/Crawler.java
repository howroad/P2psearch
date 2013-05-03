/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import p2psearch.chord.Node;
import Configuration.DebugConfig;

/**
 *
 * @author
 */
public class Crawler implements Serializable, Runnable {

    private static final int QueueStorePeriod = 2000;//every 2000 pages store the linkToCrawl and linkCrawled
    private String fileAbsPath = "";
    private volatile boolean running = true;//running flag, for termination
    private volatile long pageCrawledNum = 0;//the number of pages crawled
    private int parserThreadNum;
    private String seedUrl;// the seed Url
    private String domainFilter;//domain filter, in order to avoid crawling to externel website links.
    private ConcurrentSkipListSet<String> linkToCrawl;
    private ConcurrentSkipListSet<String> linkCrawled;
    private HashSet<String> stopList = new HashSet<String>();//list for common words that won't be crawled
    private volatile Node myself;//the Chord p2p layer instance

    public Crawler(String seed, String domain, int threadNum, String fileAbsPath, Node node) {
        this.seedUrl = seed;
        this.domainFilter = domain;
        this.parserThreadNum = threadNum;
        this.fileAbsPath = fileAbsPath;
        myself = node;
        this.running = true;
    }

    public void terminate() {
        DebugConfig.printAndLog(Crawler.class.getName(), "Crawler terminating...");
        running = false;
    }

    @Override
    public void run() {
        initialize();

        Thread[] parserThread = new Thread[parserThreadNum];
        //array of done flags
        boolean[] threadDoneFlag = new boolean[parserThreadNum];
        try {
            //create parser threads
            for (int i = 0; i < parserThreadNum; i++) {
                threadDoneFlag[i] = false;
                Parser parser = new Parser(threadDoneFlag, i);// See at the bottom the Parser class
                parserThread[i] = new Thread(parser);
                parserThread[i].start();
            }
            //wait until they are done. So that the DoCrawl servlet will know the correct status of the crawling process.
            for (Thread t : parserThread) {
                t.join();
            }
            System.out.println("Crawler stopped");
        } catch (InterruptedException ex) {
        }
    }

    public long getPageCrawledNum() {
        return pageCrawledNum;
    }

// ********************  private helper functions *****************************
    private void initialize() {
// <editor-fold defaultstate="collapsed" desc=" initialization ">

        //initialize folders
        fillStopList();
        File pageFolder = new File(fileAbsPath + "pageRepo/original");
        if (pageFolder.exists() == false) {
            pageFolder.mkdirs();
        }
        File postingListFolder = new File(fileAbsPath + "pageRepo/keywords");
        if (postingListFolder.exists() == false) {
            postingListFolder.mkdirs();
        }
        File assetsFile = new File(fileAbsPath + "pageRepo/crawlerAssets");
        if (assetsFile.exists() == false) {
            assetsFile.mkdirs();
        };

        //load the queues. if they are already there, it might be because of an unfinished crawling earlier
        try {
            File linkToCrawlFile = new File(fileAbsPath + "pageRepo/crawlerAssets", "linktoCrawl");
            File linkCrawledFile = new File(fileAbsPath + "pageRepo/crawlerAssets", "linkCrawled");
            if (linkToCrawlFile.exists() == false) {
                linkToCrawl = new ConcurrentSkipListSet<String>();
                linkToCrawl.add(seedUrl);
            } else {
                ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(linkToCrawlFile));
                linkToCrawl = (ConcurrentSkipListSet<String>) inStream.readObject();
                inStream.close();
            }
            if (linkCrawledFile.exists() == false) {
                linkCrawled = new ConcurrentSkipListSet<String>();
            } else {
                ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(linkCrawledFile));
                linkCrawled = (ConcurrentSkipListSet<String>) inStream.readObject();
                inStream.close();
            }
        } catch (Exception ex) {
            linkToCrawl = new ConcurrentSkipListSet<String>();
            linkCrawled = new ConcurrentSkipListSet<String>();
            linkToCrawl.add(seedUrl);
        }

// </editor-fold>
    }

    //used to determine whether this keyword belongs to this node,when crawling.
    private boolean takesChargeOf(String keyword) {
// <editor-fold defaultstate="collapsed" desc=" basic keyword filtering ">
        if (keyword.length() < 3) {
            return false;
        }
        for (int ii = 0; ii < keyword.length(); ii++) {
            if (Character.isLetterOrDigit(keyword.charAt(ii)) == false) {
                return false;
            }
        }

// </editor-fold>

        //if there's no p2p layer, just always return true
        if (myself == null) {
            return true;
        } else {
            //call the lower level Chord algorithm to determine if this node takes charge of the keyword
            return myself.takesChargeOf(keyword);
        }
    }

    //get the text of the page, for keywords extracting
    private String getPageText(Document currentPage) {
        Element pageBody = currentPage.body();
        String pageText = pageBody.text();
        pageText = pageText.toLowerCase();// get all keyword texts
        pageText = pageText.replaceAll("\n\r ", " ");
        return pageText;
    }

    //store the posting list into disk
    private void storePostingList(String fileName, String title, String url, String pageText) throws IOException {
// <editor-fold defaultstate="collapsed" desc=" store the posting list for the keywords in the page ">
        PageInfo currentPageInfo = new PageInfo(fileName, title, url);
        //strip keywords from the text
        String[] keywordsString;
        keywordsString = pageText.split(" ");

        HashSet<String> keywords = new HashSet<String>(Arrays.asList(keywordsString));//used to get each keyword
        HashMultiset<String> keywordsMulti = HashMultiset.create(Arrays.asList(keywordsString));//used to determine counts

        /*
         * for each keyword, create/get its postinglist, which is a list of all the pages that contain this keyword
         * and then update the postinglist to include the current page into it
         * requires synchronized file accessing
         */
        for (String keyword : keywords) {
            // <editor-fold defaultstate="collapsed" desc=" create/get the keyword postinglist and update ">
            if (stopList.contains(keyword) || takesChargeOf(keyword) == false) {
                continue;
            }

            String keywordHash = MiscUtil.getHash(keyword);
            String urlHash = MiscUtil.getHash(url);
            TreeMultimap<Integer, PageInfo> postingList = null;
            HashSet<String> keywordUrlSet = null;
            ObjectInputStream postingListInStream = null;
            ObjectInputStream keywordUrlSetInStream = null;

            File postingListFile = new File(fileAbsPath + "pageRepo/keywords", keywordHash + "Tree");
            File keywordUrlSetFile = new File(fileAbsPath + "pageRepo/keywords", keywordHash + "Set");

            synchronized (this) {
                //get the postingList from file or create a new one
                try {
                    if (postingListFile.exists()) {

                        postingListInStream = new ObjectInputStream(new FileInputStream(postingListFile));
                        keywordUrlSetInStream = new ObjectInputStream(new FileInputStream(keywordUrlSetFile));

                        postingList = (TreeMultimap<Integer, PageInfo>) postingListInStream.readObject();
                        keywordUrlSet = (HashSet<String>) keywordUrlSetInStream.readObject();

                    } else {
                        postingList = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
                        keywordUrlSet = new HashSet<String>();
                    }
                } catch (Exception ex) {
                    DebugConfig.printAndLog(Crawler.class.getName(), "storePostingList:", ex);
                    postingList = TreeMultimap.create(Ordering.natural().reverse(), Ordering.natural());
                    keywordUrlSet = new HashSet<String>();

                } finally {
                    if (postingListInStream != null) {
                        postingListInStream.close();
                    }
                    if (keywordUrlSetInStream != null) {
                        keywordUrlSetInStream.close();
                    }
                }
                //put the keyword and page info into the postingList
                if (keywordUrlSet.contains(urlHash)) {
                    continue;
                } else {
                    int keyOccur = keywordsMulti.count(keyword);
                    postingList.put(keyOccur, currentPageInfo);
                    keywordUrlSet.add(urlHash);
                }
                //store the postingList
                ObjectOutputStream listOutStream = null;
                ObjectOutputStream setOutStream = null;
                try {
                    listOutStream = new ObjectOutputStream(new FileOutputStream(postingListFile));
                    setOutStream = new ObjectOutputStream(new FileOutputStream(keywordUrlSetFile));
                    listOutStream.writeObject(postingList);
                    setOutStream.writeObject(keywordUrlSet);
                } catch (Exception ex) {
                    DebugConfig.printAndLog(Crawler.class.getName(), "storePostingList:", ex);
                } finally {
                    if (listOutStream != null) {
                        listOutStream.close();
                    }
                    if (setOutStream != null) {
                        setOutStream.close();
                    }
                }

            }

// </editor-fold>            
        }

    }
// <editor-fold defaultstate="collapsed" desc=" load&store other data structures ">

    //store the page for cached view
    private void storeOriginalHTML(Document currentPage, String fileName) {
        try {
            String content = currentPage.html();
            File originalFile = new File(fileAbsPath + "pageRepo/original", fileName + ".html");               // used for File index
            BufferedWriter originalOut = new BufferedWriter(new FileWriter(originalFile));
            originalOut.write(content);
            originalOut.close();

        } catch (Exception ex) {
            DebugConfig.printAndLog(Crawler.class.getName(), "storeOriginalHTML:", ex);
        }
    }

    //store the queues for BFS
    private void storeCurrentQueues() throws IOException {

        File linkToCrawlFile = new File(fileAbsPath + "pageRepo/crawlerAssets", "linktoCrawl");
        File linkCrawledFile = new File(fileAbsPath + "pageRepo/crawlerAssets", "linkCrawled");

        ObjectOutputStream toCrawlOut = null;
        ObjectOutputStream crawledOut = null;

        try {
            toCrawlOut = new ObjectOutputStream(new FileOutputStream(linkToCrawlFile));
            crawledOut = new ObjectOutputStream(new FileOutputStream(linkCrawledFile));
            toCrawlOut.writeObject(linkToCrawl);
            crawledOut.writeObject(linkCrawled);
        } catch (Exception ex) {
            DebugConfig.printAndLog(Crawler.class.getName(), "storeCurrentQueues:", ex);
        } finally {
            if (toCrawlOut != null) {
                toCrawlOut.close();
            }
            if (crawledOut != null) {
                crawledOut.close();
            }
        }

    }

    //stop words. We decide not to provide searching for 3 letter word, so those are commented out.
    private void fillStopList() {
//        stopList.add("and");
//        stopList.add("or");
//        stopList.add("a");
//        stopList.add("bu");
//        stopList.add("edu");
//        stopList.add("an");
//        stopList.add("an");
//        stopList.add("no");
//        stopList.add("not");
//        stopList.add("as");
//        stopList.add("of");
//        stopList.add("off");
//        stopList.add("is");
//        stopList.add("are");
//        stopList.add("we");
//        stopList.add("i");
//        stopList.add("you");
        stopList.add("they");
        stopList.add("them");
//        stopList.add("on");
        stopList.add("this");
//        stopList.add("to");
//        stopList.add("the");
//        stopList.add("for");
//        stopList.add("we");
//        stopList.add("in");
//        stopList.add("at");
//        stopList.add("its");
//        stopList.add("but");
        stopList.add("with");
        stopList.add("what");
//        stopList.add("who");
        stopList.add("some");
        stopList.add("here");
//        stopList.add("get");

    }

// </editor-fold>
    /*
     * the Parser class takes charge of the actual crawling and parsing.
     * By making it a separate class the crawler would be able to run multithreading parsing.
     */
    private class Parser implements Runnable {

        private volatile boolean[] threadParseDoneFlags;
        private int threadID;

        public Parser(boolean[] doneFlags, int id) {
            this.threadParseDoneFlags = doneFlags;
            this.threadID = id;

        }

        //process the current page, write it to the file
        //extract all the links from one web page and put them to the queue
        private boolean parse(String url) {

            Document currentPage;
            //if url is already crawled, return
            if (linkCrawled.contains(url) == true) {
                return false;
            }
            try {
                currentPage = Jsoup.connect(url).timeout(10000).get();
                Elements links = currentPage.select("a[href]");
// <editor-fold desc=" add all the links in the current page to the linkToCrawl list ">
                for (Element link : links) {
                    String newUrl = link.attr("abs:href");
                    if (newUrl.contains("#") || newUrl.contains("?") || newUrl.contains("mailto") || newUrl.contains("jpg") || newUrl.contains(domainFilter) == false || linkToCrawl.contains(newUrl) || linkCrawled.contains(newUrl)) {
                        continue;
                    } else {
                        linkToCrawl.add(newUrl);
                    }
                }

// </editor-fold>

// <editor-fold desc=" process the page itself ">
                //Use md5 of the url as the file name
                String fileName = MiscUtil.getHash(url);
                String title = currentPage.title();
                String pageText = getPageText(currentPage);

                storePostingList(fileName, title, url, pageText);
                //write the html for offline caching
                storeOriginalHTML(currentPage, fileName);

// </editor-fold>

            } catch (org.jsoup.UnsupportedMimeTypeException e) {
                return false;
            } catch (org.jsoup.HttpStatusException e) {
                return false;
            } catch (Exception ex) {
                DebugConfig.printAndLog(Crawler.class.getName(), "parse:", ex);
            }

            linkCrawled.add(url);
//            DebugConfig.printAndLog(Crawler.class.getName(), "added " + url);  //if this is uncommented, it will show the url of the page crawled
            return true;
        }

        @Override
        public void run() {
            boolean allDone = false;
            int storeNum = 0;//number of pages since last store-queues. only for thread 0.
            DebugConfig.printAndLog(Crawler.class.getName(), "thread " + threadID + "starts parsing");

            try {
                while (running == true) {

                    allDone = true;
                    // <editor-fold defaultstate="collapsed" desc=" Parsing loop ">
                    while (running == true && linkToCrawl.isEmpty() == false) {
                        //every once in a while store the queues
                        if (threadID == 0) {
                            storeNum++;
                            if (storeNum >= QueueStorePeriod) {
                                storeNum = 0;
                                storeCurrentQueues();
                            }
                        }
                        //get one address from the pool and crawl
                        String currentUrl = linkToCrawl.pollLast();
                        try {
                            //----------the core part --------------
                            if (parse(currentUrl)) {
                                pageCrawledNum++;
                                System.out.println(currentUrl);
                            }
                        } catch (Exception ex) {
                            DebugConfig.printAndLog(Crawler.class.getName(), "Parser run:", ex);
                        }
                    }

// </editor-fold>
                    //If reaches here, that means the link pool is empty, and it is done with the current parsing. 
                    //But chances are that other parsers may generate more links
                    //So it has to check if all threads are done, otherwise go to the parsing loop again.
                    threadParseDoneFlags[threadID] = true;
                    for (boolean threadDoneFlag : threadParseDoneFlags) {
                        if (threadDoneFlag == false) {
                            allDone = false;
                            break;
                        }
                    }
                    if (allDone) {
                        DebugConfig.printAndLog(Crawler.class.getName(), "crawler thread " + threadID + " has done crawling");
                        if (threadID == 0) {
                            storeCurrentQueues();
                        }
                        return;
                    }
                }
            } catch (Exception ex) {
                DebugConfig.printAndLog(Crawler.class.getName(), "Parser run:", ex);
            }
        }
    }
}
