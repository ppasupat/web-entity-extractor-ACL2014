package edu.stanford.nlp.semparse.open.model.tree;

import java.util.*;

public class KNodeUtils {

  /**
   * @return The lowest common ancestor of all specified nodes.
   */
  public static KNode lowestCommonAncestor(Collection<KNode> nodes) {
    int minDepth = Integer.MAX_VALUE;
    for (KNode node : nodes)
      minDepth = Math.min(minDepth, node.depth);
    Set<KNode> sameDepthAncestors = new HashSet<>();
    for (KNode node : nodes) {
      while (node.depth != minDepth) node = node.parent;
      sameDepthAncestors.add(node);
    }
    while (sameDepthAncestors.size() != 1) {
      if (minDepth == 0) return null;
      Set<KNode> newSameDepthAncestors = new HashSet<>();
      for (KNode node : sameDepthAncestors) newSameDepthAncestors.add(node.parent);
      sameDepthAncestors = newSameDepthAncestors;
    }
    return sameDepthAncestors.iterator().next();
  }
  
  public static boolean isDescendantOf(KNode allegedDescendant, KNode node) {
    KNode currentNode = allegedDescendant;
    while (currentNode != null) {
      if (currentNode == node) return true;
      currentNode = currentNode.parent;
    }
    return false;
  }
  
  /**
   * Print the tree to standard error. Useful for debugging.
   */
  public static void printTree(KNode node) { printTree(node, 0); }
  
  public static void printTree(KNode node, int indent) {
    if ("text".equals(node.value)) {
      String text = node.fullText;
      if (text == null) text = "...";
      System.err.printf("%s%s\n", new String(new char[indent]).replace('\0', ' '), text);
      return;
    }
    System.err.printf("%s<%s>\n", new String(new char[indent]).replace('\0', ' '), node.value);
    for (KNode child : node.getChildren()) {
      printTree(child, indent + 2);
    }
    System.err.printf("%s</%s>\n", new String(new char[indent]).replace('\0', ' '), node.value);
  }

  /**
   * Copy a KNode and its subtree to a new parent.
   */
  public static KNode copyTree(KNode node, KNode newParent) {
    KNode newNode = newParent.createChild(node);
    for (KNode x : node.getChildren()) copyTree(x, newNode);
    for (KNode x : node.getAttributes()) copyTree(x, newNode);
    return newNode;
  }
  
}
