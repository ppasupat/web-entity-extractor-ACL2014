package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import edu.stanford.nlp.semparse.open.core.eval.CandidateStatistics;
import fig.basic.Option;

/**
 * Gives reward = 1 if the predicted entities match all criteria, and reward = 0 otherwise.
 */
public class ExpectedAnswerCriteriaMatch extends ExpectedAnswer {
  public static class Options {
    @Option(gloss = "Give partial reward for lists that don't exactly match the criteria")
    public boolean generous = false;
  }
  public static Options opts = new Options();
  
  public final Criteria criteria; 
  
  public ExpectedAnswerCriteriaMatch(Criteria criteria) {
    super(criteria.getTargetEntities());
    this.criteria = criteria;
  }
  
  @Override
  public IRScore getIRScore(List<String> predictedEntities) {
    return criteria.getIRScore(predictedEntities);
  }
  
  @Override
  public double reward(List<String> predictedEntities) {
    if (!opts.generous) {
      return countCorrectEntities(predictedEntities) == criteria.numCriteria() ? 1 : 0;
    } else {
      // Generous reward
      double f1 = criteria.getIRScore(predictedEntities).f1;
      return f1 > ExpectedAnswerInjectiveMatch.opts.irThreshold ? f1 : 0;
    }
  }
  
  @Override
  public int computeCountCorrectEntities(List<String> predictedEntities) {
    return criteria.countMatchedCriteria(predictedEntities);
  }
  
  @Override
  public boolean isLikelyCorrect(List<String> predictedEntities) {
    return countCorrectEntities(predictedEntities) == criteria.numCriteria();
  }
  
  @Override
  public CandidateStatistics findBestCandidate(List<CandidateStatistics> rankedCandidateStats) {
    double bestCorrectnessScore = 0;
    CandidateStatistics best = null;
    for (CandidateStatistics candidateStat : rankedCandidateStats) {
      double correctnessScore = criteria.getCorrectnessScore(candidateStat.candidate.predictedEntities);
      if (correctnessScore > bestCorrectnessScore) {
        best = candidateStat;
        bestCorrectnessScore = correctnessScore;
      }
    }
    return best;
  }
}