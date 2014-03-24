package edu.stanford.nlp.semparse.open.dataset;

import java.util.List;

import edu.stanford.nlp.semparse.open.core.eval.CandidateStatistics;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.util.BipartiteMatcher;
import fig.basic.LogInfo;
import fig.basic.Option;

public class ExpectedAnswerInjectiveMatch extends ExpectedAnswer {
  public static class Options {
    @Option public double irThreshold = 0.8;
    @Option public String irCriterion = "recall";
  }
  public static Options opts = new Options();

  public ExpectedAnswerInjectiveMatch(TargetEntity... targetEntities) {super(targetEntities);}
  public ExpectedAnswerInjectiveMatch(List<TargetEntity> targetEntities) {super(targetEntities);}
  public ExpectedAnswerInjectiveMatch(String... targetStrings) {super(targetStrings);}
  
  @Override
  public IRScore getIRScore(List<String> predictedEntities) {
    return new IRScore(countCorrectEntities(predictedEntities), predictedEntities.size(), targetEntities.size());
  }
  
  @Override
  public double reward(List<String> predictedEntities) {
    IRScore score = getIRScore(predictedEntities);
    double criterionScore = 0;
    switch (opts.irCriterion) {
      case "precision": case "p":
        criterionScore = score.precision; break;
      case "recall": case "r":
        criterionScore = score.recall; break;
      case "f1":
        criterionScore = score.f1; break;
      case "raw":
        return (score.numCorrect >= score.numGold - opts.irThreshold) ? 1 : 0;
      default:
        LogInfo.fails("IR Criterion %s not recognized", opts.irCriterion);
    }
    return (criterionScore < opts.irThreshold) ? 0 : criterionScore;
  }

  @Override
  public int computeCountCorrectEntities(List<String> predictedEntities) {
    return new BipartiteMatcher(targetEntities, predictedEntities).findMaximumMatch();
  }

  @Override
  public boolean isLikelyCorrect(List<String> predictedEntities) {
    return reward(predictedEntities) > 0;
  }
  
  @Override
  public CandidateStatistics findBestCandidate(List<CandidateStatistics> rankedCandidateStats) {
    double bestReward = 0;
    CandidateStatistics best = null;
    for (CandidateStatistics candidateStat : rankedCandidateStats) {
      double reward = reward(candidateStat.candidate);
      if (reward > bestReward) {
        best = candidateStat;
        bestReward = reward;
      }
    }
    return best;
  }
}
