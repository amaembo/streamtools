package one.util.streamtools;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Test;

public class SplitTreeTest {
    @Test
    public void testSequential() {
        assertEquals(Arrays.asList("[1..3]"), Stream.of(1, 2, 3).collect(SplitTree.collector()).asLines());
    }

    @Test
    public void testParallel() {
        assertEquals(Arrays.asList("[1..2] ", "  _/\\  ", " |   | ", "[1] [2]"),
                Stream.of(1, 2).parallel().collect(SplitTree.collector()).asLines());
        assertEquals("[1..2] \n" + "  _/\\  \n" + " |   | \n" + "[1] [2]",
                Stream.of(1, 2).parallel().collect(SplitTree.collector()).asString());
    }
}
