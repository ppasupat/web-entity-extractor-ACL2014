package edu.stanford.nlp.semparse.open.model.tree;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fig.basic.LogInfo;
import fig.basic.StrUtils;

/**
 * A node in the knowledge tree.
 * 
 * Nodes can be one of the following:
 * <ul>
 *   <li>type = QUERY, value = search query (e.g., "us cities")</li>
 *   <li>type = URL, value = url (e.g., http://en.wikipedia.org/wiki/List_of_United_States_cities_by_population)</li>
 *   <li>type = TAG, value = tag of the DOM node (e.g., "h1")</li>
 *   <li>type = ATTR, value = attribute (e.g., "class")</li>
 *   <li>type = TEXT, value = a string (e.g., "San Francisco")</li>
 * </ul>
 */
public class KNode {
  public enum Type { QUERY, URL, TAG, ATTR, TEXT };
  
  public final Type type;
  public final String value;
  private final List<KNode> children;
  private final List<KNode> attributes;
  
  // fullText == '' if the node is empty
  // fullText == null if the full text is longer than the specified length.
  public final String fullText;
  
  // parent of root node is null
  public final KNode parent;
  
  // depth of root node is 0
  public final int depth;
  
  // timestamps of depth first search (used for firing range features) 
  public int timestampIn, timestampOut, timestampInCollapsed;

  public KNode(KNode parent, Type type, String value) {
    this(parent, type, value, "");
  }
  
  public KNode(KNode parent, Type type, String value, String fullText) {
    this.type = type;
    this.value = value;
    this.children = Lists.newArrayList();
    this.attributes = Lists.newArrayList();
    this.fullText = fullText;
    
    this.parent = parent;
    if (this.parent == null) {
      this.depth = 0;
    } else {
      this.depth = this.parent.depth + 1;
      if (type == Type.ATTR) {
        this.parent.attributes.add(this);
      } else {
        this.parent.children.add(this);
      }
    }
  }
  
  /**
   * Create a child and return the child.
   */
  public KNode createChild(Type type, String value) {
    return new KNode(this, type, value);
  }
  
  /**
   * Create a child and return the child.
   */
  public KNode createChild(Type type, String value, String fullText) {
    return new KNode(this, type, value, fullText);
  }
  
  public KNode createAttribute(String attributeName, String attributeValue) {
    KNode attributeNode = createChild(Type.ATTR, attributeName, attributeValue);
    attributeNode.createChild(Type.TEXT, attributeValue, attributeValue);
    return attributeNode;
  }
  
  // Getters
  
  public List<KNode> getChildren() {
    return Collections.unmodifiableList(children);
  }
  
  public List<KNode> getChildrenOfTag(String tag) {
    List<KNode> answer = Lists.newArrayList();
    for (KNode child : children) {
      if (child.type == Type.TAG && (tag.equals(child.value) || tag.equals("*"))) {
        answer.add(child);
      }
    }
    return answer;
  }
  
  // The index is 0-based
  public KNode getChildrenOfTag(String tag, int index) {
    int count = 0;
    for (KNode child : children) {
      if (child.type == Type.TAG && (tag.equals(child.value) || tag.equals("*"))) {
        if (count == index) return child;
        count++;
      }
    }
    return null;
  }
  
  public String getAttribute(String attributeName) {
    for (KNode attributeNode : attributes) {
      if (attributeNode.value.equals(attributeName))
        return attributeNode.fullText;
    }
    return "";
  }
  
  public String[] getAttributeList(String attributeName) {
    for (KNode attributeNode : attributes) {
      if (attributeNode.value.equals(attributeName) && !attributeNode.fullText.isEmpty())
        return attributeNode.fullText.split(" ");
    }
    return new String[0];
  }
  
  // The index is 0-based
  public int getChildIndex() {
    return this.parent.children.indexOf(this);
  }
  
  // The index is 0-based
  public int getChildIndexOfSameTag() {
    int count = 0;
    for (KNode child : this.parent.children) {
      if (child.type == Type.TAG && child.value.equals(this.value)){
        if (child == this) return count;
        count++;
      }
    }
    return -1;
  }
  
  public int countChildren() {
    return this.children.size();
  }
  
  public int countChildren(String tag) {
    int count = 0;
    for (KNode child : children) {
      if (child.type == Type.TAG && child.value.equals(tag)) count++;
    }
    return count;
  }
  
  // Debug Print
  
  public void debugPrint(int indent) {
    LogInfo.logs(StrUtils.repeat(" ", indent) + "%s '%s'", type, value);
    for (KNode child : children) {
      child.debugPrint(indent + 2);
    }
  }
  
  // Timestamp
  
  public void generateTimestamp() {
    generateTimestamp(1);
    generateTimestampInCollapsed(1);
  }
  
  protected int generateTimestamp(int currentTimestamp) {
    timestampIn = currentTimestamp++;
    for (KNode node : children) {
      currentTimestamp = node.generateTimestamp(currentTimestamp);
    }
    timestampOut = currentTimestamp++;
    return currentTimestamp;
  }
  
  protected int generateTimestampInCollapsed(int currentTimestampInCollapsed) {
    timestampInCollapsed = currentTimestampInCollapsed++;
    for (KNode node : children) {
      currentTimestampInCollapsed = node.generateTimestampInCollapsed(currentTimestampInCollapsed);
    }
    return currentTimestampInCollapsed;
  }
  
}
