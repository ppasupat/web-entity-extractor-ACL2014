package edu.stanford.nlp.semparse.open.core.eval;

import java.util.*;

import edu.stanford.nlp.semparse.open.core.OpenSemanticParser;
import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswer;
import edu.stanford.nlp.semparse.open.dataset.ExpectedAnswerInjectiveMatch;
import edu.stanford.nlp.semparse.open.dataset.IRScore;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntity;
import edu.stanford.nlp.semparse.open.dataset.entity.TargetEntityNearMatch;
import edu.stanford.nlp.semparse.open.model.candidate.Candidate;
import fig.basic.LogInfo;

public abstract class EvaluationCase {
  public final Evaluator evaluator;
  public final Example ex;
  public final CandidateStatistics pred, firstTrue, best;
  
  // IR scores compared to expected entities
  public final IRScore predIRScore, firstTrueIRScore, bestIRScore;
  
  // IR scores compared to best true
  public final IRScore predIRScoreOnBest, firstTrueIRScoreOnBest;
  
  protected EvaluationCase(Evaluator evaluator, Example ex, CandidateStatistics pred,
      CandidateStatistics firstTrue, CandidateStatistics best) {
    this.evaluator = evaluator;
    this.ex = ex;
    this.pred = pred;
    this.firstTrue = firstTrue;
    this.best = best;
    // Compute IR scores
    predIRScore = (pred == null) ? null : ex.expectedAnswer.getIRScore(pred.candidate);
    firstTrueIRScore = (firstTrue == null) ? null : ex.expectedAnswer.getIRScore(firstTrue.candidate);
    bestIRScore = (best == null) ? null : ex.expectedAnswer.getIRScore(best.candidate);
    if (best == null) {
      predIRScoreOnBest = firstTrueIRScoreOnBest = null;
    } else {
      List<TargetEntity> bestTrueEntites = new ArrayList<>();
      for (String entity : best.candidate.predictedEntities) {
        bestTrueEntites.add(new TargetEntityNearMatch(entity));
      }
      ExpectedAnswer bestTrueAnswer = new ExpectedAnswerInjectiveMatch(bestTrueEntites);
      predIRScoreOnBest = (pred == null) ? null : bestTrueAnswer.getIRScore(pred.candidate);
      firstTrueIRScoreOnBest = (firstTrue == null) ? null : bestTrueAnswer.getIRScore(firstTrue.candidate);
    }
  }
  
  /** Log TRUE : the first likely correct candidate */
  public void logTrue() {
    LogInfo.begin_track("TRUE (likely correct candidate):");
    if (firstTrue == null) {
      LogInfo.logs("<%s SUPER FAIL> Correct candidate not found!", evaluator.testSuiteName);
    } else {
      LogInfo.logs("<%s %s> Rank %d [Unique Rank %d]: (Total Feature Score = %s)", evaluator.testSuiteName,
          firstTrue.rank == 1 ? "SUCCESS" : "FAIL", firstTrue.rank,
          firstTrue.uniqueRank, firstTrue.score);
      logCandidate(firstTrue);
    }
    LogInfo.end_track();
  }
  
  /** Log PRED : the top scoring candidate */
  public void logPred() {
    if (pred != null) {
      LogInfo.begin_track("PRED (top scoring candidate):");
      LogInfo.logs("Rank 1 [Unique Rank 1]: (Total Feature Score = %s)", pred.score);
      logCandidate(pred);
      LogInfo.end_track();
    }
  }
  
  private void logCandidate(CandidateStatistics candidateStat) {
    Candidate candidate = candidateStat.candidate;
    LogInfo.logs("%s %s", candidate.pattern, candidate.ex.expectedAnswer.getIRScore(candidate));
    if (OpenSemanticParser.opts.logVerbosity >= 2)
      LogInfo.log(candidate.sampleEntities());
    if (OpenSemanticParser.opts.logVerbosity >= 3)
      evaluator.learner.logFeatureWeights(candidate);
  }
  
  public abstract void logFeatureDiff();
}
