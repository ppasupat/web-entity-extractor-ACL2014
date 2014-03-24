package edu.stanford.nlp.semparse.open.core.eval;

import edu.stanford.nlp.semparse.open.dataset.Example;

public class EvaluationSuccess extends EvaluationCase {

  public EvaluationSuccess(Evaluator evaluator, Example ex, CandidateStatistics pred,
      CandidateStatistics firstTrue, CandidateStatistics best) {
    super(evaluator, ex, pred, firstTrue, best);
  }

  @Override
  public void logFeatureDiff() {
    // Do nothing
  }
}
