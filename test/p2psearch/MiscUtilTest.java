/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * The test for misc functions
 */
public class MiscUtilTest {
    
    public MiscUtilTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getHash method, of class MiscUtil.
     */
    @Test
    public void testGetHash() {
        System.out.println("getHash test");
        String plain = "504";
        String result = MiscUtil.getHash(plain);
        String plain2 = "is";
        String result2 = MiscUtil.getHash(plain2);
        String plain3 = "not";
        String result3 = MiscUtil.getHash(plain3);
        String plain4 = "hello world";
        String result4 = MiscUtil.getHash(plain4);
        String plain5 = "";
        String result5 = MiscUtil.getHash(plain5);
        System.out.println(result);
        System.out.println(result2);
        System.out.println(result3);
        System.out.println(result4);
        System.out.println(result5);

    }
}
