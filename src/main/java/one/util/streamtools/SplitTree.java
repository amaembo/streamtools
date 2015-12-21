package one.util.streamtools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class SplitTree {
	private final Node root;
	
	private SplitTree(Node root) {
		this.root = root;
	}

	private static class Node {
		private String first, last;
		private Node left, right;

		void accept(Object obj) {
			if (first == null)
				first = obj.toString();
			else
				last = obj.toString();
		}

		Node combine(Node that) {
			Node p = new Node();
			p.first = first == null ? that.first : first;
			p.last = Stream.of(that.last, that.first, this.last, this.first)
					.filter(Objects::nonNull).findFirst().orElse(null);
			p.left = this;
			p.right = that;
			return p;
		}

		String pad(String s, int left, int len) {
			if (len == s.length())
				return s;
			char[] result = new char[len];
			Arrays.fill(result, ' ');
			s.getChars(0, s.length(), result, left);
			return new String(result);
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
			Arrays.fill(dashes, len1 / 2 + leftAdd + 1, len1 + len2 / 2 + 1
					+ leftAdd, '_');
			int mid = totalLen / 2;
			dashes[mid] = '/';
			dashes[mid + 1] = '\\';
			result.add(new String(dashes));

			Arrays.fill(dashes, ' ');
			dashes[len1 / 2 + leftAdd] = '|';
			dashes[len1 + len2 / 2 + 1 + leftAdd] = '|';
			result.add(new String(dashes));

			int maxSize = Math.max(l.size(), r.size());
			for (int i = 0; i < maxSize; i++) {
				String lstr = l.size() > i ? l.get(i) : String.format("%"
						+ len1 + "s", "");
				String rstr = r.size() > i ? r.get(i) : String.format("%"
						+ len2 + "s", "");
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
	
	public List<String> asLines() {
		return root.asLines();
	}
	
	public String asString() {
		return String.join("\n", root.asLines());
	}
	
	public static Collector<Object, ?, SplitTree> collector() {
		return Collector.of(Node::new, Node::accept, Node::combine, SplitTree::new);
	}
}
