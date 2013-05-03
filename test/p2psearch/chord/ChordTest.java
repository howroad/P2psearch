/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2psearch.chord;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * The test for Chord misc functions.
 */
public class ChordTest {

    public ChordTest() {
    }

    /**
     * Test of getIdentifier method, of class Chord.
     */
    @Test
    public void testGetIdentifier_String() {
        System.out.println("getIdentifier");

        System.out.println(Chord.getIdentifier("504"));
        System.out.println(Chord.getIdentifier("is"));
        System.out.println(Chord.getIdentifier("not"));
        System.out.println(Chord.getIdentifier("helloworld"));
        System.out.println(Chord.getIdentifier(""));

    }

    /**
     * Test of belongToRange method, of class Chord.
     */
    @Test
    public void testBelongToRange() {
        System.out.println("\nbelongToRange");
        assertTrue(Chord.belongToRange((long) 30, (long) 90, (long) 80));//true
        assertTrue(Chord.belongToRange((long) 100, (long) 90, (long) 80));//true
        assertFalse(Chord.belongToRange((long) 85, (long) 90, (long) 80));//false
        assertFalse(Chord.belongToRange((long) 30, (long) 60, (long) 80));//false
        assertFalse(Chord.belongToRange((long) 100, (long) 60, (long) 80));//false
        assertTrue(Chord.belongToRange((long) 70, (long) 60, (long) 80));//true
        assertTrue(Chord.belongToRange((long) 30, (long) 80, (long) 80));//true

    }

}
