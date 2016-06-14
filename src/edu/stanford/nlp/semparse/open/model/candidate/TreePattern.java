package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.*;

import edu.stanford.nlp.semparse.open.model.tree.KNode;

/**
 * A TreePattern is the entire specification for selecting entities from the knowledge tree.
 */
public class TreePattern {

  protected final KNode rootNode;
  protected final List<PathEntry> recordPath;
  protected final List<KNode> recordNodes;
  
  public TreePattern(KNode rootNode, Collection<PathEntry> recordPath, Collection<KNode> recordNodes) {
    this.rootNode = rootNode;
    this.recordPath = new ArrayList<>(recordPath);
    this.recordNodes = new ArrayList<>(recordNodes);
  }

  @Override public String toString() {
    return PathUtils.getXPathString(recordPath);
  }
  
  public KNode getRoot() {
    return rootNode;
  }
  
  public List<PathEntry> getPath() {
    return recordPath;
  }
  
  public List<KNode> getNodes() {
    return recordNodes;
  }
  
}
