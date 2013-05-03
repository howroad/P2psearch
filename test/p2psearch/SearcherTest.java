/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch;

import Configuration.DebugConfig;
import java.io.File;
import java.util.LinkedList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * The test for searcher. But because of the visualization nature of searching, it's a little bit inconvenient to test without a web application.
 */
public class SearcherTest {

    private File folder;
    private Crawler testCrawler;

    public SearcherTest() {
    }

    @Before
    public void setUp() {
        folder = new File("pageRepo");
        DebugConfig.deleteFileForTest(folder);
        String fileAbsPath = "";
        String seedUrl = "http://www.bu.edu";
        String domainFilter = "bu.edu";
        int threadNum = 2;

        testCrawler = new Crawler(seedUrl, domainFilter, threadNum, fileAbsPath, null);
        Thread crawlerThread = new Thread(testCrawler);
        crawlerThread.start();
    }

    @After
    public void tearDown() {
        testCrawler.terminate();
        DebugConfig.deleteFileForTest(folder);
    }

    /**
     * Test of search method, of class Searcher.
     * This is just a simple test. Please use the web application for more testing.
     */
    @Test
    public void testSearch() {
        System.out.println("search::Searcher test");
        Searcher instance = new Searcher(null, "");
        LinkedList<String> keywordPool = new LinkedList<String>();
        keywordPool.add("Boston");
        keywordPool.add("boston");
        keywordPool.add("bos");
        keywordPool.add("");
        keywordPool.add("123)(*)");

        while (keywordPool.isEmpty() == false) {
            try {

                Thread.sleep(1000 * 5);

                String keyword = keywordPool.pollFirst();
                int page = (int) Math.random() * 3;
                String result = instance.search(keyword, page);
                System.out.println("result for " + keyword + "\n");
                System.out.println(result);

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
    }
}
