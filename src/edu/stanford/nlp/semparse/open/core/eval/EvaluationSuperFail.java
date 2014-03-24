package edu.stanford.nlp.semparse.open.core.eval;

import edu.stanford.nlp.semparse.open.dataset.Example;
import fig.basic.LogInfo;

/**
 * When the correct candidate is not found
 */
public class EvaluationSuperFail extends EvaluationCase {

  public EvaluationSuperFail(Evaluator evaluator, Example ex, CandidateStatistics pred,
      CandidateStatistics firstTrue, CandidateStatistics best) {
    super(evaluator, ex, pred, firstTrue, best);
  }

  @Override
  public void logFeatureDiff() {
    LogInfo.begin_track("### %s ###", ex);
    LogInfo.logs("GOLD: %s", ex.expectedAnswer.sampleEntities());
    if (pred != null) {
      LogInfo.logs("PRED: %s", pred.candidate.pattern);
      LogInfo.logs("      (vs target entities) %s", predIRScore);
      LogInfo.logs("      (vs best candidate)  %s", predIRScoreOnBest);
      LogInfo.logs("      %s", pred.candidate.sampleEntities());
    } else {
      LogInfo.log("PRED: NOT FOUND!");
    }
    LogInfo.log("TRUE: NOT FOUND!");
    if (best != null) {
      LogInfo.logs("BEST: %s", best.candidate.pattern);
      LogInfo.logs("      (vs target entities) %s", bestIRScore);
      LogInfo.logs("      %s", best.candidate.sampleEntities());
    } else {
      LogInfo.log("BEST: NOT FOUND!");
    }
    LogInfo.end_track();
  }
  
}
