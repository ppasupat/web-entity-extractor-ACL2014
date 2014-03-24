package edu.stanford.nlp.semparse.open.model.candidate;

public class PathEntryWithRange extends PathEntry {
  
  public final int indexStart, indexEnd;
  
  public PathEntryWithRange(String tag, int indexStart, int indexEnd) {
    super(tag);
    this.indexStart = indexStart;
    this.indexEnd = indexEnd;
  }
  
  public boolean isIndexed() {
    return true;
  }
  
  @Override public String toString() {
    StringBuilder sb = new StringBuilder().append(this.tag).append("[");
    if (indexStart != 0) sb.append(indexStart);
    sb.append(":");
    if (indexEnd != 0) sb.append("-").append(indexEnd);
    sb.append("]");
    return sb.toString();
  }

  @Override public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (obj == null || obj.getClass() != this.getClass())
      return false;
    PathEntryWithRange that = (PathEntryWithRange) obj;
    return this.tag.equals(that.tag) && this.indexStart == that.indexStart && this.indexEnd == that.indexEnd;
  }

  @Override public int hashCode() {
    return tag.hashCode() + indexStart << 8 + indexEnd;
  }

}
