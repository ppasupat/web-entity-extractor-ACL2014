package edu.stanford.nlp.semparse.open.core.eval;

import java.util.List;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;

import edu.stanford.nlp.semparse.open.dataset.Example;
import edu.stanford.nlp.semparse.open.model.Learner;
import fig.basic.Fmt;
import fig.basic.ListUtils;
import fig.basic.LogInfo;
import fig.exec.Execution;

/**
 * Deal with evaluation.
 * With zero-one loss, "correct" means matching all criteria
 * - Wiki : match all target entities
 * - Web : match first, second, and last
 * 
 * pred = predicted candidate (first candidate in rankedCandidates)
 * true = first correct candidate in rankedCandidates
 * best = the best thing we can ever select from the web page (may not match all target entities)
 * 
 * SUPER FAIL = (firstTrue == null)
 * NORMAL FAIL = (firstTrue.rank != 1)
 * SUCCESS = (firstTrue.rank == 1)
 */
public class Evaluator {
  String testSuiteName;
  Learner learner;
  int numExamples = 0, numSuccess = 0, numNormalFail = 0, numSuperFail = 0, numFound = 0;
  List<EvaluationCase> successes, normalFails, superFails;
  double sumF1onExpectedEntities = 0, sumF1onBest = 0;
  Multiset<Integer> firstTrueUniqueRanks;
  
  public Evaluator(String testSuiteName, Learner learner) {
    this.testSuiteName = testSuiteName;
    this.learner = learner;
    successes = Lists.newArrayList();
    normalFails = Lists.newArrayList();
    superFails = Lists.newArrayList();
    firstTrueUniqueRanks = HashMultiset.create();
  }
  
  public EvaluationCase add(Example ex, CandidateStatistics pred, CandidateStatistics firstTrue, CandidateStatistics best) {
    numExamples++;
    EvaluationCase evaluationCase;
    if (firstTrue == null) {
      evaluationCase = new EvaluationSuperFail(this, ex, pred, firstTrue, best);
      superFails.add(evaluationCase);
      numSuperFail++;
      firstTrueUniqueRanks.add(Integer.MAX_VALUE);
    } else if (firstTrue.rank != 1) {
      evaluationCase = new EvaluationNormalFail(this, ex, pred, firstTrue, best);
      normalFails.add(evaluationCase);
      numNormalFail++;
      numFound++;
      firstTrueUniqueRanks.add(firstTrue.uniqueRank);
    } else {
      evaluationCase = new EvaluationSuccess(this, ex, pred, firstTrue, best);
      successes.add(evaluationCase);
      numSuccess++;
      numFound++;
      firstTrueUniqueRanks.add(firstTrue.uniqueRank);
    }
    if (evaluationCase.predIRScore != null)
      sumF1onExpectedEntities += evaluationCase.predIRScore.f1;
    if (evaluationCase.predIRScoreOnBest != null)
      sumF1onBest += evaluationCase.predIRScoreOnBest.f1;
    return evaluationCase;
  }
  
  public double[] getAccuracyAtK(int maxK) {
    double[] accuracyAtK = new double[maxK + 1];
    accuracyAtK[0] = 0.0;     // Accuracy at 0 is always 0 (used for padding)
    int sumCorrect = 0;
    for (int k = 1; k <= maxK; k++) {
      sumCorrect += firstTrueUniqueRanks.count(k);
      accuracyAtK[k] = sumCorrect * 1.0 / numExamples;
    }
    return accuracyAtK;
  }
  
  public void printDetails() {
    LogInfo.begin_track("### %s: %d Normal FAILS (correct candidate in other rank) ###", testSuiteName, numNormalFail);
    for (EvaluationCase fail : normalFails) fail.logFeatureDiff();
    LogInfo.end_track();
    LogInfo.begin_track("### %s: %d Super FAILS (correct candidate not found) ###", testSuiteName, numSuperFail);
    for (EvaluationCase fail : superFails) fail.logFeatureDiff();
    LogInfo.end_track();
  }

  public Evaluator putOutput(String prefix) {
    Execution.putOutput(prefix + ".numExamples", numExamples);
    Execution.putOutput(prefix + ".accuracy", 1.0 * numSuccess / numExamples);
    Execution.putOutput(prefix + ".accuracyFound", 1.0 * numSuccess / numFound);
    Execution.putOutput(prefix + ".oracle", 1.0 * numFound / numExamples);
    Execution.putOutput(prefix + ".averageF1onTargetEntities", sumF1onExpectedEntities * 1.0 / numExamples);
    Execution.putOutput(prefix + ".averageF1onBestCandidate", sumF1onBest * 1.0 / numExamples);
    Execution.putOutput(prefix + ".accuracyAtK", Fmt.D(ListUtils.subArray(getAccuracyAtK(10), 1)));
    return this;
  }
  
  public Evaluator printScores() {
    LogInfo.begin_track("%s Evaluation", testSuiteName);
    LogInfo.logs("Number of examples: %d", numExamples);
    LogInfo.logs("Correct candidate in rank #1: %d", numSuccess);
    LogInfo.logs("Correct candidate in other rank: %d", numNormalFail);
    LogInfo.logs("Correct candidate not found: %d", numSuperFail);
    LogInfo.logs("Oracle: %.3f%% ( %d / %d )", numFound * 100.0 / numExamples, numFound, numExamples);
    LogInfo.logs("Accuracy (vs all): %.3f%% ( %d / %d )", numSuccess * 100.0 / numExamples, numSuccess, numExamples);
    LogInfo.logs("Accuracy (vs found): %.3f%% ( %d / %d )", numSuccess * 100.0 / numFound, numSuccess, numFound);
    LogInfo.logs("Average F1 (vs target entities): %.3f%%", sumF1onExpectedEntities * 100.0 / numExamples);
    LogInfo.logs("Average F1 (vs best candidate): %.3f%%", sumF1onBest * 100.0 / numExamples);
    LogInfo.logs("Accuracy @ k : %s", Fmt.D(ListUtils.mult(100.0, ListUtils.subArray(getAccuracyAtK(10), 1))));
    LogInfo.end_track();
    return this;
  }
  
  public Evaluator printScores(boolean beVeryQuiet) {
    return beVeryQuiet ? this : printScores();
  }
  
}
