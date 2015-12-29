package one.util.streamtools;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.Test;

public class SplitTreeTest {
    @Test
    public void testSequential() {
        SplitTree tree = Stream.of(1, 2, 3).collect(SplitTree.collector());
        assertEquals(1, tree.nodes().count());
        assertEquals(1, tree.leafs().count());
        assertEquals(3, tree.leafs().mapToLong(SplitNode::getCount).sum());
        
        SplitNode node = tree.leafs().findFirst().get();
        
        assertSame(tree.root(), node);
        
        assertEquals("[1..3]", node.toString());
        
        assertTrue(node.isLeaf());
        
        assertNull(node.getLeft());
        
        assertNull(node.getRight());
        
        assertEquals(Arrays.asList("[1..3]"), tree.asLines());
    }
    
    @Test
    public void testEmulated() {
        long start = System.nanoTime();
        SplitNode left = new SplitNode();
        left.accept(1);
        left.accept(2);
        left.accept(3);
        SplitNode right = new SplitNode();
        right.accept(4);
        right.accept(5);
        right.accept(6);
        SplitNode combo = left.combine(right);
        SplitNode root = combo.combine(new SplitNode());
        SplitTree tree = new SplitTree(root);
        long end = System.nanoTime();
        assertEquals(5, tree.nodes().count());
        assertEquals("      [1..6]      \n"+
                    "      ___/\\___    \n"+
                    "     |        |   \n"+
                    "   [1..6]  (empty)\n"+
                    "    __/\\__        \n"+
                    "   |      |       \n"+
                    "[1..3] [4..6]     ",
                tree.toString());
        assertEquals(Arrays.asList(Thread.currentThread().getName()), tree.nodes().map(SplitNode::getThreadName)
                .distinct().collect(Collectors.toList()));
        assertTrue(tree.nodes().allMatch(node -> node.getStartNanos() >= start));
        assertTrue(tree.nodes().allMatch(node -> node.getEndNanos() >= node.getStartNanos()));
        assertTrue(tree.nodes().allMatch(node -> node.getEndNanos() <= end));
    }

    @Test
    public void testParallel() {
        SplitTree tree = Stream.of(1, 2).parallel().collect(SplitTree.collector());
        assertEquals(Arrays.asList("[1..2] ", "  _/\\  ", " |   | ", "[1] [2]"),
                tree.asLines());
        assertEquals("[1..2] \n" + "  _/\\  \n" + " |   | \n" + "[1] [2]",
                tree.toString());
    }
    
    @Test
    public void testBig() {
        assertEquals(10000, SplitTree.inspect(IntStream.range(0, 10000)).leafs().mapToLong(SplitNode::getCount).sum());
    }
    
    @Test
    public void testInspect()
    {
        assertEquals(2, SplitTree.inspect(IntStream.of(1, 2)).leafs().count());
        assertEquals(2, SplitTree.inspect(LongStream.of(1, 2)).leafs().count());
        assertEquals(2, SplitTree.inspect(DoubleStream.of(1, 2)).leafs().count());
        assertEquals(2, SplitTree.inspect(Stream.of(1, 2)).leafs().count());
    }
}
