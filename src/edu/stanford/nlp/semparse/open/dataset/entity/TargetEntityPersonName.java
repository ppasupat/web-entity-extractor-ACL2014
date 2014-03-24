package edu.stanford.nlp.semparse.open.dataset.entity;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;

public class TargetEntityPersonName implements TargetEntity {
  
  public final String first;
  public final String mid;
  public final String last;
  final List<String> patterns = Lists.newArrayList();
  
  public TargetEntityPersonName(String first, String last) {
    this.first = first;
    this.mid = null;
    this.last = last;
    generatePatterns();
  }
  
  public TargetEntityPersonName(String first, String mid, String last) {
    this.first = first;
    if (mid.length() == 2 && mid.charAt(1) == '.')
      this.mid = mid.substring(0, 1);
    else
      this.mid = mid;
    this.last = last;
    generatePatterns();
  }
  
  private void generatePatterns() {
    patterns.add(first + " " + last);
    patterns.add(last + ", " + first);
    patterns.add(first.charAt(0) + ". " + last);
    patterns.add(last + ", " + first.charAt(0) + ".");
    if (mid != null) {
      if (mid.length() > 1) {
        patterns.add(first + " " + mid + " " + last);
        patterns.add(last + ", " + first + " " + mid);
      }
      patterns.add(first + " " + mid.charAt(0) + ". " + last);
      patterns.add(last + ", " + first + " " + mid.charAt(0) + ".");
    }
  }
  
  @Override
  public String toString() {
    if (mid != null)
      return first + " " + mid + " " + last;
    return first + " " + last;
  }

  @Override
  public boolean match(String predictedEntity) {
    for (String pattern : patterns)
      if (pattern.equals(predictedEntity)) return true;
    return false;
  }

  @Override
  public boolean matchAny(Collection<String> predictedEntities) {
    for (String pattern : patterns)
      if (predictedEntities.contains(pattern)) return true;
    return false;
  }

}
