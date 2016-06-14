package edu.stanford.nlp.semparse.open.dataset;

import java.util.*;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.util.BipartiteMatcher;

/**
 * Only 1 criteria: whether the lists are exactly the same.
 */
public class CriteriaExactMatch implements Criteria {
  public final List<TargetEntity> targetEntities;
  
  public CriteriaExactMatch(TargetEntity... targetEntities) {
    this.targetEntities = Arrays.asList(targetEntities);
  }
  
  public CriteriaExactMatch(List<TargetEntity> targetEntities) {
    this.targetEntities = targetEntities;
  }

  @Override
  public List<TargetEntity> getTargetEntities() {
    return targetEntities;
  }

  @Override
  public int numCriteria() {
    return 1;
  }

  @Override
  public int countMatchedCriteria(List<String> predictedEntities) {
    if (predictedEntities.size() == targetEntities.size())
      if (new BipartiteMatcher(targetEntities, predictedEntities).findMaximumMatch() == targetEntities.size())
        return 1;
    return 0;
  }
  
  Map<List<String>, IRScore> irScoreCache = new HashMap<>();

  @Override
  public IRScore getIRScore(List<String> predictedEntities) {
    IRScore answer = irScoreCache.get(predictedEntities);
    if (answer == null) {
      answer = new IRScore(targetEntities, predictedEntities);
      irScoreCache.put(predictedEntities, answer);
    }
    return answer;
  }
  
  /**
   * More F1 = better candidate.
   */
  @Override
  public double getCorrectnessScore(List<String> predictedEntities) {
    return getIRScore(predictedEntities).f1;
  }

}
