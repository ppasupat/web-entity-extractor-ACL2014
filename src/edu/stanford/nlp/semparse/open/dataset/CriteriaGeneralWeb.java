package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import com.google.common.collect.Lists;

import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityNearMatch;
import edu.stanford.nlp.semparse.open.dataset.library.JSONDataset.JSONDatasetDatum;

/**
 * Must match first, second, and last entities
 */
public class CriteriaGeneralWeb implements Criteria {
  public final JSONDatasetDatum datum;
  public final TargetEntity first, second, last;
  
  public CriteriaGeneralWeb(JSONDatasetDatum datum) {
    this.datum = datum;
    this.first = new TargetEntityNearMatch(datum.criteria.first);
    this.second = new TargetEntityNearMatch(datum.criteria.second);
    this.last = new TargetEntityNearMatch(datum.criteria.last);
  }

  @Override
  public List<TargetEntity> getTargetEntities() {
    return Lists.newArrayList(first, second, last);
  }

  @Override
  public int countMatchedCriteria(List<String> predictedEntities) {
    int n = predictedEntities.size(), answer = 0;
    String predictedFirst = n > 0 ? predictedEntities.get(0) : "",
          predictedSecond = n > 1 ? predictedEntities.get(1) : "",
            predictedLast = n > 0 ? predictedEntities.get(n - 1) : "";
    if (first.match(predictedFirst)) answer++;
    if (second.match(predictedSecond)) answer++;
    if (last.match(predictedLast)) answer++;
    return answer;
  }

  @Override
  public int numCriteria() {
    return 3;
  }

  @Override
  public IRScore getIRScore(List<String> predictedEntities) {
    return new IRScore(countMatchedCriteria(predictedEntities), numCriteria(), numCriteria());
  }

  @Override
  public double getCorrectnessScore(List<String> predictedEntities) {
    // TODO Make this better
    if (countMatchedCriteria(predictedEntities) != numCriteria()) return 0;
    return predictedEntities.size();    // Prefer larger set of entities
  }
}