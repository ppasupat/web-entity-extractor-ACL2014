package edu.stanford.nlp.semparse.open.dataset.entity;

import java.util.Collection;

public class TargetEntitySubstring implements TargetEntity {
  
  public final String expected;
  
  public TargetEntitySubstring(String expected) {
    this.expected = expected;
  }
  
  @Override public String toString() {
    return expected;
  }

  @Override
  public boolean match(String predictedEntity) {
    return predictedEntity.contains(expected);
  }

  @Override
  public boolean matchAny(Collection<String> predictedEntities) {
    for (String predictedEntity : predictedEntities) {
      if (match(predictedEntity)) return true;
    }
    return false;
  }

}
