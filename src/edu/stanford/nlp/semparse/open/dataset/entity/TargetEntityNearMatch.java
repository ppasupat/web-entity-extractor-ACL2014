package edu.stanford.nlp.semparse.open.dataset.entity;

import java.util.Collection;

import edu.stanford.nlp.semparse.open.ling.LingUtils;
import edu.stanford.nlp.semparse.open.util.EditDistance;
import fig.basic.Option;

public class TargetEntityNearMatch implements TargetEntity {
  public static class Options {
    @Option public int nearMatchMaxEditDistance = 2;
    @Option(gloss = "level of target entity string normalization "
                  + "(0 = none / 1 = whitespace / 2 = simple / 3 = aggressive)")
    public int targetNormalizeEntities = 2;
  }
  public static Options opts = new Options();
  
  public final String expected, normalizedExpected;
  
  public TargetEntityNearMatch(String expected) {
    this.expected = expected;
    this.normalizedExpected = LingUtils.normalize(expected, opts.targetNormalizeEntities);
  }
  
  @Override public String toString() {
    StringBuilder sb = new StringBuilder(expected);
    if (!expected.equals(normalizedExpected))
      sb.append(" || ").append(normalizedExpected);
    return sb.toString();
  }

  @Override
  public boolean match(String predictedEntity) {
    // Easy cases
    if (expected.equals(predictedEntity))
      return true;
    // Edit distance
    if (EditDistance.withinEditDistance(normalizedExpected, predictedEntity, opts.nearMatchMaxEditDistance))
      return true;
    return false;
  }

  @Override
  public boolean matchAny(Collection<String> predictedEntities) {
    for (String predictedEntity : predictedEntities) {
      if (match(predictedEntity)) return true;
    }
    return false;
  }

}
