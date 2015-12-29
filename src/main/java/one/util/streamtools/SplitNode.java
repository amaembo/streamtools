package one.util.streamtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents either intermediate or terminal split tree node
 * 
 * @author Tagir Valeev
 */
public class SplitNode {
    private String first, last;
    private SplitNode left, right;
    private long count;
    private final long start = System.nanoTime();
    private long end = start;
    private String threadName = Thread.currentThread().getName();

    void accept(Object obj) {
        if (first == null)
            first = obj.toString();
        else
            last = obj.toString();
        count++;
        end = System.nanoTime();
    }

    /**
     * @return number of elements collected in this node
     */
    public long getCount() {
        return count;
    }
    
    /**
     * @return the left child (or null for leaf node)
     */
    public SplitNode getLeft() {
        return left;
    }

    /**
     * @return the right child (or null for leaf node)
     */
    public SplitNode getRight() {
        return right;
    }

    /**
     * @return the name of the thread where this node was accumulated or combined
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * @return true if this node is a leaf (not the splitting node)
     */
    public boolean isLeaf() {
        return left == null;
    }

    /**
     * @return the result of System.nanoTime() when this node was created 
     */
    public long getStartNanos() {
        return start;
    }

    /**
     * @return the result of System.nanoTime() when the last operation was performed on this node 
     */
    public long getEndNanos() {
        return end;
    }

    SplitNode combine(SplitNode that) {
        SplitNode p = new SplitNode();
        p.threadName = Thread.currentThread().getName();
        p.count = this.count + that.count;
        p.first = first == null ? that.first : first;
        p.last = Stream.of(that.last, that.first, this.last, this.first).filter(Objects::nonNull).findFirst()
                .orElse(null);
        p.left = this;
        p.right = that;
        p.end = System.nanoTime();
        return p;
    }

    private static String pad(String s, int left, int len) {
        if (len == s.length())
            return s;
        char[] result = new char[len];
        Arrays.fill(result, ' ');
        s.getChars(0, s.length(), result, left);
        return new String(result);
    }
    
    private static int leftSpaces(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ')
                return i;
        }
        return s.length();
    }

    private static int rightSpaces(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(s.length()-1-i) != ' ')
                return i;
        }
        return s.length();
    }

    List<String> asLines() {
        String cur = toString();
        if (left == null) {
            return Collections.singletonList(cur);
        }
        List<String> l = left.asLines();
        List<String> r = right.asLines();
        int len1 = l.get(0).length();
        int len2 = r.get(0).length();
        
        int mid1 = len1/2;
        int mid2 = len2/2;

        if(l.size() > r.size()) {
            len1 -= Math.min(len2, Math.max(0, l.stream().limit(r.size()).mapToInt(SplitNode::rightSpaces).max().getAsInt() - 1));
        } else if(l.size() < r.size()) {
            len2 -= Math.min(len1, Math.max(0, r.stream().limit(l.size()).mapToInt(SplitNode::leftSpaces).max().getAsInt() - 1));
        }

        int totalLen = len1 + len2 + 1;
        
        int leftAdd = 0;
        if (cur.length() < totalLen) {
            cur = pad(cur, (totalLen - cur.length()) / 2, totalLen);
        } else {
            leftAdd = (cur.length() - totalLen) / 2;
            totalLen = cur.length();
        }
        List<String> result = new ArrayList<>();
        result.add(cur);

        char[] dashes = new char[totalLen];
        Arrays.fill(dashes, ' ');
        Arrays.fill(dashes, mid1 + leftAdd + 1, len1 + len2 + 1 - mid2 + leftAdd, '_');
        int mid = totalLen / 2;
        dashes[mid] = mid1 + leftAdd == mid ? '|' : '/';
        dashes[mid + 1] = len1 + len2 - mid2 + leftAdd == mid ? '|' : '\\';
        result.add(new String(dashes));

        Arrays.fill(dashes, ' ');
        dashes[mid1 + leftAdd] = '|';
        dashes[len1 + len2 + 1 - mid2 + leftAdd] = '|';
        result.add(new String(dashes));

        int maxSize = Math.max(l.size(), r.size());
        
        for (int i = 0; i < maxSize; i++) {
            String lstr = l.size() > i ? r.size() > i ? l.get(i).substring(0, len1) : l.get(i) : "";
            String rstr = r.size() > i ? l.size() > i ? r.get(i).substring(r.get(i).length() - len2) : r.get(i) : "";
            if(lstr.isEmpty())
                result.add(pad(rstr, leftAdd + len1 - rstr.length() + len2 + 1, totalLen));
            else
                result.add(pad(lstr + " " + rstr, leftAdd, totalLen));
        }
        return result;
    }

    @Override
    public String toString() {
        if (first == null)
            return "(empty)";
        else if (last == null)
            return "[" + first + "]";
        return "[" + first + ".." + last + "]";
    }
}