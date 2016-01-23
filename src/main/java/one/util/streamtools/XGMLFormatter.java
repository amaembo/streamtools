package one.util.streamtools;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XGMLFormatter {
    private int nodeWidth = 100, nodeHeight = 30;
    private String format = "[%f..%l]\nSize: %c";
    private String empty = "(empty)";
    
    private static class GraphNode {
        int x, y, w, h;
        int id;
        String color;
        GraphNode l, r;
        String text;

        GraphNode(int x, int y, int w, int h, String text) {
            super();
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
        }

        Element xmlNode(Document doc) {
            Element node = section(doc, "node");
            node.appendChild(attribute(doc, "id", "int", String.valueOf(id)));
            node.appendChild(attribute(doc, "label", "String", text));

            Element graphics = section(doc, "graphics");
            graphics.appendChild(attribute(doc, "x", "double", String.valueOf(x)));
            graphics.appendChild(attribute(doc, "y", "double", String.valueOf(y)));
            graphics.appendChild(attribute(doc, "w", "double", String.valueOf(w)));
            graphics.appendChild(attribute(doc, "h", "double", String.valueOf(h)));
            graphics.appendChild(attribute(doc, "type", "String", "roundrectangle"));
            graphics.appendChild(attribute(doc, "fill", "String", color));
            graphics.appendChild(attribute(doc, "outline", "String", "#223344"));
            node.appendChild(graphics);
            return node;
        }

        List<Element> xmlEdges(Document doc) {
            if (l == null)
                return Collections.emptyList();

            return Arrays.asList(edge(doc, l), edge(doc, r));
        }

        private Element edge(Document doc, GraphNode target) {
            Element edge = section(doc, "edge");
            edge.appendChild(attribute(doc, "source", "int", String.valueOf(id)));
            edge.appendChild(attribute(doc, "target", "int", String.valueOf(target.id)));

            Element graphics = section(doc, "graphics");
            graphics.appendChild(attribute(doc, "fill", "String", "#000000"));
            graphics.appendChild(attribute(doc, "targetArrow", "String", "standard"));
            edge.appendChild(graphics);

            return edge;
        }
    }

    private List<GraphNode> nodes(SplitNode node) {
        GraphNode n = new GraphNode(0, 0, nodeWidth, nodeHeight, formatNode(node));
        if (node.isLeaf()) {
            n.color = "#EEDDAA";
            return Collections.singletonList(n);
        }
        n.color = "#88CCDD";
        List<GraphNode> left = nodes(node.getLeft());
        List<GraphNode> right = nodes(node.getRight());
        n.l = left.get(0);
        n.r = right.get(0);
        left.forEach(gn -> gn.y += 70);
        right.forEach(gn -> gn.y += 70);
        IntSummaryStatistics leftStat = left.stream().collect(Collectors.summarizingInt(gn -> gn.x));
        IntSummaryStatistics rightStat = right.stream().collect(Collectors.summarizingInt(gn -> gn.x));
        int leftMin = leftStat.getMin();
        int leftMax = leftStat.getMax() + n.w;
        int rightMin = rightStat.getMin();
        int rightMax = rightStat.getMax() + n.w;
        int totalWidth = leftMax - leftMin + rightMax - rightMin + 30;
        int shiftLeft = n.x + (n.w - totalWidth) / 2 - leftMin;
        int shiftRight = n.x + (n.w + totalWidth) / 2 - rightMax;
        left.forEach(gn -> gn.x += shiftLeft);
        right.forEach(gn -> gn.x += shiftRight);
        List<GraphNode> result = new ArrayList<>(left.size() + right.size() + 1);
        result.add(n);
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    private static Element section(Document doc, String name) {
        Element s = doc.createElement("section");
        s.setAttribute("name", name);
        return s;
    }

    private static Element attribute(Document doc, String key, String type, String value) {
        Element e = doc.createElement("attribute");
        e.setAttribute("key", key);
        e.setAttribute("type", type);
        e.appendChild(doc.createTextNode(value));
        return e;
    }
    
    public XGMLFormatter nodeWidth(int width) {
        this.nodeWidth = width;
        return this;
    }

    public XGMLFormatter nodeHeight(int height) {
        this.nodeHeight = height;
        return this;
    }
    
    public XGMLFormatter nodeFormat(String nonEmpty, String empty) {
        this.format = nonEmpty;
        this.empty = empty;
        return this;
    }
    
    private String formatNode(SplitNode node) {
        String format = node.isEmpty() ? empty : this.format;
        return format.replace("%f", String.valueOf(node.getFirst()))
            .replace("%l", String.valueOf(node.getLast()))
            .replace("%c", String.valueOf(node.getCount()));
    }
    
    public Document asDocument(SplitTree tree) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        Element root = section(doc, "xgml");
        root.appendChild(attribute(doc, "Creator", "String", "StreamTools"));
        doc.appendChild(root);

        Element graph = section(doc, "graph");
        graph.appendChild(attribute(doc, "hierarchic", "int", "1"));
        graph.appendChild(attribute(doc, "label", "String", ""));
        graph.appendChild(attribute(doc, "directed", "int", "1"));
        root.appendChild(graph);

        List<GraphNode> nodes = nodes(tree.root());
        for (int i = 0; i < nodes.size(); i++)
            nodes.get(i).id = i;

        nodes.forEach(n -> graph.appendChild(n.xmlNode(doc)));

        nodes.forEach(n -> n.xmlEdges(doc).forEach(graph::appendChild));
        return doc;
    }

    public void writeTo(SplitTree tree, OutputStream os) throws ParserConfigurationException, TransformerException {
        DOMSource source = new DOMSource(asDocument(tree));
        StreamResult result = new StreamResult(os);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
    }
}
