package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.*;

import edu.stanford.nlp.semparse.open.model.tree.KNode;
import edu.stanford.nlp.semparse.open.model.tree.KNodeUtils;
import fig.basic.LogInfo;

/**
 * Utilities that deal with XPath (list of PathEntry)
 */
public class PathUtils {
  
  public static String getXPathString(List<PathEntry> path) {
    StringBuilder sb = new StringBuilder();
    for (PathEntry entry : path) {
      sb.append("/").append(entry);
    }
    return sb.toString();
  }
  
  public static String getXPathSuffixString(List<PathEntry> path, int amount) {
    StringBuilder sb = new StringBuilder();
    int startIndex = Math.max(0, path.size() - amount);
    for (PathEntry entry : path.subList(startIndex, path.size())) {
      sb.append("/").append(entry.toString());
    }
    return sb.toString();
  }
  
  public static String getXPathSuffixStringNoIndex(List<PathEntry> path, int amount) {
    StringBuilder sb = new StringBuilder();
    int startIndex = Math.max(0, path.size() - amount);
    for (PathEntry entry : path.subList(startIndex, path.size())) {
      sb.append("/").append(entry.tag);
    }
    return sb.toString();
  }
  
  public static List<PathEntry> getXPathSuffix(List<PathEntry> path, int amount) {
    List<PathEntry> suffix = new ArrayList<>();
    int startIndex = Math.max(0, path.size() - amount);
    for (PathEntry entry : path.subList(startIndex, path.size())) {
      suffix.add(entry);
    }
    return suffix;
  }
  
  public static List<PathEntry> getXPathSuffixNoIndex(List<PathEntry> path, int amount) {
    List<PathEntry> suffix = new ArrayList<>();
    int startIndex = Math.max(0, path.size() - amount);
    for (PathEntry entry : path.subList(startIndex, path.size())) {
      suffix.add(entry.getNoIndexVersion());
    }
    return suffix;
  }
  
  /**
   * Execute XPath on the currentNode and add the matched nodes to the answer collection.
   * Only add nodes with short text (i.e., fullText != null and fullText != "")
   */
  public static void executePath(List<PathEntry> path, KNode currentNode, Collection<KNode> answer) {
    if (!path.get(0).matchTag(currentNode))
      LogInfo.fails("XPath mismatch (node %s != xpath %s)", currentNode.value, path.get(0).tag);
    if (path.size() == 1) {
      if (currentNode.fullText != null && !currentNode.fullText.isEmpty())
        answer.add(currentNode);
      return;
    }
    // Go to the next element of the path
    List<PathEntry> descendants = path.subList(1, path.size());
    PathEntry nextPathEntry = descendants.get(0);
    if (nextPathEntry instanceof PathEntryWithRange) {
      PathEntryWithRange nextPathEntryWithRange = (PathEntryWithRange) nextPathEntry;
      List<KNode> children = currentNode.getChildrenOfTag(nextPathEntry.tag);
      int start = nextPathEntryWithRange.indexStart;
      int end = children.size() - nextPathEntryWithRange.indexEnd;
      if (start >= end) return;
      for (KNode child : children.subList(start, end)) {
        executePath(descendants, child, answer);
      }
    } else if (nextPathEntry.isIndexed()) {
      KNode child = currentNode.getChildrenOfTag(nextPathEntry.tag, nextPathEntry.index);
      if (child != null)
        executePath(descendants, child, answer);
    } else {
      for (KNode child : currentNode.getChildrenOfTag(nextPathEntry.tag))
        executePath(descendants, child, answer);
    }
  }
  
  /**
   * Return the prefix of the given XPath where the last entry of the prefix refers to the given node.
   */
  public static List<PathEntry> pathPrefixAtNode(List<PathEntry> path, KNode root, KNode node) {
    if (!KNodeUtils.isDescendantOf(node, root)) {
      LogInfo.fails("%s not a descendant of %s", node, root);
    }
    return path.subList(0, node.depth - root.depth);
  }
  
  /**
   * Return the suffix of the given XPath where the first entry of the suffix refers to the given node.
   */
  public static List<PathEntry> pathSuffixAtNode(List<PathEntry> path, KNode root, KNode node) {
    if (!KNodeUtils.isDescendantOf(node, root)) {
      LogInfo.fails("%s not a descendant of %s", node, root);
    }
    return path.subList(node.depth - root.depth, path.size());
  }

}
