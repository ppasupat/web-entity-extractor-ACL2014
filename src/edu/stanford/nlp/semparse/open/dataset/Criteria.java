package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;

public interface Criteria {
  public List<TargetEntity> getTargetEntities();
  
  /** Number of criteria **/
  public int numCriteria();
  
  /** Number of matched criteria **/
  public int countMatchedCriteria(List<String> predictedEntities);
  
  /** Return a custom IR score */
  public IRScore getIRScore(List<String> predictedEntities);
  
  /**
   * Return the correctness score. Between correct candidates, the one with
   * higher correctness score is more correct.
   * 
   * Normally, this is just getIRScore().f1 
   */
  public double getCorrectnessScore(List<String> predictedEntities);
}