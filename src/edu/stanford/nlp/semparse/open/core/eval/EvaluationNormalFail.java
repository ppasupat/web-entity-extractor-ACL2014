package edu.stanford.nlp.semparse.open.core.eval;

import edu.stanford.nlp.semparse.open.dataset.Example;
import fig.basic.LogInfo;

/**
 * When the correct candidate is found but not in the 1st rank
 */
public class EvaluationNormalFail extends EvaluationCase {

  public EvaluationNormalFail(Evaluator evaluator, Example ex, CandidateStatistics pred,
      CandidateStatistics firstTrue, CandidateStatistics best) {
    super(evaluator, ex, pred, firstTrue, best);
  }

  @Override
  public void logFeatureDiff() {
    LogInfo.begin_track("### %s ###", ex);
    LogInfo.logs("GOLD: %s", ex.expectedAnswer.sampleEntities());
    LogInfo.logs("PRED: %s", pred.candidate.pattern);
    LogInfo.logs("      (vs target entities) %s", predIRScore);
    LogInfo.logs("      (vs best candidate)  %s", predIRScoreOnBest);
    LogInfo.logs("      %s", pred.candidate.sampleEntities());
    LogInfo.logs("TRUE: %s", firstTrue.candidate.pattern);
    LogInfo.logs("      (vs target entities) %s", firstTrueIRScore);
    LogInfo.logs("      (vs best candidate)  %s", firstTrueIRScoreOnBest);
    LogInfo.logs("      (Rank %d [Unique Rank %d])", firstTrue.rank, firstTrue.uniqueRank);
    LogInfo.logs("      %s", firstTrue.candidate.sampleEntities());
    LogInfo.logs("BEST: %s", best.candidate.pattern);
    LogInfo.logs("      (vs target entities) %s", bestIRScore);
    LogInfo.logs("      %s", best.candidate.sampleEntities());
    evaluator.learner.logFeatureDiff(firstTrue.candidate, pred.candidate);
    LogInfo.end_track();
  }
  
}
