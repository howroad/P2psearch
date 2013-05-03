/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch;

import Configuration.DebugConfig;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * The test for crawler
 */
public class CrawlerTest {

    private File folder;

    public CrawlerTest() {
    }

    @Before
    public void setUp() {
        //delete the page repository created last time, and start crawling from scratch.
        folder = new File("pageRepo");
        DebugConfig.deleteFileForTest(folder);
    }

    @After
    public void tearDown() {
        DebugConfig.deleteFileForTest(folder);
    }

    /**
     * Test of run method, of class Crawler.
     */
    @Test
    public void testRun() throws InterruptedException {
        System.out.println("Crawler:test");
        String fileAbsPath = "";
        String seedUrl = "http://www.bu.edu";
        String domainFilter = "bu.edu";
        int threadNum = 2;
        System.out.println("Please go to the foler : " + folder.getAbsolutePath() + " to check the files crawled.");
        Crawler testCrawler = new Crawler(seedUrl, domainFilter, threadNum, fileAbsPath, null);
        Thread crawlerThread = new Thread(testCrawler);
        crawlerThread.start();

        int checkNum = 0;
        while (checkNum < 10) {
            System.out.println("crawled pages : " + testCrawler.getPageCrawledNum());
            Thread.sleep(1000 * 5);
            checkNum++;
        }
        testCrawler.terminate();
    }
}
