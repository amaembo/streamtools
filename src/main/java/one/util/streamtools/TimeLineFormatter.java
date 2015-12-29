package one.util.streamtools;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class TimeLineFormatter {
    public static List<String> format(SplitTree tree, long quantum) {
        long startTime = tree.leafs().mapToLong(SplitNode::getStartNanos).min().getAsLong();
        long endTime = tree.leafs().mapToLong(SplitNode::getEndNanos).max().getAsLong();
        Map<String, String> perThread = tree.leafs()
                .sorted(Comparator.comparingLong(SplitNode::getStartNanos))
                .collect(Collectors.groupingBy(SplitNode::getThreadName, formatTimeLine(quantum, startTime, endTime)));
        int maxLen = perThread.keySet().stream().mapToInt(String::length).max().getAsInt();
        return perThread.entrySet().stream()
                .map(entry -> String.format(Locale.ENGLISH, "%" + maxLen + "s : %s", entry.getKey(), entry.getValue()))
                .sorted().collect(Collectors.toList());
    }

    private static Collector<SplitNode, ?, String> formatTimeLine(long quantum, long startTime, long endTime) {
        int length = (int) ((endTime - startTime) / quantum) + 1;
        return Collector.of(() -> {
            char[] buf = new char[length];
            Arrays.fill(buf, ' ');
            return buf;
        }, (buf, node) -> {
            int start = (int) ((node.getStartNanos() - startTime) / quantum);
            if(buf[start] != ' ')
                start++;
            int end = (int) ((node.getEndNanos() - startTime) / quantum);
            if(end < start)
                end++;
            int l = end - start + 1;
            String startStr = "[" + node.getFirst();
            String endStr = node.getLast() + "]";
            if (startStr.length() + endStr.length() + 2 >= l)
                endStr = "]";
            String result;
            if (startStr.length() + endStr.length() + 2 >= l) {
                switch (l) {
                case 1:
                    result = "|";
                    break;
                case 2:
                    result = "[]";
                    break;
                case 3:
                    result = "[.]";
                    break;
                default:
                    result = startStr.substring(0, l - 3) + "..]";
                }
            } else {
                int mid = (l - startStr.length() - endStr.length()) / 2 + 2;
                int fin = l - startStr.length() - mid;
                result = String.format(Locale.ENGLISH, "%s%" + mid + "s%" + fin + "s", startStr, "..", endStr);
            }
            result.getChars(0, result.length(), buf, start);
        }, (buf1, buf2) -> {
            for (int i = 0; i < buf2.length; i++) {
                if (buf2[i] != ' ')
                    buf1[i] = buf2[i];
            }
            return buf1;
        }, String::new);
    }
}
