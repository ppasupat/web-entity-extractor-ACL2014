package edu.stanford.nlp.semparse.open.dataset.entity;

import java.util.Collection;

/**
 * A TargetEntity represents an answer key -- an entity that appears in the target web page
 * (according to a Turker).
 * 
 * A TargetEntity provides matching methods, which may implement fancy matching schemes
 * such as partial matching or person name matching.
 */
public interface TargetEntity {
  public boolean match(String predictedEntity);
  public boolean matchAny(Collection<String> predictedEntities);
}
