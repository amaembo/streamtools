package one.util.streamtools;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class SplitTree {
    private final SplitNode root;
    
    private static class NodeBox {
        SplitNode node = new SplitNode();
        
        void accept(Object obj) {
            node.accept(obj);
        }
        
        void combine(NodeBox other) {
            node = node.combine(other.node);
        }
        
        SplitTree tree() {
            return new SplitTree(node);
        }
    }

    SplitTree(SplitNode root) {
        this.root = root;
    }
    
    private static <T> Stream<T> flatTraverse(Stream<T> src, Function<T, Stream<T>> streamProvider) {
        return src.flatMap(t -> {
            Stream<T> result = streamProvider.apply(t);
            return result == null ? Stream.of(t) : Stream.concat(Stream.of(t), flatTraverse(result, streamProvider));
        });
    }
    
    public SplitNode root() {
        return root;
    }

    public Stream<SplitNode> nodes() {
        return flatTraverse(Stream.of(root), node -> node.isLeaf() ? null : Stream.of(node.getLeft(), node.getRight())); 
    }

    public Stream<SplitNode> leafs() {
        return nodes().filter(SplitNode::isLeaf); 
    }
    
    public List<String> asLines() {
        return root.asLines();
    }

    @Override
    public String toString() {
        return String.join("\n", root.asLines());
    }

    public static Collector<Object, ?, SplitTree> collector() {
        return Collector.of(SplitNode::new, SplitNode::accept, SplitNode::combine, SplitTree::new);
    }
    
    public static SplitTree inspect(Stream<?> stream) {
        return stream.parallel().collect(collector());
    }
    
    public static SplitTree inspect(IntStream stream) {
        return stream.parallel().collect(NodeBox::new, NodeBox::accept, NodeBox::combine).tree();
    }
    
    public static SplitTree inspect(LongStream stream) {
        return stream.parallel().collect(NodeBox::new, NodeBox::accept, NodeBox::combine).tree();
    }
    
    public static SplitTree inspect(DoubleStream stream) {
        return stream.parallel().collect(NodeBox::new, NodeBox::accept, NodeBox::combine).tree();
    }
}
