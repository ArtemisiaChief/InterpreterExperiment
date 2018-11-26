package mini.entity;

import java.util.ArrayList;

/**
 * 树节点类
 * 用于构造语法树
 * 以便进行语义分析
 */

public class Node {
    private ArrayList<Node> childNodes;
    private String content;
    private String type;
    private int count;

    public Node(String type) {
        childNodes = new ArrayList<>();
        this.type = type;
        content = null;
    }

    public Node(String type, String content) {
        childNodes = new ArrayList<>();
        this.type = type;
        this.content = content;
    }

    public Node(String type, String content, int count) {
        childNodes = new ArrayList<>();
        this.type = type;
        this.content = content;
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getCount() {
        return count;
    }

    public void addChild(Node node) {
        childNodes.add(node);
    }

    public ArrayList<Node> getChildren() {
        return childNodes;
    }

    public Node getChild(int index) {
        if (index < childNodes.size())
            return childNodes.get(index);
        return null;
    }

    public String toString() {
        return String.format("%s\n", type);
    }

    public String toString(boolean isTerminal) {
        if (isTerminal)
            return String.format("%s\n", content);
        return String.format("%s\n", type);
    }

    public String print(int height) {
        String str = "";
        for (int i = 0; i < height; i++) {
            str += "           ";
        }
        str += this.toString(content != null) + "\n";
        for (Node child : childNodes) {
            str += child.print(height + 1);
        }
        return str;
    }

}