package edu.stanford.nlp.semparse.open.model.candidate;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;

import edu.stanford.nlp.semparse.open.model.tree.KNode;

/**
 * A TreePattern is the entire specification for selecting entities from the knowledge tree.
 */
public class TreePattern {

  protected final KNode rootNode;
  protected final ImmutableList<PathEntry> recordPath;
  protected final ImmutableList<KNode> recordNodes;
  
  public TreePattern(KNode rootNode, Collection<PathEntry> recordPath, Collection<KNode> recordNodes) {
    this.rootNode = rootNode;
    this.recordPath = ImmutableList.copyOf(recordPath);
    this.recordNodes = ImmutableList.copyOf(recordNodes);
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
