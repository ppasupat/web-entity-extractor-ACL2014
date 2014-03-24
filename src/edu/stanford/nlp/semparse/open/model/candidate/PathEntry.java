package edu.stanford.nlp.semparse.open.model.candidate;

import edu.stanford.nlp.semparse.open.model.tree.KNode;

/**
 * A PathEntry represents an entry in an XPath.
 * It is immutable.
 * 
 * An XPath is just a list of PathEntry.
 * Utilities involving XPath are in PathUtils class. 
 * 
 * Use an ImmutableList for a fixed XPath (e.g. in TreePattern).
 * The main benefit is that the paths can be hashed and compared consistently.
 * 
 * Use a normal List for an editable path.
 */
public class PathEntry {
  final public String tag;
  final public int index;   // 0-based; -1 = no index

  public PathEntry(String tag, int index) {
    this.tag = tag;
    this.index = index;
  }

  public PathEntry(String tag) {
    this.tag = tag;
    this.index = -1;
  }
  
  public boolean isIndexed() {
    return this.index != -1;
  }
  
  public PathEntry getIndexedVersion(int newIndex) {
    return new PathEntry(tag, newIndex);
  }
  
  public PathEntry getNoIndexVersion() {
    return new PathEntry(tag);
  }
  
  /** Check if the PathEntry's tag matches the node's tag. */
  public boolean matchTag(KNode node) {
    return tag.equals("*") || tag.equals(node.value);
  }

  @Override public String toString() {
    if (this.index == -1)
      return this.tag;
    return this.tag + "[" + (this.index + 1) + "]";
  }

  @Override public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    PathEntry that = (PathEntry) obj;
    return this.tag.equals(that.tag) && this.index == that.index;
  }

  @Override public int hashCode() {
    return tag.hashCode() + index;
  }
}