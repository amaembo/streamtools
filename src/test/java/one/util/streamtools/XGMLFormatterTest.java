package one.util.streamtools;

import java.io.FileOutputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.w3c.dom.Document;

public class XGMLFormatterTest {
    @Test
    public void testXGMLGen() throws Exception {
        SplitTree tree = createGraph();
        Document doc = new XGMLFormatter().asDocument(tree);

        DOMSource source = new DOMSource(doc);
        try (FileOutputStream stream = new FileOutputStream("genrange.xgml")) {
            StreamResult result = new StreamResult(stream);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        }
    }

    private SplitTree createGraph() {
        return SplitTree.inspect(IntStream.range(0, 7000));
//        SplitNode left = new SplitNode();
//        left.accept(1);
//        left.accept(2);
//        left.accept(3);
//        SplitNode right = new SplitNode();
//        right.accept(4);
//        right.accept(5);
//        right.accept(6);
//        SplitNode combo = left.combine(right);
//        SplitNode root = combo.combine(new SplitNode());
//        return new SplitTree(root);
    }
}
