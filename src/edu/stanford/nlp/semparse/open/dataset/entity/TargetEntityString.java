package edu.stanford.nlp.semparse.open.dataset.entity;

import java.util.Collection;

public class TargetEntityString implements TargetEntity {
  
  public final String expected;
  
  public TargetEntityString(String expected) {
    this.expected = expected;
  }
  
  @Override
  public String toString() {
    return expected;
  }

  @Override
  public boolean match(String predicted) {
    return expected.equals(predicted);
  }

  @Override
  public boolean matchAny(Collection<String> predictedEntities) {
    return predictedEntities.contains(expected);
  }
  
}
